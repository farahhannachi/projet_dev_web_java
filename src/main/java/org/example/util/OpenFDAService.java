package org.example.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service OpenFDA : récupère effets secondaires, contre-indications et interactions
 * pour un médicament donné via l'API publique openFDA.
 */
public class OpenFDAService {

    private static OpenFDAService instance;
    private static final String BASE_URL = "https://api.fda.gov/drug/label.json";

    private OpenFDAService() {}

    public static OpenFDAService getInstance() {
        if (instance == null) instance = new OpenFDAService();
        return instance;
    }

    public static class DrugInfo {
        public String effetsSecondaires = "Non disponible";
        public String contreIndications = "Non disponible";
        public String interactions      = "Non disponible";
    }

    public DrugInfo getInfo(String nomMedicament) {
        DrugInfo info = new DrugInfo();
        if (nomMedicament == null || nomMedicament.isBlank()) return info;

        String nomNorm = normaliser(nomMedicament);

        // 1. Priorité : base locale (médicaments français/marocains connus)
        DrugInfo local = getInfoLocale(nomNorm);
        if (local != null) return local;

        try {
            // 2. Chercher par brand_name exact sur OpenFDA
            String json = fetchByField("openfda.brand_name", nomMedicament);
            // 3. Fallback : generic_name exact
            if (json == null) json = fetchByField("openfda.generic_name", nomMedicament);
            // 4. Fallback : brand_name normalisé
            if (json == null && !nomNorm.equals(nomMedicament.toLowerCase()))
                json = fetchByField("openfda.brand_name", nomNorm);
            // 5. Fallback : recherche libre
            if (json == null) {
                String query = URLEncoder.encode(nomNorm, StandardCharsets.UTF_8);
                json = fetch(BASE_URL + "?search=" + query + "&limit=1");
            }

            if (json != null && !json.contains("\"error\"")) {
                String effets = extraire(json, "adverse_reactions");
                String contre = extraire(json, "contraindications");
                String inter  = extraire(json, "drug_interactions");
                // N'utiliser le résultat OpenFDA que si au moins un champ est renseigné
                if (!effets.equals("Non renseigné") || !contre.equals("Non renseigné") || !inter.equals("Non renseigné")) {
                    info.effetsSecondaires = effets;
                    info.contreIndications = contre;
                    info.interactions      = inter;
                    return info;
                }
            }
        } catch (Exception e) {
            System.err.println("[OpenFDA] Erreur : " + e.getMessage());
        }

        return info;
    }

