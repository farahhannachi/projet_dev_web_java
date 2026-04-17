package org.example.service;

import org.example.model.Question;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestionService {
    private String userFkColumn;
    private String responseQuestionFkColumn;

    public List<Question> getAllQuestions() {
        return searchQuestions(null, null, null);
    }

    /**
     * Recherche/filtre des questions (utilisé par le dashboard admin).
     * - search: recherche sur objet/description + champs client (nom/prenom/email)
     * - statut/priorite: filtres exacts (valeurs DB en minuscules)
     */
    public List<Question> searchQuestions(String search, String statut, String priorite) {
        List<Question> questions = new ArrayList<>();
        String userCol = getUserFkColumn();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT q.id_question, q.type_ticket, q.objet, q.description, q.priorite, q.statut, ");
        sql.append("q.file_name, q.file_path, q.file_type, q.file_size, q.created_at, ");
        sql.append("q.").append(userCol).append(" AS utilisateur_id, ");
        sql.append("CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom, ");
        sql.append("u.email AS utilisateur_email ");
        sql.append("FROM question q ");
        sql.append("LEFT JOIN utilisateur u ON q.").append(userCol).append(" = u.id_utilisateur ");
        sql.append("WHERE 1=1 ");

        if (statut != null && !statut.isBlank()) {
            sql.append("AND q.statut = ? ");
        }
        if (priorite != null && !priorite.isBlank()) {
            sql.append("AND q.priorite = ? ");
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (q.objet LIKE ? OR q.description LIKE ? OR u.nom LIKE ? OR u.prenom LIKE ? OR u.email LIKE ?) ");
        }

        sql.append("ORDER BY q.id_question DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (statut != null && !statut.isBlank()) {
                stmt.setString(idx++, statut.trim());
            }
            if (priorite != null && !priorite.isBlank()) {
                stmt.setString(idx++, priorite.trim());
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim() + "%";
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
                stmt.setString(idx++, like);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return questions;
    }

    public boolean createQuestion(String typeTicket,
                                 String objet,
                                 String description,
                                 String priorite,
                                 String statut,
                                 String fileName,
                                 String filePath,
                                 String fileType,
                                 Integer fileSize,
                                 java.time.LocalDateTime createdAt,
                                 Integer utilisateurId) {
        String sql = "INSERT INTO question (type_ticket, objet, description, priorite, statut, file_name, file_path, file_type, file_size, created_at, "
                + getUserFkColumn() + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, typeTicket);
            stmt.setString(2, objet);
            stmt.setString(3, description);
            stmt.setString(4, priorite);
            stmt.setString(5, statut);
            stmt.setString(6, fileName);
            stmt.setString(7, filePath);
            stmt.setString(8, fileType);
            if (fileSize == null) {
                stmt.setNull(9, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(9, fileSize);
            }
            stmt.setTimestamp(10, java.sql.Timestamp.valueOf(createdAt));
            if (utilisateurId == null) {
                stmt.setNull(11, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(11, utilisateurId);
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Utilisé par d'anciennes vues : conserve la logique mais retourne maintenant des Questions enrichies.
     */
    public List<Question> getQuestionsByAnswered(boolean answered) {
        List<Question> questions = new ArrayList<>();
        String joinColumn = getResponseQuestionFkColumn();
        String userCol = getUserFkColumn();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT q.id_question, q.type_ticket, q.objet, q.description, q.priorite, q.statut, ");
        sql.append("q.file_name, q.file_path, q.file_type, q.file_size, q.created_at, ");
        sql.append("q.").append(userCol).append(" AS utilisateur_id, ");
        sql.append("CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom, ");
        sql.append("u.email AS utilisateur_email ");
        sql.append("FROM question q ");
        sql.append("LEFT JOIN response_question rq ON rq.").append(joinColumn).append(" = q.id_question ");
        sql.append("LEFT JOIN utilisateur u ON q.").append(userCol).append(" = u.id_utilisateur ");
        sql.append("WHERE ").append(answered ? "rq.id_reponse IS NOT NULL" : "rq.id_reponse IS NULL").append(' ');
        sql.append("GROUP BY q.id_question ");
        sql.append("ORDER BY q.id_question DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString());
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                questions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return questions;
    }

    /**
     * Tickets côté client ("Mes tickets").
     */
    public List<Question> getQuestionsForUser(int userId, int limit) {
        List<Question> questions = new ArrayList<>();
        String userCol = getUserFkColumn();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT q.id_question, q.type_ticket, q.objet, q.description, q.priorite, q.statut, ");
        sql.append("q.file_name, q.file_path, q.file_type, q.file_size, q.created_at, ");
        sql.append("q.").append(userCol).append(" AS utilisateur_id, ");
        sql.append("CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom, ");
        sql.append("u.email AS utilisateur_email ");
        sql.append("FROM question q ");
        sql.append("LEFT JOIN utilisateur u ON q.").append(userCol).append(" = u.id_utilisateur ");
        sql.append("WHERE q.").append(userCol).append(" = ? ");
        sql.append("ORDER BY q.id_question DESC ");
        if (limit > 0) {
            sql.append("LIMIT ?");
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setInt(1, userId);
            if (limit > 0) {
                stmt.setInt(2, limit);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return questions;
    }

    public int countQuestionsForUser(int userId) {
        String userCol = getUserFkColumn();
        String sql = "SELECT COUNT(*) AS cnt FROM question q WHERE q." + userCol + " = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        Question q = new Question(
                rs.getInt("id_question"),
                rs.getString("objet"),
                rs.getString("type_ticket"),
                rs.getString("priorite"),
                rs.getString("statut"),
                createdAt != null ? createdAt.toLocalDateTime() : null
        );

        q.setDescription(rs.getString("description"));
        q.setFileName(rs.getString("file_name"));
        q.setFilePath(rs.getString("file_path"));
        q.setFileType(rs.getString("file_type"));

        int fs = rs.getInt("file_size");
        q.setFileSize(rs.wasNull() ? null : fs);

        int uid = rs.getInt("utilisateur_id");
        q.setUtilisateurId(rs.wasNull() ? null : uid);
        String nom = rs.getString("utilisateur_nom");
        q.setUtilisateurNom(nom != null ? nom.trim() : null);
        q.setUtilisateurEmail(rs.getString("utilisateur_email"));

        return q;
    }

    private String getUserFkColumn() {
        if (userFkColumn != null) {
            return userFkColumn;
        }
        String resolved = "id_utilisateur_id";
        try (Connection conn = DatabaseUtil.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, "question", null)) {
            Set<String> columns = new HashSet<>();
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            if (columns.contains("id_utilisateur")) {
                resolved = "id_utilisateur";
            }
        } catch (SQLException e) {
            // keep default
        }
        userFkColumn = resolved;
        return userFkColumn;
    }

    private String getResponseQuestionFkColumn() {
        if (responseQuestionFkColumn != null) {
            return responseQuestionFkColumn;
        }
        String resolved = "id_question_id";
        try (Connection conn = DatabaseUtil.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, "response_question", null)) {
            Set<String> columns = new HashSet<>();
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            if (columns.contains("id_question")) {
                resolved = "id_question";
            }
        } catch (SQLException e) {
            // keep default
        }
        responseQuestionFkColumn = resolved;
        return responseQuestionFkColumn;
    }
}
