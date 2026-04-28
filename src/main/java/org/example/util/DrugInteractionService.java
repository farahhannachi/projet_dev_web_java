package org.example.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de détection d'interactions médicamenteuses et d'allergies.
 * Utilise les règles locales + OpenFDA API.
 */
public class DrugInteractionService {

    private static DrugInteractionService instance;

    // Paires de médicaments incompatibles (règles locales)
    public static final String[][] INTERACTIONS_LOCALES = {
        {"aspirine", "ibuprofene"},
        {"aspirine", "ibuprofen"},
        {"warfarine", "aspirine"},
        {"warfarine", "ibuprofene"},
        {"metformine", "alcool"},
        {"paracetamol", "alcool"},
        {"amoxicilline", "methotrexate"},
        {"ciprofloxacine", "antiacide"},
        {"simvastatine", "erythromycine"},
        {"digoxine", "amiodarone"},
        {"lithium", "ibuprofene"},
        {"lithium", "aspirine"},
        {"clopidogrel", "omeprazole"},
        {"fluoxetine", "tramadol"},
        {"sertraline", "tramadol"}
    };

    /**
     * Mapping principes actifs → noms commerciaux connus.
     * Permet de détecter qu'un produit comme "Panadol" contient du "paracétamol".
     */
    private static final String[][] PRINCIPES_ACTIFS = {
        // paracétamol
        {"paracetamol", "panadol", "doliprane", "efferalgan", "dafalgan", "tylenol", "claradol", "perfalgan"},
        // ibuprofène
        {"ibuprofene", "ibuprofen", "advil", "nurofen", "brufen", "motrin", "antarene"},
        // aspirine
        {"aspirine", "aspegic", "aspirin", "cardioaspirine", "kardegic"},
        // amoxicilline
        {"amoxicilline", "amoxil", "clamoxyl", "augmentin", "amoxicillin"},
        // codéine
        {"codeine", "codoliprane", "dafalgan codeine", "efferalgan codeine", "tussipax"},
        // tramadol
        {"tramadol", "topalgic", "contramal", "zamudol", "ixprim"},
        // metformine
        {"metformine", "glucophage", "stagid", "metformin"},
        // warfarine
        {"warfarine", "coumadine", "warfarin"},
        // oméprazole
        {"omeprazole", "mopral", "prilosec", "losec"},
        // simvastatine
        {"simvastatine", "zocor", "lodales", "simvastatin"},
        // cetirizine
        {"cetirizine", "zyrtec", "virlix", "reactine"},
        // loratadine
        {"loratadine", "clarityne", "claritin"},
        // diazépam
        {"diazepam", "valium", "diazepam"},
        // fluoxétine
        {"fluoxetine", "prozac", "fluoxetine"},
        // sertraline
        {"sertraline", "zoloft", "sertraline"},
        // amiodarone
        {"amiodarone", "cordarone"},
        // digoxine
        {"digoxine", "digoxin", "lanoxin"},
        // ciprofloxacine
        {"ciprofloxacine", "ciprofloxacin", "ciflox", "cipro"},
        // lithium
        {"lithium", "teralithe", "camcolit"},
        // clopidogrel
        {"clopidogrel", "plavix"},
        // érythromycine
        {"erythromycine", "erythromycin", "erythrocine", "abboticine"},
        // methotrexate
        {"methotrexate", "methotrexat", "novatrex", "imeth"},
    };

    /**
     * Résultat structuré d'une vérification d'allergie intelligente.
     */
    public static class AllergieResult {
        public boolean critique;           // true = bloquant (allergie confirmée)
        public List<String> problemes;     // liste des problèmes détectés
        public String recommandation;      // texte de recommandation
        public String alternativeSuggestion; // produit alternatif suggéré par l'IA

        public AllergieResult() {
            problemes = new ArrayList<>();
        }

        public boolean hasProbleme() {
            return !problemes.isEmpty();
        }
    }

    private DrugInteractionService() {}

    public static DrugInteractionService getInstance() {
        if (instance == null) {
            instance = new DrugInteractionService();
        }
        return instance;
    }

    /**
     * Résout le principe actif d'un nom de produit.
     * Ex: "Panadol" → "paracetamol"
     */
    public String resoudrePrincipeActif(String nomProduit) {
        String lower = nomProduit.toLowerCase().trim();
        for (String[] groupe : PRINCIPES_ACTIFS) {
            for (String nom : groupe) {
                if (lower.contains(nom) || nom.contains(lower)) {
                    return groupe[0]; // retourne le principe actif (premier élément)
                }
            }
        }
        return lower;
    }

