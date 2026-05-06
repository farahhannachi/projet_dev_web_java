package org.example.util; // Package "util" — services transversaux

import java.io.BufferedReader;      // Lecture ligne par ligne d'un flux texte
import java.io.InputStreamReader;   // Convertit un flux binaire en flux texte
import java.net.HttpURLConnection;  // Connexion HTTP pour appeler l'API OpenFDA
import java.net.URL;                // Représente une URL
import java.net.URLEncoder;         // Encode les caractères spéciaux dans une URL (ex: espace → %20)
import java.nio.charset.StandardCharsets; // Charset UTF-8
import java.sql.*;                  // Classes JDBC
import java.util.ArrayList;         // Liste dynamique
import java.util.List;              // Interface List

/**
 * DrugInteractionService — Service de détection d'interactions médicamenteuses et d'allergies.
 *
 * Rôle : protéger le patient en détectant :
 *   1. Les allergies : le patient est allergique à un principe actif du médicament sélectionné
 *   2. Les interactions locales : deux médicaments incompatibles (règles codées en dur)
 *   3. Les interactions OpenFDA : effets indésirables signalés à la FDA pour une paire de médicaments
 *
 * Fonctionnalité clé : résolution des principes actifs
 *   "Panadol" → "paracetamol" → peut détecter une allergie au paracétamol même si
 *   le patient a écrit "allergique au Doliprane" dans ses antécédents.
 *
 * Pattern Singleton : une seule instance dans toute l'application.
 */
public class DrugInteractionService {

    // Instance unique (Singleton)
    private static DrugInteractionService instance;

    /**
     * INTERACTIONS_LOCALES — Tableau des paires de médicaments incompatibles.
     *
     * public static final : accessible depuis l'extérieur sans instancier la classe
     * String[][] : tableau 2D → chaque ligne est une paire [médicament1, médicament2]
     *
     * Ces règles sont basées sur des interactions médicamenteuses connues et documentées.
     * La vérification se fait dans les deux sens : aspirine+ibuprofène = ibuprofène+aspirine.
     */
    public static final String[][] INTERACTIONS_LOCALES = {
        {"aspirine", "ibuprofene"},      // Aspirine + Ibuprofène → risque hémorragique majeur
        {"aspirine", "ibuprofen"},       // Même paire, orthographe anglaise
        {"warfarine", "aspirine"},       // Anticoagulant + aspirine → saignements graves
        {"warfarine", "ibuprofene"},     // Anticoagulant + AINS → saignements graves
        {"metformine", "alcool"},        // Antidiabétique + alcool → acidose lactique
        {"paracetamol", "alcool"},       // Paracétamol + alcool → toxicité hépatique (foie)
        {"amoxicilline", "methotrexate"}, // Antibiotique + méthotrexate → toxicité accrue
        {"ciprofloxacine", "antiacide"}, // Antibiotique + antiacide → absorption réduite
        {"simvastatine", "erythromycine"}, // Statine + macrolide → rhabdomyolyse (destruction musculaire)
        {"digoxine", "amiodarone"},      // Cardiotonique + antiarythmique → toxicité cardiaque
        {"lithium", "ibuprofene"},       // Lithium + AINS → augmentation taux lithium (toxique)
        {"lithium", "aspirine"},         // Lithium + aspirine → même risque
        {"clopidogrel", "omeprazole"},   // Antiplaquettaire + IPP → réduction de l'effet anticoagulant
        {"fluoxetine", "tramadol"},      // Antidépresseur ISRS + opioïde → syndrome sérotoninergique
        {"sertraline", "tramadol"}       // Même risque avec sertraline
    };

