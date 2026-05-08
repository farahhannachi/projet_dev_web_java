package org.example.util;

import java.util.HashMap;
import java.util.Map;

public class DepotValidator {
    private final Map<String, String> errors = new HashMap<>();

    public boolean validate(String nom, String adresse, String ville, String capacite,
                            String responsable, String telephone, String latitude, String longitude) {
        return validate(nom, adresse, ville, capacite, responsable, telephone, latitude, longitude, "");
    }

    public boolean validate(String nom, String adresse, String ville, String capacite,
                            String responsable, String telephone, String latitude, String longitude,
                            String locationName) {
        errors.clear();

        validateNom(nom);
        validateAdresse(adresse);
        validateVille(ville);
        validateCapacite(capacite);
        validateResponsable(responsable);
        // Telephone, latitude, longitude, locationName - no strict validation
        // Accept any input for maximum flexibility

        return errors.isEmpty();
    }

    private void validateNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            errors.put("nom", "Le nom du depot est obligatoire");
            return;
        }

        String cleaned = nom.trim();
        if (cleaned.length() < 2) {
            errors.put("nom", "Le nom doit contenir au moins 2 caracteres");
            return;
        }

        if (cleaned.length() > 255) {
            errors.put("nom", "Le nom ne peut pas depasser 255 caracteres");
        }
    }

    private void validateAdresse(String adresse) {
        if (adresse == null || adresse.trim().isEmpty()) {
            errors.put("adresse", "L'adresse est obligatoire");
            return;
        }

        String cleaned = adresse.trim();
        if (cleaned.length() < 5) {
            errors.put("adresse", "L'adresse doit contenir au moins 5 caracteres");
            return;
        }

        if (cleaned.length() > 255) {
            errors.put("adresse", "L'adresse ne peut pas depasser 255 caracteres");
        }
    }

    private void validateVille(String ville) {
        if (ville == null || ville.trim().isEmpty()) {
            errors.put("ville", "La ville est obligatoire");
            return;
        }

        String cleaned = ville.trim();
        if (cleaned.length() < 2) {
            errors.put("ville", "La ville doit contenir au moins 2 caracteres");
            return;
        }

        if (cleaned.length() > 100) {
            errors.put("ville", "La ville ne peut pas depasser 100 caracteres");
        }
    }

    private void validateCapacite(String capacite) {
        if (capacite == null || capacite.trim().isEmpty()) {
            errors.put("capacite", "La capacite est obligatoire");
            return;
        }

        String cleaned = capacite.trim();
        if (!cleaned.matches("^\\d+$")) {
            errors.put("capacite", "La capacite doit etre un nombre entier positif");
            return;
        }

        try {
            int cap = Integer.parseInt(cleaned);
            if (cap <= 0) {
                errors.put("capacite", "La capacite doit etre superieure a 0");
            } else if (cap > 1_000_000) {
                errors.put("capacite", "La capacite semble trop elevee");
            }
        } catch (NumberFormatException exception) {
            errors.put("capacite", "Erreur lors du traitement de la capacite");
        }
    }

    private void validateResponsable(String responsable) {
        if (responsable == null || responsable.trim().isEmpty()) {
            errors.put("responsable", "Le responsable est obligatoire");
            return;
        }

        String cleaned = responsable.trim();
        if (cleaned.length() < 2) {
            errors.put("responsable", "Le nom du responsable doit contenir au moins 2 caracteres");
            return;
        }

        if (cleaned.length() > 255) {
            errors.put("responsable", "Le nom du responsable ne peut pas depasser 255 caracteres");
        }
    }

    public Map<String, String> getErrors() {
        return new HashMap<>(errors);
    }

    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        return String.join("; ", errors.values());
    }
}