    /**
     * Base locale de données médicamenteuses fiables.
     * Sources : drugs.com, medicines.org.uk (emc), NIH/NLM
     */
    private DrugInfo getInfoLocale(String nomNorm) {
        DrugInfo info = new DrugInfo();

        // ── PARACÉTAMOL / PANADOL / DOLIPRANE / EFFERALGAN ──────────────
        if (nomNorm.contains("paracetamol") || nomNorm.contains("panadol") ||
            nomNorm.contains("doliprane") || nomNorm.contains("efferalgan") ||
            nomNorm.contains("dafalgan") || nomNorm.contains("tylenol") ||
            nomNorm.contains("claradol") || nomNorm.contains("calpol")) {
            info.effetsSecondaires =
                "Rares : éruption cutanée, urticaire, réactions allergiques. " +
                "Très rares : thrombocytopénie, agranulocytose. " +
                "En cas de surdosage : atteinte hépatique grave (nécrose hépatique), " +
                "insuffisance rénale. Nausées, vomissements possibles à forte dose. " +
                "Source : medicines.org.uk (emc) / drugs.com";
            info.contreIndications =
                "Hypersensibilité au paracétamol. Insuffisance hépatocellulaire sévère. " +
                "Alcoolisme chronique (risque de toxicité hépatique accrue). " +
                "Ne pas dépasser 4g/jour chez l'adulte. " +
                "Prudence chez les patients sous anticoagulants (warfarine). " +
                "Source : medicines.org.uk (emc)";
            info.interactions =
                "Warfarine/anticoagulants : potentialisation de l'effet anticoagulant à doses élevées. " +
                "Alcool : risque accru de toxicité hépatique. " +
                "Métoclopramide/dompéridone : absorption accélérée du paracétamol. " +
                "Cholestyramine : absorption réduite. " +
                "Isoniazide : risque de toxicité hépatique. " +
                "Source : drugs.com / medicines.org.uk";
            return info;
        }

        // ── IBUPROFÈNE / ADVIL / NUROFEN / BRUFEN ───────────────────────
        if (nomNorm.contains("ibuprofene") || nomNorm.contains("ibuprofen") ||
            nomNorm.contains("advil") || nomNorm.contains("nurofen") ||
            nomNorm.contains("brufen") || nomNorm.contains("motrin") ||
            nomNorm.contains("antarene")) {
            info.effetsSecondaires =
                "Fréquents : douleurs gastriques, nausées, dyspepsie, diarrhée, constipation. " +
                "Moins fréquents : ulcère gastroduodénal, hémorragie digestive, céphalées, vertiges. " +
                "Rares : réactions allergiques graves (anaphylaxie), bronchospasme chez asthmatiques. " +
                "Risque cardiovasculaire accru (infarctus, AVC) à long terme. " +
                "Source : drugs.com / NIH";
            info.contreIndications =
                "Allergie aux AINS ou à l'aspirine (risque de bronchospasme). " +
                "Ulcère gastroduodénal actif ou antécédents d'hémorragie digestive. " +
                "Insuffisance rénale, hépatique ou cardiaque sévère. " +
                "3ème trimestre de grossesse. Chirurgie cardiaque (pontage coronarien). " +
                "Enfants < 3 mois. " +
                "Source : drugs.com / medicines.org.uk";
            info.interactions =
                "Aspirine : augmentation du risque hémorragique, réduction de l'effet cardioprotecteur de l'aspirine. " +
                "Anticoagulants (warfarine) : risque hémorragique accru. " +
                "Lithium : augmentation des taux plasmatiques de lithium. " +
                "Méthotrexate : toxicité accrue. " +
                "Antihypertenseurs (IEC, ARA2) : réduction de l'effet antihypertenseur. " +
                "Corticoïdes : risque accru d'ulcère. " +
                "Source : drugs.com";
            return info;
        }

        // ── ASPIRINE / ASPÉGIC / KARDÉGIC ───────────────────────────────
        if (nomNorm.contains("aspirine") || nomNorm.contains("aspirin") ||
            nomNorm.contains("aspegic") || nomNorm.contains("kardegic") ||
            nomNorm.contains("cardioaspirine")) {
            info.effetsSecondaires =
                "Fréquents : irritation gastrique, nausées, vomissements, hémorragie digestive. " +
                "Rares : réactions allergiques, bronchospasme, syndrome de Reye chez l'enfant. " +
                "Acouphènes et vertiges en cas de surdosage. " +
                "Source : drugs.com";
            info.contreIndications =
                "Allergie aux salicylés ou AINS. Ulcère gastroduodénal actif. " +
                "Troubles de la coagulation. Enfants < 16 ans (risque de syndrome de Reye). " +
                "3ème trimestre de grossesse. Insuffisance rénale ou hépatique sévère. " +
                "Source : drugs.com / medicines.org.uk";
            info.interactions =
                "Anticoagulants : risque hémorragique majeur. " +
                "Ibuprofène/AINS : antagonisme et risque hémorragique accru. " +
                "Méthotrexate : toxicité accrue. " +
                "Lithium : augmentation des taux de lithium. " +
                "Antidiabétiques oraux : potentialisation de l'effet hypoglycémiant. " +
                "Source : drugs.com";
            return info;
        }

        // ── AMOXICILLINE / CLAMOXYL / AUGMENTIN ─────────────────────────
        if (nomNorm.contains("amoxicilline") || nomNorm.contains("amoxicillin") ||
            nomNorm.contains("clamoxyl") || nomNorm.contains("augmentin") ||
            nomNorm.contains("amoxil")) {
            info.effetsSecondaires =
                "Fréquents : diarrhée, nausées, vomissements, éruption cutanée. " +
                "Moins fréquents : urticaire, candidose buccale ou vaginale. " +
                "Rares : réactions anaphylactiques, colite pseudomembraneuse, " +
                "hépatite cholestatique (surtout avec acide clavulanique). " +
                "Source : drugs.com / medicines.org.uk";
            info.contreIndications =
                "Allergie aux pénicillines ou céphalosporines (risque d'allergie croisée). " +
                "Antécédents de jaunisse ou atteinte hépatique liée à l'amoxicilline. " +
                "Mononucléose infectieuse (risque d'éruption cutanée). " +
                "Source : medicines.org.uk (emc)";
            info.interactions =
                "Méthotrexate : réduction de l'élimination, toxicité accrue. " +
                "Anticoagulants oraux (warfarine) : potentialisation possible. " +
                "Contraceptifs oraux : réduction possible de l'efficacité. " +
                "Probénécide : augmentation des taux plasmatiques d'amoxicilline. " +
                "Source : drugs.com";
            return info;
        }

        // ── AZITHROMYCINE / ZITHROMAX ────────────────────────────────────
        if (nomNorm.contains("azithromycine") || nomNorm.contains("azithromycin") ||
            nomNorm.contains("zithromax") || nomNorm.contains("zitromax")) {
            info.effetsSecondaires =
                "Fréquents : diarrhée, nausées, douleurs abdominales, vomissements. " +
                "Moins fréquents : céphalées, vertiges, éruption cutanée. " +
                "Rares : allongement de l'intervalle QT (risque cardiaque), " +
                "hépatotoxicité, réactions allergiques graves. " +
                "Source : drugs.com";
            info.contreIndications =
                "Allergie aux macrolides. Insuffisance hépatique sévère. " +
                "Antécédents d'arythmie cardiaque ou allongement du QT. " +
                "Association avec des médicaments allongeant le QT. " +
                "Source : drugs.com / medicines.org.uk";
            info.interactions =
                "Antiacides contenant aluminium/magnésium : réduction de l'absorption. " +
                "Warfarine : potentialisation de l'effet anticoagulant. " +
                "Digoxine : augmentation des taux de digoxine. " +
                "Médicaments allongeant le QT (amiodarone, quinidine) : risque d'arythmie. " +
                "Source : drugs.com";
            return info;
        }

        // ── OMÉPRAZOLE / MOPRAL / PANTOPRAZOLE ──────────────────────────
        if (nomNorm.contains("omeprazole") || nomNorm.contains("mopral") ||
            nomNorm.contains("pantoprazole") || nomNorm.contains("losec") ||
            nomNorm.contains("prilosec")) {
            info.effetsSecondaires =
                "Fréquents : céphalées, diarrhée, nausées, douleurs abdominales, constipation. " +
                "Moins fréquents : sécheresse buccale, vertiges, insomnie. " +
                "Long terme : carence en magnésium, vitamine B12, risque de fractures osseuses. " +
                "Source : drugs.com / NIH";
            info.contreIndications =
                "Hypersensibilité aux inhibiteurs de la pompe à protons. " +
                "Association avec nelfinavir (antirétroviral). " +
                "Prudence en cas d'insuffisance hépatique sévère. " +
                "Source : medicines.org.uk";
            info.interactions =
                "Clopidogrel : réduction de l'effet antiplaquettaire (éviter l'association). " +
                "Méthotrexate : augmentation des taux plasmatiques. " +
                "Kétoconazole/itraconazole : absorption réduite. " +
                "Digoxine : légère augmentation des taux. " +
                "Warfarine : potentialisation possible. " +
                "Source : drugs.com";
            return info;
        }

        // ── CÉTIRIZINE / ZYRTEC / LORATADINE / CLARITYNE ────────────────
        if (nomNorm.contains("cetirizine") || nomNorm.contains("zyrtec") ||
            nomNorm.contains("virlix") || nomNorm.contains("reactine")) {
            info.effetsSecondaires =
                "Fréquents : somnolence (modérée), sécheresse buccale, céphalées, vertiges. " +
                "Moins fréquents : fatigue, agitation chez l'enfant, troubles digestifs. " +
                "Source : drugs.com";
            info.contreIndications =
                "Hypersensibilité à la cétirizine ou à l'hydroxyzine. " +
                "Insuffisance rénale sévère (adapter la dose). " +
                "Prudence lors de la conduite (somnolence possible). " +
                "Source : medicines.org.uk";
            info.interactions =
                "Alcool et dépresseurs du SNC : potentialisation de la somnolence. " +
                "Théophylline : légère réduction de la clairance de la cétirizine. " +
                "Source : drugs.com";
            return info;
        }

        if (nomNorm.contains("loratadine") || nomNorm.contains("clarityne") ||
            nomNorm.contains("claritin")) {
            info.effetsSecondaires =
                "Très peu sédatif. Fréquents : céphalées, sécheresse buccale, fatigue. " +
                "Rares : tachycardie, réactions allergiques. " +
                "Source : drugs.com";
            info.contreIndications =
                "Hypersensibilité à la loratadine. " +
                "Insuffisance hépatique sévère (réduire la fréquence). " +
                "Source : medicines.org.uk";
            info.interactions =
                "Kétoconazole, érythromycine, cimétidine : augmentation des taux de loratadine. " +
                "Alcool : somnolence légèrement potentialisée. " +
                "Source : drugs.com";
            return info;
        }

        // ── METFORMINE / GLUCOPHAGE ──────────────────────────────────────
        if (nomNorm.contains("metformine") || nomNorm.contains("metformin") ||
            nomNorm.contains("glucophage") || nomNorm.contains("stagid")) {
            info.effetsSecondaires =
                "Très fréquents : nausées, vomissements, diarrhée, douleurs abdominales " +
                "(surtout en début de traitement, diminuent avec le temps). " +
                "Rare mais grave : acidose lactique (surtout si insuffisance rénale). " +
                "Carence en vitamine B12 à long terme. " +
                "Source : drugs.com / NIH";
            info.contreIndications =
                "Insuffisance rénale (DFG < 30 mL/min). Insuffisance hépatique. " +
                "Alcoolisme. Acidose métabolique. " +
                "Suspendre avant injection de produit de contraste iodé. " +
                "Source : medicines.org.uk";
            info.interactions =
                "Alcool : risque accru d'acidose lactique. " +
                "Produits de contraste iodés : risque d'insuffisance rénale aiguë et d'acidose lactique. " +
                "Cimétidine : augmentation des taux de metformine. " +
                "Diurétiques, AINS : risque d'insuffisance rénale. " +
                "Source : drugs.com";
            return info;
        }

        // ── TRAMADOL / TOPALGIC / CONTRAMAL ─────────────────────────────
        if (nomNorm.contains("tramadol") || nomNorm.contains("topalgic") ||
            nomNorm.contains("contramal") || nomNorm.contains("zamudol")) {
            info.effetsSecondaires =
                "Très fréquents : nausées, vertiges, somnolence, céphalées, constipation. " +
                "Fréquents : vomissements, sécheresse buccale, sudation. " +
                "Graves : dépression respiratoire (surdosage), convulsions, " +
                "syndrome sérotoninergique, dépendance. " +
                "Source : drugs.com / medicines.org.uk";
            info.contreIndications =
                "Hypersensibilité au tramadol. Épilepsie non contrôlée. " +
                "Association avec IMAO. Insuffisance respiratoire sévère. " +
                "Enfants < 12 ans. Grossesse et allaitement. " +
                "Source : medicines.org.uk";
            info.interactions =
                "IMAO : syndrome sérotoninergique grave (contre-indication absolue). " +
                "Antidépresseurs (ISRS, IRSN) : risque de syndrome sérotoninergique. " +
                "Fluoxétine/sertraline : interaction sérotoninergique. " +
                "Alcool et dépresseurs du SNC : potentialisation de la dépression respiratoire. " +
                "Carbamazépine : réduction de l'effet analgésique. " +
                "Source : drugs.com";
            return info;
        }

        // ── WARFARINE / COUMADINE ────────────────────────────────────────
        if (nomNorm.contains("warfarine") || nomNorm.contains("warfarin") ||
            nomNorm.contains("coumadine")) {
            info.effetsSecondaires =
                "Principal risque : hémorragies (saignements cutanés, digestifs, cérébraux). " +
                "Nécrose cutanée (rare). Alopécie. " +
                "Source : drugs.com";
            info.contreIndications =
                "Grossesse (tératogène). Hémorragie active. " +
                "Hypertension artérielle sévère non contrôlée. " +
                "Chirurgie récente du SNC ou de l'œil. " +
                "Source : medicines.org.uk";
            info.interactions =
                "Nombreuses interactions : AINS, aspirine, antibiotiques, antifongiques, " +
                "amiodarone, statines, paracétamol à forte dose. " +
                "Surveillance INR obligatoire lors de tout changement de traitement. " +
                "Source : drugs.com";
            return info;
        }

        // ── SIMVASTATINE / ZOCOR / ATORVASTATINE / TAHOR ────────────────
        if (nomNorm.contains("simvastatine") || nomNorm.contains("simvastatin") ||
            nomNorm.contains("zocor") || nomNorm.contains("atorvastatine") ||
            nomNorm.contains("atorvastatin") || nomNorm.contains("tahor") ||
            nomNorm.contains("lodales")) {
            info.effetsSecondaires =
                "Fréquents : myalgies (douleurs musculaires), céphalées, troubles digestifs. " +
                "Rare mais grave : rhabdomyolyse (destruction musculaire), hépatotoxicité. " +
                "Augmentation des enzymes hépatiques. " +
                "Source : drugs.com / NIH";
            info.contreIndications =
                "Maladie hépatique active. Grossesse et allaitement. " +
                "Association avec certains médicaments (voir interactions). " +
                "Source : medicines.org.uk";
            info.interactions =
                "Érythromycine/clarithromycine : risque accru de rhabdomyolyse. " +
                "Antifongiques azolés (kétoconazole) : risque accru de rhabdomyolyse. " +
                "Amiodarone : augmentation du risque de myopathie. " +
                "Warfarine : potentialisation de l'effet anticoagulant. " +
                "Jus de pamplemousse : augmentation des taux plasmatiques. " +
                "Source : drugs.com";
            return info;
        }

        // ── AÉROSOL / SALBUTAMOL / VENTOLINE ────────────────────────────
        if (nomNorm.contains("aerol") || nomNorm.contains("salbutamol") ||
            nomNorm.contains("ventoline") || nomNorm.contains("albuterol") ||
            nomNorm.contains("ventolin")) {
            info.effetsSecondaires =
                "Fréquents : tremblements des mains, tachycardie, palpitations, céphalées. " +
                "Moins fréquents : crampes musculaires, hypokaliémie à forte dose. " +
                "Paradoxal : bronchospasme paradoxal (rare). " +
                "Source : drugs.com / medicines.org.uk";
            info.contreIndications =
                "Hypersensibilité au salbutamol. " +
                "Prudence en cas de cardiopathie, hyperthyroïdie, diabète. " +
                "Source : medicines.org.uk";
            info.interactions =
                "Bêtabloquants : antagonisme (réduction de l'effet bronchodilatateur). " +
                "Diurétiques thiazidiques : risque d'hypokaliémie. " +
                "IMAO et antidépresseurs tricycliques : risque cardiovasculaire. " +
                "Source : drugs.com";
            return info;
        }

        return null; // Médicament non trouvé dans la base locale
    }

