package org.example.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validateur côté serveur pour les données Service
 * Toute la validation est faite ici avant insertion en base de données
 * Aucune validation JavaScript - validation strictement backend
 */
public class ServiceValidator {

    // Regex pour validation
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[1-9]\\d{8}$"); // Format FR: 0X XXXXXXXX
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-àâäæèéêëìîïòôöœùûüœçñ]+$");

    private Map<String, String> errors = new HashMap<>();

    /**
     * Valide tous les champs du service
     * @return true si valide, false sinon
     */
    public boolean validate(String nom, String type, String specialite, String telephone, String email, String adresse) {
        errors.clear();

        // Valider chaque champ
        validateNom(nom);
        validateType(type);
        validateSpecialite(specialite);
        validateTelephone(telephone);
        validateEmail(email);
        validateAdresse(adresse);

        return errors.isEmpty();
    }

    /**
     * Valide le nom du service
     */
    private void validateNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            errors.put("nom", "Le nom est obligatoire");
            return;
        }

        nom = nom.trim();

        if (nom.length() < 2) {
            errors.put("nom", "Le nom doit contenir au moins 2 caractères");
            return;
        }

        if (nom.length() > 255) {
            errors.put("nom", "Le nom ne peut pas dépasser 255 caractères");
            return;
        }

        if (!NAME_PATTERN.matcher(nom).matches()) {
            errors.put("nom", "Le nom contient des caractères invalides");
        }
    }

    /**
     * Valide le type (Médecin/Infirmier)
     */
    private void validateType(String type) {
        if (type == null || type.trim().isEmpty()) {
            errors.put("type", "Le type est obligatoire");
            return;
        }

        type = type.trim();

        if (!type.equals("Médecin") && !type.equals("Infirmier")) {
            errors.put("type", "Le type doit être 'Médecin' ou 'Infirmier'");
        }
    }

    /**
     * Valide la spécialité
     */
    private void validateSpecialite(String specialite) {
        if (specialite == null || specialite.trim().isEmpty()) {
            errors.put("specialite", "La spécialité est obligatoire");
            return;
        }

        specialite = specialite.trim();

        if (specialite.length() < 2) {
            errors.put("specialite", "La spécialité doit contenir au moins 2 caractères");
            return;
        }

        if (specialite.length() > 255) {
            errors.put("specialite", "La spécialité ne peut pas dépasser 255 caractères");
        }
    }

    /**
     * Valide le numéro de téléphone
     */
    private void validateTelephone(String telephone) {
        if (telephone == null || telephone.trim().isEmpty()) {
            errors.put("telephone", "Le téléphone est obligatoire");
            return;
        }

        telephone = telephone.trim();

        if (!PHONE_PATTERN.matcher(telephone).matches()) {
            errors.put("telephone", "Format invalide. Utilisez: 0X XXXXXXXX (ex: 0123456789)");
        }
    }

    /**
     * Valide l'email
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            errors.put("email", "L'email est obligatoire");
            return;
        }

        email = email.trim();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "Format d'email invalide");
        }
    }

    /**
     * Valide l'adresse
     */
    private void validateAdresse(String adresse) {
        if (adresse == null || adresse.trim().isEmpty()) {
            errors.put("adresse", "L'adresse est obligatoire");
            return;
        }

        adresse = adresse.trim();

        if (adresse.length() < 5) {
            errors.put("adresse", "L'adresse doit contenir au moins 5 caractères");
            return;
        }

        if (adresse.length() > 255) {
            errors.put("adresse", "L'adresse ne peut pas dépasser 255 caractères");
        }
    }

    /**
     * Retourne les erreurs de validation
     */
    public Map<String, String> getErrors() {
        return new HashMap<>(errors);
    }

    /**
     * Retourne les erreurs sous forme de message
     */
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        errors.forEach((field, message) -> {
            if (sb.length() > 0) sb.append("\n");
            sb.append("• ").append(message);
        });
        return sb.toString();
    }

    /**
     * Nettoie et sécurise une chaîne de caractères
     * - Trim les espaces
     * - Échappe les caractères HTML
     * - Prévient les injections
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        // Trim
        input = input.trim();
        // Échapper les caractères HTML spéciaux pour éviter XSS (même si on est en desktop)
        input = input.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&#x27;");
        return input;
    }

    /**
     * Valide et nettoie une chaîne
     */
    public static String validateAndSanitize(String input) {
        return sanitize(input);
    }
}
