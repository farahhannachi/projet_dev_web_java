package org.example.services;

import org.example.entities.Ordonnance;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceService {

    private final Connection connection;

    public OrdonnanceService() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    public void insert(Ordonnance o) throws SQLException {
        String sql = "INSERT INTO ordonnance (id_utilisateur, numero_ordonnance, date_ordonnance, date_expiration, statut, note_medical, signature_electronique, signature_date, signature_medecin, docusign_envelope_id, docusign_status, signature_document_path, signature_patient, signature_patient_date, signature_patient_ip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, o.getIdUtilisateur());
        ps.setString(2, o.getNumeroOrdonnance());
        ps.setString(3, o.getDateOrdonnance());
        ps.setString(4, o.getDateExpiration());
        ps.setString(5, o.getStatut());
        ps.setString(6, o.getNoteMedical());
        ps.setBoolean(7, o.isSignatureElectronique());
        ps.setString(8, o.getSignatureDate());
        ps.setString(9, o.getSignatureMedecin());
        ps.setString(10, o.getDocusignEnvelopeId());
        ps.setString(11, o.getDocusignStatus());
        ps.setString(12, o.getSignatureDocumentPath());
        ps.setString(13, o.getSignaturePatient());
        ps.setString(14, o.getSignaturePatientDate());
        ps.setString(15, o.getSignaturePatientIp());
        ps.executeUpdate();
    }

    public List<Ordonnance> getAll() throws SQLException {
        List<Ordonnance> list = new ArrayList<>();
        String sql = "SELECT * FROM ordonnance";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public Ordonnance getById(int id) throws SQLException {
        String sql = "SELECT * FROM ordonnance WHERE id_ordonnance = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    public void update(Ordonnance o) throws SQLException {
        String sql = "UPDATE ordonnance SET id_utilisateur=?, numero_ordonnance=?, date_ordonnance=?, date_expiration=?, statut=?, note_medical=?, signature_electronique=?, signature_date=?, signature_medecin=?, docusign_envelope_id=?, docusign_status=?, signature_document_path=?, signature_patient=?, signature_patient_date=?, signature_patient_ip=? WHERE id_ordonnance=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, o.getIdUtilisateur());
        ps.setString(2, o.getNumeroOrdonnance());
        ps.setString(3, o.getDateOrdonnance());
        ps.setString(4, o.getDateExpiration());
        ps.setString(5, o.getStatut());
        ps.setString(6, o.getNoteMedical());
        ps.setBoolean(7, o.isSignatureElectronique());
        ps.setString(8, o.getSignatureDate());
        ps.setString(9, o.getSignatureMedecin());
        ps.setString(10, o.getDocusignEnvelopeId());
        ps.setString(11, o.getDocusignStatus());
        ps.setString(12, o.getSignatureDocumentPath());
        ps.setString(13, o.getSignaturePatient());
        ps.setString(14, o.getSignaturePatientDate());
        ps.setString(15, o.getSignaturePatientIp());
        ps.setInt(16, o.getIdOrdonnance());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ordonnance WHERE id_ordonnance = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Ordonnance mapRow(ResultSet rs) throws SQLException {
        Ordonnance o = new Ordonnance();
        o.setIdOrdonnance(rs.getInt("id_ordonnance"));
        o.setIdUtilisateur(rs.getInt("id_utilisateur"));
        o.setNumeroOrdonnance(rs.getString("numero_ordonnance"));
        o.setDateOrdonnance(rs.getString("date_ordonnance"));
        o.setDateExpiration(rs.getString("date_expiration"));
        o.setStatut(rs.getString("statut"));
        o.setNoteMedical(rs.getString("note_medical"));
        o.setSignatureElectronique(rs.getBoolean("signature_electronique"));
        o.setSignatureDate(rs.getString("signature_date"));
        o.setSignatureMedecin(rs.getString("signature_medecin"));
        o.setDocusignEnvelopeId(rs.getString("docusign_envelope_id"));
        o.setDocusignStatus(rs.getString("docusign_status"));
        o.setSignatureDocumentPath(rs.getString("signature_document_path"));
        o.setSignaturePatient(rs.getString("signature_patient"));
        o.setSignaturePatientDate(rs.getString("signature_patient_date"));
        o.setSignaturePatientIp(rs.getString("signature_patient_ip"));
        return o;
    }
}