    private String fetchByField(String field, String value) {
        try {
            String query = URLEncoder.encode(field + ":\"" + value + "\"", StandardCharsets.UTF_8);
            return fetch(BASE_URL + "?search=" + query + "&limit=1");
        } catch (Exception e) {
            return null;
        }
    }

    /** Normalise un nom de médicament : minuscules, sans accents */
    private String normaliser(String nom) {
        return java.text.Normalizer.normalize(nom, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim();
    }

    private String fetch(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != 200) { conn.disconnect(); return null; }
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrait la valeur d'un champ JSON (tableau de strings).
     * Gère les deux formats : "field":["value"] et "field": ["value"]
     */
    private String extraire(String json, String champ) {
        try {
            // Chercher le champ avec ou sans espace avant le crochet
            int start = -1;
            for (String pattern : new String[]{
                    "\"" + champ + "\":[\"",
                    "\"" + champ + "\": [\""}) {
                start = json.indexOf(pattern);
                if (start != -1) {
                    start += pattern.length();
                    break;
                }
            }
            if (start == -1) return "Non renseigné";

            int end = json.indexOf("\"]", start);
            if (end == -1) end = Math.min(start + 600, json.length());

            String val = json.substring(start, end)
                    .replace("\\n", " ")
                    .replace("\\r", "")
                    .replace("\\t", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            return val.length() > 350 ? val.substring(0, 350) + "..." : val;
        } catch (Exception e) {
            return "Non disponible";
        }
    }
}
