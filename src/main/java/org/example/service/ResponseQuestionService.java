package org.example.service;

import org.example.model.ActionType;
import org.example.model.AuteurType;
import org.example.model.ImpactStatut;
import org.example.model.ReponseRole;
import org.example.model.ResponseQuestion;
import org.example.model.ResponseQuestionFilter;
import org.example.model.ResponseQuestionPage;
import org.example.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class ResponseQuestionService {
    private String questionFkColumn;
    private String userFkColumn;

    public ResponseQuestionPage search(ResponseQuestionFilter filter, String sortColumn, boolean ascending, int pageIndex, int pageSize) {
        List<ResponseQuestion> items = new ArrayList<>();
        int totalCount = count(filter);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT rq.*, q.objet AS question_objet, ");
        sql.append("CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom ");
        sql.append("FROM response_question rq ");
        sql.append("JOIN question q ON rq.").append(getQuestionFkColumn()).append(" = q.id_question ");
        sql.append("LEFT JOIN utilisateur u ON rq.").append(getUserFkColumn()).append(" = u.id_utilisateur ");
        sql.append(buildWhereClause(filter));
        sql.append(" ORDER BY ").append(resolveSortColumn(sortColumn)).append(ascending ? " ASC" : " DESC");
        sql.append(" LIMIT ? OFFSET ?");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = bindFilterParams(stmt, filter, 1);
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex, pageIndex * pageSize);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ResponseQuestionPage(items, totalCount);
    }

    public List<ResponseQuestion> findAllFiltered(ResponseQuestionFilter filter, String sortColumn, boolean ascending) {
        List<ResponseQuestion> items = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT rq.*, q.objet AS question_objet, ");
        sql.append("CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom ");
        sql.append("FROM response_question rq ");
        sql.append("JOIN question q ON rq.").append(getQuestionFkColumn()).append(" = q.id_question ");
        sql.append("LEFT JOIN utilisateur u ON rq.").append(getUserFkColumn()).append(" = u.id_utilisateur ");
        sql.append(buildWhereClause(filter));
        sql.append(" ORDER BY ").append(resolveSortColumn(sortColumn)).append(ascending ? " ASC" : " DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            bindFilterParams(stmt, filter, 1);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    public ResponseQuestion getById(int id) {
        String sql = "SELECT rq.*, q.objet AS question_objet, " +
                "CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom " +
                "FROM response_question rq " +
                "JOIN question q ON rq." + getQuestionFkColumn() + " = q.id_question " +
                "LEFT JOIN utilisateur u ON rq." + getUserFkColumn() + " = u.id_utilisateur " +
                "WHERE rq.id_reponse = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ResponseQuestion getLatestByQuestionId(int questionId) {
        String sql = "SELECT rq.*, q.objet AS question_objet, " +
                "CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom " +
                "FROM response_question rq " +
                "JOIN question q ON rq." + getQuestionFkColumn() + " = q.id_question " +
                "LEFT JOIN utilisateur u ON rq." + getUserFkColumn() + " = u.id_utilisateur " +
                "WHERE rq." + getQuestionFkColumn() + " = ? " +
                "ORDER BY rq.created_at DESC LIMIT 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int create(ResponseQuestion response) {
        String sql = "INSERT INTO response_question " +
                "(" + getQuestionFkColumn() + ", " + getUserFkColumn() + ", auteur_type, reponse_text, reponse_role, action_type, impact_statut, " +
                "file_name, file_path, file_type, file_size, created_at, lu_par_client) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindUpsert(stmt, response, true);
            stmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBoolean(13, response.isLuParClient());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean update(ResponseQuestion response) {
        String sql = "UPDATE response_question SET " +
                getQuestionFkColumn() + " = ?, " + getUserFkColumn() + " = ?, auteur_type = ?, reponse_text = ?, reponse_role = ?, " +
                "action_type = ?, impact_statut = ?, file_name = ?, file_path = ?, file_type = ?, file_size = ?, " +
                "lu_par_client = ? WHERE id_reponse = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int index = bindUpsert(stmt, response, false);
            stmt.setBoolean(index++, response.isLuParClient());
            stmt.setInt(index, response.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM response_question WHERE id_reponse = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<ResponseQuestion> getResponsesForClient(int userId) {
        String questionUserFk = getQuestionUserFkColumn();
        String sql = "SELECT rq.*, q.objet AS question_objet, " +
                "CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom " +
                "FROM response_question rq " +
                "JOIN question q ON rq." + getQuestionFkColumn() + " = q.id_question " +
                "LEFT JOIN utilisateur u ON rq." + getUserFkColumn() + " = u.id_utilisateur " +
                "WHERE q." + questionUserFk + " = ? " +
                "ORDER BY rq.created_at DESC";

        List<ResponseQuestion> items = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public int countResponsesForClient(int userId) {
        String questionUserFk = getQuestionUserFkColumn();
        String sql = "SELECT COUNT(*) FROM response_question rq " +
                "JOIN question q ON rq." + getQuestionFkColumn() + " = q.id_question " +
                "WHERE q." + questionUserFk + " = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countUnreadResponsesForClient(int userId) {
        String questionUserFk = getQuestionUserFkColumn();
        String sql = "SELECT COUNT(*) FROM response_question rq " +
                "JOIN question q ON rq." + getQuestionFkColumn() + " = q.id_question " +
                "WHERE q." + questionUserFk + " = ? AND rq.lu_par_client = FALSE";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<ResponseQuestion> getLatestResponsesForClient(int userId, int limit) {
        String questionUserFk = getQuestionUserFkColumn();
        String sql = "SELECT rq.*, q.objet AS question_objet, " +
                "CONCAT(IFNULL(u.prenom, ''), ' ', IFNULL(u.nom, '')) AS utilisateur_nom " +
                "FROM response_question rq " +
                "JOIN question q ON rq." + getQuestionFkColumn() + " = q.id_question " +
                "LEFT JOIN utilisateur u ON rq." + getUserFkColumn() + " = u.id_utilisateur " +
                "WHERE q." + questionUserFk + " = ? " +
                "ORDER BY rq.lu_par_client ASC, rq.created_at DESC " +
                (limit > 0 ? "LIMIT ?" : "");

        List<ResponseQuestion> items = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            if (limit > 0) {
                stmt.setInt(2, limit);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public boolean markAsSeenForClient(int responseId, int userId) {
        String questionUserFk = getQuestionUserFkColumn();
        String sql = "UPDATE response_question rq " +
                "JOIN question q ON rq." + getQuestionFkColumn() + " = q.id_question " +
                "SET rq.lu_par_client = TRUE " +
                "WHERE rq.id_reponse = ? AND q." + questionUserFk + " = ? AND rq.lu_par_client = FALSE";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, responseId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int count(ResponseQuestionFilter filter) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM response_question rq ");
        sql.append(buildWhereClause(filter));

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            bindFilterParams(stmt, filter, 1);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String buildWhereClause(ResponseQuestionFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (filter != null) {
            if (filter.getQuestionId() != null) {
                where.append(" AND rq.").append(getQuestionFkColumn()).append(" = ? ");
            }
            if (filter.getAuteurType() != null) {
                where.append(" AND rq.auteur_type = ? ");
            }
            if (filter.getReponseRole() != null) {
                where.append(" AND rq.reponse_role = ? ");
            }
            if (filter.getActionType() != null) {
                where.append(" AND rq.action_type = ? ");
            }
            if (filter.getImpactStatut() != null) {
                where.append(" AND rq.impact_statut = ? ");
            }
            if (filter.getLuParClient() != null) {
                where.append(" AND rq.lu_par_client = ? ");
            }
            if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                where.append(" AND (rq.reponse_text LIKE ? OR q.objet LIKE ?) ");
            }
        }
        return where.toString();
    }

    private int bindFilterParams(PreparedStatement stmt, ResponseQuestionFilter filter, int startIndex) throws SQLException {
        int index = startIndex;
        if (filter != null) {
            if (filter.getQuestionId() != null) {
                stmt.setInt(index++, filter.getQuestionId());
            }
            if (filter.getAuteurType() != null) {
                stmt.setString(index++, filter.getAuteurType().getDbValue());
            }
            if (filter.getReponseRole() != null) {
                stmt.setString(index++, filter.getReponseRole().getDbValue());
            }
            if (filter.getActionType() != null) {
                stmt.setString(index++, filter.getActionType().getDbValue());
            }
            if (filter.getImpactStatut() != null) {
                stmt.setString(index++, filter.getImpactStatut().getDbValue());
            }
            if (filter.getLuParClient() != null) {
                stmt.setBoolean(index++, filter.getLuParClient());
            }
            if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                String like = "%" + filter.getSearchQuery().trim() + "%";
                stmt.setString(index++, like);
                stmt.setString(index++, like);
            }
        }
        return index;
    }

    private int bindUpsert(PreparedStatement stmt, ResponseQuestion response, boolean isCreate) throws SQLException {
        stmt.setInt(1, response.getQuestionId());
        if (response.getUtilisateurId() == null) {
            stmt.setNull(2, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(2, response.getUtilisateurId());
        }
        stmt.setString(3, response.getAuteurType().getDbValue());
        stmt.setString(4, response.getReponseText());
        stmt.setString(5, response.getReponseRole().getDbValue());
        stmt.setString(6, response.getActionType().getDbValue());
        stmt.setString(7, response.getImpactStatut().getDbValue());
        stmt.setString(8, response.getFileName());
        stmt.setString(9, response.getFilePath());
        stmt.setString(10, response.getFileType());
        if (response.getFileSize() == null) {
            stmt.setNull(11, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(11, response.getFileSize());
        }
        if (!isCreate) {
            return 12;
        }
        return 11;
    }

    private ResponseQuestion mapRow(ResultSet rs) throws SQLException {
        ResponseQuestion response = new ResponseQuestion();
        response.setId(rs.getInt("id_reponse"));
        response.setQuestionId(rs.getInt(getQuestionFkColumn()));
        response.setQuestionObjet(rs.getString("question_objet"));

        int userId = rs.getInt(getUserFkColumn());
        response.setUtilisateurId(rs.wasNull() ? null : userId);
        response.setUtilisateurNom(rs.getString("utilisateur_nom"));

        response.setAuteurType(AuteurType.fromDb(rs.getString("auteur_type")));
        response.setReponseText(rs.getString("reponse_text"));
        response.setReponseRole(ReponseRole.fromDb(rs.getString("reponse_role")));
        response.setActionType(ActionType.fromDb(rs.getString("action_type")));
        response.setImpactStatut(ImpactStatut.fromDb(rs.getString("impact_statut")));
        response.setFileName(rs.getString("file_name"));
        response.setFilePath(rs.getString("file_path"));
        response.setFileType(rs.getString("file_type"));

        int fileSize = rs.getInt("file_size");
        response.setFileSize(rs.wasNull() ? null : fileSize);

        Timestamp createdAt = rs.getTimestamp("created_at");
        response.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        response.setLuParClient(rs.getBoolean("lu_par_client"));

        return response;
    }

    private String resolveSortColumn(String sortColumn) {
        if (sortColumn == null) {
            return "rq.created_at";
        }
        switch (sortColumn) {
            case "id":
                return "rq.id_reponse";
            case "question":
                return "q.objet";
            case "auteurType":
                return "rq.auteur_type";
            case "reponseRole":
                return "rq.reponse_role";
            case "actionType":
                return "rq.action_type";
            case "impactStatut":
                return "rq.impact_statut";
            case "createdAt":
                return "rq.created_at";
            case "luParClient":
                return "rq.lu_par_client";
            default:
                return "rq.created_at";
        }
    }

    private String getQuestionFkColumn() {
        ensureColumnNames();
        return questionFkColumn;
    }

    private String getUserFkColumn() {
        ensureColumnNames();
        return userFkColumn;
    }

    private String getQuestionUserFkColumn() {
        String resolved = "id_utilisateur_id";
        try (Connection conn = DatabaseUtil.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, "question", null)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                if (column != null && column.equalsIgnoreCase("id_utilisateur")) {
                    resolved = "id_utilisateur";
                    break;
                }
            }
        } catch (SQLException e) {
            // keep default
        }
        return resolved;
    }

    private void ensureColumnNames() {
        if (questionFkColumn != null && userFkColumn != null) {
            return;
        }
        String resolvedQuestion = "id_question_id";
        String resolvedUser = "id_utilisateur_id";
        try (Connection conn = DatabaseUtil.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, "response_question", null)) {
            Set<String> columns = new HashSet<>();
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            if (columns.contains("id_question")) {
                resolvedQuestion = "id_question";
            }
            if (columns.contains("id_utilisateur")) {
                resolvedUser = "id_utilisateur";
            }
        } catch (SQLException e) {
            // keep defaults
        }
        questionFkColumn = resolvedQuestion;
        userFkColumn = resolvedUser;
    }
}