    /**
     * PRINCIPES_ACTIFS — Mapping noms commerciaux → principe actif.
     *
     * Chaque ligne est un groupe : [principe_actif, nom_commercial_1, nom_commercial_2, ...]
     * Le premier élément est toujours le principe actif (molécule).
     * Les suivants sont les noms commerciaux (marques).
     *
     * Permet de détecter qu'un patient allergique au "paracétamol" ne doit pas
     * prendre "Panadol", "Doliprane", "Efferalgan" etc.
     *
     * Exemple : groupe paracétamol
     *   {"paracetamol", "panadol", "doliprane", "efferalgan", ...}
     *   resoudrePrincipeActif("Panadol") → "paracetamol"
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
     * Résout le principe actif d'un nom de produit commercial.
     *
     * Algorithme : parcourir tous les groupes PRINCIPES_ACTIFS,
     * chercher si le nom du produit contient un des noms du groupe
     * (ou si un nom du groupe contient le nom du produit).
     * Si trouvé → retourner groupe[0] (le principe actif).
     *
     * Exemple : resoudrePrincipeActif("Panadol") → "paracetamol"
     * Exemple : resoudrePrincipeActif("Advil 400mg") → "ibuprofene"
     *
     * @param nomProduit Le nom commercial du médicament
     * @return Le principe actif (molécule), ou le nom en minuscules si non trouvé
     */
    public String resoudrePrincipeActif(String nomProduit) {
        String lower = nomProduit.toLowerCase().trim(); // Normaliser en minuscules

        for (String[] groupe : PRINCIPES_ACTIFS) {
            for (String nom : groupe) {
                // Vérification bidirectionnelle :
                // - lower.contains(nom) : "Panadol 500mg" contient "panadol" ✓
                // - nom.contains(lower) : "panadol" contient "panadol" ✓
                if (lower.contains(nom) || nom.contains(lower)) {
                    return groupe[0]; // Retourner le principe actif (premier élément du groupe)
                }
            }
        }
        return lower; // Non trouvé → retourner le nom normalisé tel quel
    }

    /**
     * Retourne tous les noms commerciaux connus pour un principe actif.
     *
     * Exemple : getNomsCommerciaux("paracetamol")
     *   → ["panadol", "doliprane", "efferalgan", "dafalgan", "tylenol", ...]
     *
     * @param principeActif Le principe actif (ex: "paracetamol")
     * @return Liste des noms commerciaux connus
     */
    public List<String> getNomsCommerciaux(String principeActif) {
        List<String> noms = new ArrayList<>();
        String lower = principeActif.toLowerCase();

        for (String[] groupe : PRINCIPES_ACTIFS) {
            if (groupe[0].equals(lower)) { // Trouver le groupe correspondant
                // Ajouter tous les noms sauf le premier (qui est le principe actif)
                for (int i = 1; i < groupe.length; i++) noms.add(groupe[i]);
                break; // Groupe trouvé → arrêter la recherche
            }
        }
        return noms;
    }