    /**
     * Retourne tous les noms commerciaux connus pour un principe actif.
     */
    public List<String> getNomsCommerciaux(String principeActif) {
        List<String> noms = new ArrayList<>();
        String lower = principeActif.toLowerCase();
        for (String[] groupe : PRINCIPES_ACTIFS) {
            if (groupe[0].equals(lower)) {
                for (int i = 1; i < groupe.length; i++) noms.add(groupe[i]);
                break;
            }
        }
        return noms;
    }

    /**
     * Vérifie intelligemment si le produit est dangereux selon les antécédents/symptômes.
     * Détecte les allergies par principe actif (ex: "allergique paracétamol" → bloque Panadol).
     * Retourne un AllergieResult structuré.
     */
    public AllergieResult verifierAllergieIntelligente(String nomProduit, String antecedents) {
        AllergieResult result = new AllergieResult();
        if (antecedents == null || antecedents.isBlank()) return result;

        String antecedentsLower = antecedents.toLowerCase();
        String principeActifProduit = resoudrePrincipeActif(nomProduit);

        // Chercher les allergies mentionnées dans les antécédents
        String[] motsAllergie = {"allergi", "intoleran", "reaction allergique", "hypersensib", "choc anaphylactique"};

        for (String[] groupe : PRINCIPES_ACTIFS) {
            String principeActif = groupe[0];
            boolean mentionneAllergie = false;

            // Vérifier si les antécédents mentionnent une allergie à ce principe actif ou ses noms commerciaux
            for (String motAllergie : motsAllergie) {
                if (antecedentsLower.contains(motAllergie)) {
                    // Vérifier si le principe actif ou un de ses noms commerciaux est mentionné
                    for (String nom : groupe) {
                        if (antecedentsLower.contains(nom)) {
                            mentionneAllergie = true;
                            break;
                        }
                    }
                }
                if (mentionneAllergie) break;
            }

            if (mentionneAllergie) {
                // Vérifier si le produit ajouté contient ce principe actif
                boolean produitContientPrincipe = false;
                for (String nom : groupe) {
                    if (nomProduit.toLowerCase().contains(nom)) {
                        produitContientPrincipe = true;
                        break;
                    }
                }

                if (produitContientPrincipe || principeActifProduit.equals(principeActif)) {
                    result.critique = true;
                    result.problemes.add("Allergie au " + principeActif + " détectée dans les antécédents");
                    result.problemes.add("Le produit sélectionné contient du " + principeActif);
                    result.problemes.add("Risque de réaction allergique grave (choc anaphylactique)");
                    result.recommandation = "NE PAS PRENDRE CE MÉDICAMENT.\nConsultez immédiatement un médecin ou un pharmacien pour une alternative.";
                    result.alternativeSuggestion = suggererAlternative(principeActif, antecedents);
                    return result;
                }
            }
        }

        // Vérification simple (fallback) : nom du produit dans les antécédents avec mot allergie
        for (String motAllergie : motsAllergie) {
            if (antecedentsLower.contains(motAllergie)) {
                String[] motsProduit = nomProduit.toLowerCase().split("[\\s\\-]+");
                for (String mot : motsProduit) {
                    if (mot.length() > 4 && antecedentsLower.contains(mot)) {
                        result.critique = true;
                        result.problemes.add("Allergie potentielle à \"" + nomProduit + "\" détectée dans les antécédents");
                        result.problemes.add("Risque de réaction allergique");
                        result.recommandation = "NE PAS PRENDRE CE MÉDICAMENT.\nConsultez immédiatement un médecin ou un pharmacien.";
                        result.alternativeSuggestion = suggererAlternative(principeActifProduit, antecedents);
                        return result;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Suggère un produit alternatif compatible selon le principe actif allergène et les symptômes.
     */
    public String suggererAlternative(String principeActifAllergene, String symptomes) {
        String symptomesLower = symptomes != null ? symptomes.toLowerCase() : "";

        // Alternatives selon le principe actif allergène
        switch (principeActifAllergene) {
            case "paracetamol":
                // Alternative pour douleur/fièvre sans paracétamol
                if (symptomesLower.contains("douleur") || symptomesLower.contains("fievre") || symptomesLower.contains("fièvre")) {
                    return "Ibuprofène (Advil/Nurofen) — anti-inflammatoire et antalgique sans paracétamol.\n⚠️ À éviter si gastrite ou grossesse.";
                }
                return "Ibuprofène (Advil) ou Naproxène — alternatives sans paracétamol.";

            case "ibuprofene":
            case "ibuprofen":
                return "Paracétamol (Doliprane/Efferalgan) — antalgique sans ibuprofène.";

            case "aspirine":
                if (symptomesLower.contains("douleur") || symptomesLower.contains("fievre")) {
                    return "Paracétamol (Doliprane) — alternative sans aspirine pour douleur/fièvre.";
                }
                return "Clopidogrel (Plavix) pour usage cardiovasculaire — sur prescription médicale.";

            case "amoxicilline":
                return "Azithromycine (Zithromax) ou Clarithromycine — antibiotiques alternatifs sans pénicilline.";

            case "codeine":
                return "Tramadol ou Ibuprofène — alternatives analgésiques sans codéine.";

            case "tramadol":
                return "Ibuprofène ou Paracétamol — alternatives sans tramadol.";

            case "cetirizine":
                return "Loratadine (Clarityne) — antihistaminique alternatif.";

            case "loratadine":
                return "Cétirizine (Zyrtec) — antihistaminique alternatif.";

            default:
                return "Consultez un médecin ou pharmacien pour une alternative adaptée à votre allergie à \"" + principeActifAllergene + "\".";
        }
    }

    /**
     * Vérifie si le produit est dangereux selon les antécédents (allergies) — version simple (rétrocompatibilité)
     */
    public String verifierAllergie(String nomProduit, String antecedents) {
        AllergieResult result = verifierAllergieIntelligente(nomProduit, antecedents);
        if (result.hasProbleme()) {
            return "⚠️ Allergie détectée : " + String.join(", ", result.problemes);
        }
        return null;
    }

    /**
     * Vérifie les interactions avec les traitements actifs du patient (règles locales)
     */
    public List<String> verifierInteractionsLocales(String nomNouveauProduit, int userId) {
        List<String> alertes = new ArrayList<>();
        String produitLower = nomNouveauProduit.toLowerCase();

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT p.nom FROM traitement t " +
                "JOIN produit p ON t.id_produit_id = p.id_produit " +
                "WHERE t.id_utilisateur_id = ? AND t.status = 'actif'"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String produitActif = rs.getString("nom").toLowerCase();
                for (String[] paire : INTERACTIONS_LOCALES) {
                    boolean match1 = produitLower.contains(paire[0]) && produitActif.contains(paire[1]);
                    boolean match2 = produitLower.contains(paire[1]) && produitActif.contains(paire[0]);
                    if (match1 || match2) {
                        alertes.add("🚫 Interaction détectée : \"" + nomNouveauProduit + "\" est incompatible avec \"" +
                                rs.getString("nom") + "\" (traitement actif).");
                    }
                }
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.err.println("[DrugInteraction] Erreur SQL : " + e.getMessage());
        }
        return alertes;
    }

    /**
     * Vérifie les interactions via OpenFDA API
     */
    public List<String> verifierInteractionsOpenFDA(String nomProduit1, String nomProduit2) {
        List<String> alertes = new ArrayList<>();
        try {
            String query = URLEncoder.encode(
                "patient.drug.medicinalproduct:\"" + nomProduit1 + "\" AND patient.drug.medicinalproduct:\"" + nomProduit2 + "\"",
                StandardCharsets.UTF_8
            );
            String urlStr = "https://api.fda.gov/drug/event.json?search=" + query + "&limit=1";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                if (response.toString().contains("\"total\"") && !response.toString().contains("\"total\":0")) {
                    alertes.add("⚠️ OpenFDA : des effets indésirables ont été signalés pour la combinaison \"" +
                            nomProduit1 + "\" + \"" + nomProduit2 + "\". Consultez un médecin.");
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("[OpenFDA] API non disponible : " + e.getMessage());
        }
        return alertes;
    }

    /**
     * Vérification complète : allergie + interactions locales + OpenFDA
     */
    public List<String> verifierTout(String nomNouveauProduit, String antecedents, int userId, List<String> produitsDejaAjoutes) {
        List<String> alertes = new ArrayList<>();

        // 1. Vérifier allergie intelligente
        String alerteAllergie = verifierAllergie(nomNouveauProduit, antecedents);
        if (alerteAllergie != null) alertes.add(alerteAllergie);

        // 2. Vérifier interactions avec traitements actifs en base
        alertes.addAll(verifierInteractionsLocales(nomNouveauProduit, userId));

        // 3. Vérifier interactions avec les produits déjà ajoutés dans ce formulaire
        for (String produitAjoute : produitsDejaAjoutes) {
            String nomAjoute = produitAjoute.contains(" - ") ? produitAjoute.split(" - ")[1] : produitAjoute;
            String produitLower = nomNouveauProduit.toLowerCase();
            String ajouteLower = nomAjoute.toLowerCase();
            for (String[] paire : INTERACTIONS_LOCALES) {
                boolean match1 = produitLower.contains(paire[0]) && ajouteLower.contains(paire[1]);
                boolean match2 = produitLower.contains(paire[1]) && ajouteLower.contains(paire[0]);
                if (match1 || match2) {
                    alertes.add("🚫 Interaction : \"" + nomNouveauProduit + "\" est incompatible avec \"" + nomAjoute + "\" (déjà sélectionné).");
                }
            }
            alertes.addAll(verifierInteractionsOpenFDA(nomNouveauProduit, nomAjoute));
        }

        return alertes;
    }
}
