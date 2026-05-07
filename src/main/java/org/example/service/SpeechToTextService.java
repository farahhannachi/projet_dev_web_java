package org.example.service;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class SpeechToTextService {
    private static final int SAMPLE_RATE = 16000;

    private final String modelPath;
    private Model model;
    private Thread worker;
    private volatile boolean running;
    private TargetDataLine line;

    public SpeechToTextService() {
        this(resolveDefaultModelPath());
    }

    public SpeechToTextService(String modelPath) {
        this.modelPath = modelPath;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void start(Consumer<String> onPartial, Consumer<String> onFinal, Consumer<String> onError) throws IOException {
        if (running) {
            return;
        }
        ensureModelLoaded();
        running = true;
        worker = new Thread(() -> runLoop(onPartial, onFinal, onError), "speech-to-text-worker");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        running = false;
        closeLine();
    }

    private void runLoop(Consumer<String> onPartial, Consumer<String> onFinal, Consumer<String> onError) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        String lastPartial = "";

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
                while (running) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead <= 0) {
                        continue;
                    }
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        String text = extractValue(recognizer.getResult(), "text");
                        if (!text.isBlank() && onFinal != null) {
                            onFinal.accept(text);
                        }
                        lastPartial = "";
                    } else {
                        String partial = extractValue(recognizer.getPartialResult(), "partial");
                        if (!partial.isBlank() && !partial.equals(lastPartial) && onPartial != null) {
                            lastPartial = partial;
                            onPartial.accept(partial);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (onError != null) {
                onError.accept(e.getMessage());
            }
        } finally {
            stop();
        }
    }

    private void ensureModelLoaded() throws IOException {
        if (model != null) {
            return;
        }
        Path modelDir = Path.of(modelPath);
        if (!modelDir.toFile().exists()) {
            throw new IOException("Vosk model not found at: " + modelPath);
        }
        model = new Model(modelPath);
    }

    private void closeLine() {
        if (line != null) {
            try {
                line.stop();
            } catch (Exception ignored) {
                // ignore
            }
            try {
                line.close();
            } catch (Exception ignored) {
                // ignore
            }
            line = null;
        }
    }

    private static String resolveDefaultModelPath() {
        String fromProperty = System.getProperty("vosk.model.path");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv("VOSK_MODEL_PATH");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return "models/vosk-model-small-fr-0.22";
    }

    private static String extractValue(String json, String key) {
        if (json == null || key == null) {
            return "";
        }
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', keyIndex + token.length());
        if (colonIndex < 0) {
            return "";
        }
        int firstQuote = json.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return "";
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }
        return json.substring(firstQuote + 1, secondQuote);
    }
}

