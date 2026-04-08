package org.example.entities;

public class Ordonnance {

    private int idOrdonnance;
    private int idUtilisateur;
    private String numeroOrdonnance;
    private String dateOrdonnance;
    private String dateExpiration;
    private String statut;
    private String noteMedical;
    private boolean signatureElectronique;
    private String signatureDate;
    private String signatureMedecin;
    private String docusignEnvelopeId;
    private String docusignStatus;
    private String signatureDocumentPath;
    private String signaturePatient;
    private String signaturePatientDate;
    private String signaturePatientIp;

    public Ordonnance() {}

    public Ordonnance(int idUtilisateur, String numeroOrdonnance, String dateOrdonnance,
                      String dateExpiration, String statut, String noteMedical,
                      boolean signatureElectronique, String signatureMedecin) {
        this.idUtilisateur = idUtilisateur;
        this.numeroOrdonnance = numeroOrdonnance;
        this.dateOrdonnance = dateOrdonnance;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
        this.noteMedical = noteMedical;
        this.signatureElectronique = signatureElectronique;
        this.signatureMedecin = signatureMedecin;
    }

    public int getIdOrdonnance() { return idOrdonnance; }
    public void setIdOrdonnance(int idOrdonnance) { this.idOrdonnance = idOrdonnance; }
    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }
    public String getNumeroOrdonnance() { return numeroOrdonnance; }
    public void setNumeroOrdonnance(String numeroOrdonnance) { this.numeroOrdonnance = numeroOrdonnance; }
    public String getDateOrdonnance() { return dateOrdonnance; }
    public void setDateOrdonnance(String dateOrdonnance) { this.dateOrdonnance = dateOrdonnance; }
    public String getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(String dateExpiration) { this.dateExpiration = dateExpiration; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getNoteMedical() { return noteMedical; }
    public void setNoteMedical(String noteMedical) { this.noteMedical = noteMedical; }
    public boolean isSignatureElectronique() { return signatureElectronique; }
    public void setSignatureElectronique(boolean signatureElectronique) { this.signatureElectronique = signatureElectronique; }
    public String getSignatureDate() { return signatureDate; }
    public void setSignatureDate(String signatureDate) { this.signatureDate = signatureDate; }
    public String getSignatureMedecin() { return signatureMedecin; }
    public void setSignatureMedecin(String signatureMedecin) { this.signatureMedecin = signatureMedecin; }
    public String getDocusignEnvelopeId() { return docusignEnvelopeId; }
    public void setDocusignEnvelopeId(String docusignEnvelopeId) { this.docusignEnvelopeId = docusignEnvelopeId; }
    public String getDocusignStatus() { return docusignStatus; }
    public void setDocusignStatus(String docusignStatus) { this.docusignStatus = docusignStatus; }
    public String getSignatureDocumentPath() { return signatureDocumentPath; }
    public void setSignatureDocumentPath(String signatureDocumentPath) { this.signatureDocumentPath = signatureDocumentPath; }
    public String getSignaturePatient() { return signaturePatient; }
    public void setSignaturePatient(String signaturePatient) { this.signaturePatient = signaturePatient; }
    public String getSignaturePatientDate() { return signaturePatientDate; }
    public void setSignaturePatientDate(String signaturePatientDate) { this.signaturePatientDate = signaturePatientDate; }
    public String getSignaturePatientIp() { return signaturePatientIp; }
    public void setSignaturePatientIp(String signaturePatientIp) { this.signaturePatientIp = signaturePatientIp; }

    @Override
    public String toString() {
        return numeroOrdonnance + " - " + statut;
    }
}