    /**
     * Vérifie intelligemment si un médicament est dangereux pour un patient
     * en analysant ses antécédents médicaux textuels.
     *
     * Algorithme en 2 passes :
     *
     * Passe 1 (par principe actif) :
     *   Pour chaque groupe de médicaments (ex: paracétamol + ses noms commerciaux) :
     *     - Vérifier si les antécédents mentionnent une allergie à ce groupe
     *     - Si oui, vérifier si le médicament sélectionné appartient à ce groupe
     *     - Si oui → allergie critique détectée
     *
     * Passe 2 (fallback simple) :
     *   Si aucune allergie trouvée par principe actif :
     *     - Chercher si le nom du médicament apparaît dans les antécédents avec un mot allergie
     *
     * @param nomProduit  Nom du médicament à vérifier
     * @param antecedents Antécédents médicaux du patient (texte libre)
     * @return AllergieResult avec critique=true si allergie détectée
     */
    public AllergieResult verifierAllergieIntelligente(String nomProduit, String antecedents) {
        AllergieResult result = new AllergieResult();

        // Si pas d'antécédents → pas d'allergie possible
        if (antecedents == null || antecedents.isBlank()) return result;

        String antecedentsLower = antecedents.toLowerCase(); // Normaliser pour comparaison
        String principeActifProduit = resoudrePrincipeActif(nomProduit); // Résoudre le principe actif

        // Mots-clés qui indiquent une allergie dans le texte des antécédents
        // Utilisation de préfixes pour capturer les variantes : "allergi" capture "allergie", "allergique", "allergies"
        String[] motsAllergie = {"allergi", "intoleran", "reaction allergique", "hypersensib", "choc anaphylactique"};

        // ── Passe 1 : Vérification par principe actif ─────────────────────
        for (String[] groupe : PRINCIPES_ACTIFS) {
            String principeActif = groupe[0]; // ex: "paracetamol"
            boolean mentionneAllergie = false;

            // Vérifier si les antécédents mentionnent une allergie à ce groupe
            for (String motAllergie : motsAllergie) {
                if (antecedentsLower.contains(motAllergie)) {
                    // Un mot d'allergie est présent → vérifier si ce groupe est mentionné
                    for (String nom : groupe) { // Parcourir principe actif + noms commerciaux
                        if (antecedentsLower.contains(nom)) {
                            mentionneAllergie = true; // Ex: "allergique au doliprane" → paracétamol mentionné
                            break;
                        }
                    }
                }
                if (mentionneAllergie) break; // Allergie trouvée → arrêter la recherche
            }

            if (mentionneAllergie) {
                // Une allergie à ce groupe est mentionnée dans les antécédents
                // Vérifier si le médicament sélectionné appartient à ce groupe
                boolean produitContientPrincipe = false;
                for (String nom : groupe) {
                    if (nomProduit.toLowerCase().contains(nom)) {
                        produitContientPrincipe = true; // Ex: "Panadol" contient "panadol" ✓
                        break;
                    }
                }

                // Si le médicament contient le principe actif allergène → CRITIQUE
                if (produitContientPrincipe || principeActifProduit.equals(principeActif)) {
                    result.critique = true; // Bloquer l'ajout du médicament
                    result.problemes.add("Allergie au " + principeActif + " détectée dans les antécédents");
                    result.problemes.add("Le produit sélectionné contient du " + principeActif);
                    result.problemes.add("Risque de réaction allergique grave (choc anaphylactique)");
                    result.recommandation = "NE PAS PRENDRE CE MÉDICAMENT.\nConsultez immédiatement un médecin ou un pharmacien pour une alternative.";
                    // Suggérer une alternative compatible
                    result.alternativeSuggestion = suggererAlternative(principeActif, antecedents);
                    return result; // Retourner immédiatement (allergie critique trouvée)
                }
            }
        }

        // ── Passe 2 : Fallback — vérification simple par nom du produit ───
        for (String motAllergie : motsAllergie) {
            if (antecedentsLower.contains(motAllergie)) {
                // Un mot d'allergie est présent → vérifier si le nom du produit est mentionné
                // Découper le nom du produit en mots (séparés par espaces ou tirets)
                String[] motsProduit = nomProduit.toLowerCase().split("[\\s\\-]+");
                for (String mot : motsProduit) {
                    // Ignorer les mots trop courts (ex: "mg", "de", "le") → longueur > 4
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

        return result; // Aucune allergie détectée → result.critique = false
    }

    /**
     * Suggère un médicament alternatif compatible quand le médicament principal est allergène.
     *
     * Logique : switch sur le principe actif allergène → retourner une alternative
     * de la même famille thérapeutique mais sans le principe actif problématique.
     * Ajustement contextuel selon les symptômes du patient.
     *
     * @param principeActifAllergene Le principe actif auquel le patient est allergique
     * @param symptomes              Les symptômes du patient (pour choisir la meilleure alternative)
     * @return Texte décrivant l'alternative suggérée
     */
    public String suggererAlternative(String principeActifAllergene, String symptomes) {
        String symptomesLower = symptomes != null ? symptomes.toLowerCase() : "";

        switch (principeActifAllergene) {
            case "paracetamol":
                // Alternative pour douleur/fièvre sans paracétamol → ibuprofène
                if (symptomesLower.contains("douleur") || symptomesLower.contains("fievre") || symptomesLower.contains("fièvre")) {
                    return "Ibuprofène (Advil/Nurofen) — anti-inflammatoire et antalgique sans paracétamol.\n⚠️ À éviter si gastrite ou grossesse.";
                }
                return "Ibuprofène (Advil) ou Naproxène — alternatives sans paracétamol.";

            case "ibuprofene":
            case "ibuprofen":
                // Alternative sans AINS → paracétamol
                return "Paracétamol (Doliprane/Efferalgan) — antalgique sans ibuprofène.";

            case "aspirine":
                if (symptomesLower.contains("douleur") || symptomesLower.contains("fievre")) {
                    return "Paracétamol (Doliprane) — alternative sans aspirine pour douleur/fièvre.";
                }
                // Pour usage cardiovasculaire → clopidogrel (sur prescription)
                return "Clopidogrel (Plavix) pour usage cardiovasculaire — sur prescription médicale.";

            case "amoxicilline":
                // Allergie pénicilline → macrolide (famille différente)
                return "Azithromycine (Zithromax) ou Clarithromycine — antibiotiques alternatifs sans pénicilline.";

            case "codeine":
                return "Tramadol ou Ibuprofène — alternatives analgésiques sans codéine.";

            case "tramadol":
                return "Ibuprofène ou Paracétamol — alternatives sans tramadol.";

            case "cetirizine":
                // Antihistaminique alternatif de la même génération
                return "Loratadine (Clarityne) — antihistaminique alternatif.";

            case "loratadine":
                return "Cétirizine (Zyrtec) — antihistaminique alternatif.";

            default:
                // Cas non géré → recommander une consultation
                return "Consultez un médecin ou pharmacien pour une alternative adaptée à votre allergie à \"" + principeActifAllergene + "\".";
        }
    }

    /**
     * Version simplifiée de verifierAllergieIntelligente() pour la rétrocompatibilité.
     * Retourne une chaîne de texte au lieu d'un AllergieResult structuré.
     *
     * @param nomProduit  Nom du médicament
     * @param antecedents Antécédents du patient
     * @return Message d'alerte ou null si aucune allergie
     */
    public String verifierAllergie(String nomProduit, String antecedents) {
        AllergieResult result = verifierAllergieIntelligente(nomProduit, antecedents);
        if (result.hasProbleme()) {
            // Joindre tous les problèmes en une seule chaîne séparée par des virgules
            return "⚠️ Allergie détectée : " + String.join(", ", result.problemes);
        }
        return null; // Aucune allergie → retourner null
    }

    /**
     * Vérifie les interactions entre un nouveau médicament et les traitements ACTIFS du patient.
     *
     * Processus :
     *   1. Récupérer tous les médicaments actifs du patient depuis la base
     *   2. Pour chaque médicament actif, vérifier si la paire (nouveau + actif)
     *      correspond à une des paires dans INTERACTIONS_LOCALES
     *   3. Vérification dans les deux sens (aspirine+ibuprofène = ibuprofène+aspirine)
     *
     * @param nomNouveauProduit Le médicament que le patient veut ajouter
     * @param userId            L'ID du patient (pour récupérer ses traitements actifs)
     * @return Liste des alertes d'interaction (vide si aucune interaction)
     */
    public List<String> verifierInteractionsLocales(String nomNouveauProduit, int userId) {
        List<String> alertes = new ArrayList<>();
        String produitLower = nomNouveauProduit.toLowerCase(); // Normaliser

        try {
            Connection conn = DatabaseUtil.getInstance().getConnection();

            // Récupérer tous les médicaments actifs du patient
            // JOIN produit : pour avoir le nom du médicament (pas juste l'ID)
            // WHERE status = 'actif' : seulement les traitements en cours
            PreparedStatement ps = conn.prepareStatement(
                "SELECT p.nom FROM traitement t " +
                "JOIN produit p ON t.id_produit_id = p.id_produit " +
                "WHERE t.id_utilisateur_id = ? AND t.status = 'actif'"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String produitActif = rs.getString("nom").toLowerCase(); // Médicament actif du patient

                // Vérifier chaque paire d'interactions connues
                for (String[] paire : INTERACTIONS_LOCALES) {
                    // Sens 1 : nouveau=paire[0] ET actif=paire[1]
                    boolean match1 = produitLower.contains(paire[0]) && produitActif.contains(paire[1]);
                    // Sens 2 : nouveau=paire[1] ET actif=paire[0] (vérification bidirectionnelle)
                    boolean match2 = produitLower.contains(paire[1]) && produitActif.contains(paire[0]);

                    if (match1 || match2) {
                        // Interaction détectée → ajouter une alerte
                        alertes.add("🚫 Interaction détectée : \"" + nomNouveauProduit + "\" est incompatible avec \"" +
                                rs.getString("nom") + "\" (traitement actif).");
                    }
                }
            }
            rs.close(); ps.close();

        } catch (SQLException e) {
            System.err.println("[DrugInteraction] Erreur SQL : " + e.getMessage());
        }
        return alertes; // Liste vide si aucune interaction
    }

    /**
     * Vérifie les interactions entre deux médicaments via l'API OpenFDA.
     *
     * Endpoint utilisé : https://api.fda.gov/drug/event.json
     * Cet endpoint contient les rapports d'effets indésirables soumis à la FDA.
     * On cherche des rapports où les deux médicaments sont mentionnés ensemble.
     *
     * Si des rapports existent → potentielle interaction signalée.
     *
     * Timeout : 3 secondes (pour ne pas bloquer l'UI trop longtemps)
     *
     * @param nomProduit1 Premier médicament
     * @param nomProduit2 Deuxième médicament
     * @return Liste des alertes (vide si aucune interaction signalée ou API indisponible)
     */
    public List<String> verifierInteractionsOpenFDA(String nomProduit1, String nomProduit2) {
        List<String> alertes = new ArrayList<>();
        try {
            // Construire la requête OpenFDA :
            // Chercher les rapports où les deux médicaments sont mentionnés
            // patient.drug.medicinalproduct : champ du nom du médicament dans les rapports FDA
            // URLEncoder.encode() : encoder les caractères spéciaux pour l'URL (espaces, guillemets...)
            String query = URLEncoder.encode(
                "patient.drug.medicinalproduct:\"" + nomProduit1 + "\" AND patient.drug.medicinalproduct:\"" + nomProduit2 + "\"",
                StandardCharsets.UTF_8
            );

            // Construire l'URL complète : base + paramètre search + limit=1 (on veut juste savoir si ça existe)
            String urlStr = "https://api.fda.gov/drug/event.json?search=" + query + "&limit=1";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");          // Requête HTTP GET
            conn.setConnectTimeout(3000);          // Timeout connexion : 3 secondes
            conn.setReadTimeout(3000);             // Timeout lecture : 3 secondes

            int responseCode = conn.getResponseCode(); // Code HTTP : 200=OK, 404=non trouvé...

            if (responseCode == 200) { // Succès
                // Lire la réponse JSON
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                // Vérifier si des résultats existent :
                // "total" présent ET "total":0 absent → il y a des rapports
                if (response.toString().contains("\"total\"") && !response.toString().contains("\"total\":0")) {
                    alertes.add("⚠️ OpenFDA : des effets indésirables ont été signalés pour la combinaison \"" +
                            nomProduit1 + "\" + \"" + nomProduit2 + "\". Consultez un médecin.");
                }
            }
            conn.disconnect(); // Fermer la connexion HTTP

        } catch (Exception e) {
            // API indisponible (pas de réseau, timeout...) → log silencieux, pas de crash
            System.err.println("[OpenFDA] API non disponible : " + e.getMessage());
        }
        return alertes;
    }

    /**
     * Vérification COMPLÈTE : combine allergie + interactions locales + OpenFDA.
     *
     * Méthode principale appelée lors de l'ajout d'un médicament.
     * Orchestre les 3 niveaux de vérification dans l'ordre de priorité.
     *
     * @param nomNouveauProduit    Le médicament à ajouter
     * @param antecedents          Les antécédents médicaux du patient
     * @param userId               L'ID du patient
     * @param produitsDejaAjoutes  Les médicaments déjà dans le formulaire (pas encore en base)
     * @return Liste de toutes les alertes détectées (vide si tout est OK)
     */
    public List<String> verifierTout(String nomNouveauProduit, String antecedents, int userId, List<String> produitsDejaAjoutes) {
        List<String> alertes = new ArrayList<>();

        // Niveau 1 : Vérifier les allergies (antécédents textuels)
        String alerteAllergie = verifierAllergie(nomNouveauProduit, antecedents);
        if (alerteAllergie != null) alertes.add(alerteAllergie);

        // Niveau 2 : Vérifier les interactions avec les traitements ACTIFS en base
        alertes.addAll(verifierInteractionsLocales(nomNouveauProduit, userId));

        // Niveau 3 : Vérifier les interactions avec les produits DÉJÀ SÉLECTIONNÉS dans le formulaire
        // (pas encore en base, mais déjà dans la liste selectedProduits du controller)
        for (String produitAjoute : produitsDejaAjoutes) {
            // Extraire le nom du produit depuis le format "ID - Nom"
            String nomAjoute = produitAjoute.contains(" - ") ? produitAjoute.split(" - ")[1] : produitAjoute;
            String produitLower = nomNouveauProduit.toLowerCase();
            String ajouteLower = nomAjoute.toLowerCase();

            // Vérifier les interactions locales entre les deux produits
            for (String[] paire : INTERACTIONS_LOCALES) {
                boolean match1 = produitLower.contains(paire[0]) && ajouteLower.contains(paire[1]);
                boolean match2 = produitLower.contains(paire[1]) && ajouteLower.contains(paire[0]);
                if (match1 || match2) {
                    alertes.add("🚫 Interaction : \"" + nomNouveauProduit + "\" est incompatible avec \"" + nomAjoute + "\" (déjà sélectionné).");
                }
            }

            // Vérifier via OpenFDA pour cette paire
            alertes.addAll(verifierInteractionsOpenFDA(nomNouveauProduit, nomAjoute));
        }

        return alertes; // Liste de toutes les alertes (vide = tout est OK)
    }
}
