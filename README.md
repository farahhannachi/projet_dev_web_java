# CuraVita - Pharmacy Management System

JavaFX desktop application for pharmacy management.

- **Java**: 17
- **JavaFX**: 21.0.1
- **Architecture**: MVC

## Features

- Front Office: Homepage with product search and display
- Back Office: Dashboard with stats, clients, products, orders management
- Modern UI with CSS styling

## Running the Application

Ensure Java 17+ is installed and JAVA_HOME is set.

```bash
mvn clean compile
mvn javafx:run
```

## Speech-to-Text (Contact Form)

This project uses Vosk for offline speech-to-text in the Contact form fields.

1) Download a French Vosk model, for example:
- https://alphacephei.com/vosk/models

2) Extract the model to:
- `models/vosk-model-small-fr-0.22`

3) Or set a custom path:
- `VOSK_MODEL_PATH` environment variable
- or `-Dvosk.model.path=...` JVM property

The mic button appears next to "Objet de la demande" and "Description détaillée".

## Groq AI (Auto Priority + Admin Tools)

Set your Groq API key as an environment variable (do not commit it):
- `GROQ_API_KEY`

Optional overrides:
- `GROQ_MODEL` (default: `llama-3.1-8b-instant`)
- `GROQ_BASE_URL` (default: `https://api.groq.com/openai/v1/chat/completions`)

Features:
- Auto-detect priority when submitting a ticket
- Admin button "Résumé IA" (summary by priority)
- Admin button "Suggestion IA" (AI response as placeholder until confirmation)

## Project Structure

- `src/main/java/org/example/model/` - Data models
- `src/main/java/org/example/service/` - Business logic services
- `src/main/java/org/example/controller/` - UI controllers
- `src/main/resources/fxml/` - FXML views
- `src/main/resources/css/` - Stylesheets

## ResponseQuestion Admin Module

Access the admin module from the dashboard sidebar using "Reponses".

A standalone launcher is also available for quick testing:
- `org.example.ResponseQuestionAdminLauncher`

The module supports CRUD, filters, pagination, file upload, and PDF export.
