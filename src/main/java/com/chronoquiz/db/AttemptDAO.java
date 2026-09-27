package com.chronoquiz.db;

import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.AttemptAnswer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for past quiz attempts and their per-question answers.
 */
public class AttemptDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public boolean saveAttempt(Attempt attempt) {
        String insertAttemptSql = """
            INSERT INTO attempts (user_id, category_id, score, total, time_taken_sec)
            VALUES (?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Resolve or verify user_id
            Integer validUserId = null;
            if (attempt.getUserId() > 0) {
                try (PreparedStatement psUser = conn.prepareStatement("SELECT id FROM users WHERE id = ?;")) {
                    psUser.setInt(1, attempt.getUserId());
                    try (ResultSet rs = psUser.executeQuery()) {
                        if (rs.next()) {
                            validUserId = attempt.getUserId();
                        }
                    }
                }
            }
            if (validUserId == null && attempt.getUserName() != null && !attempt.getUserName().trim().isEmpty()) {
                try (PreparedStatement psName = conn.prepareStatement("SELECT id FROM users WHERE name = ? COLLATE NOCASE LIMIT 1;")) {
                    psName.setString(1, attempt.getUserName().trim());
                    try (ResultSet rs = psName.executeQuery()) {
                        if (rs.next()) {
                            validUserId = rs.getInt("id");
                        }
                    }
                }
            }

            // 2. Resolve or verify category_id
            Integer validCategoryId = null;
            if (attempt.getCategoryId() > 0) {
                try (PreparedStatement psCat = conn.prepareStatement("SELECT id FROM categories WHERE id = ?;")) {
                    psCat.setInt(1, attempt.getCategoryId());
                    try (ResultSet rs = psCat.executeQuery()) {
                        if (rs.next()) {
                            validCategoryId = attempt.getCategoryId();
                        }
                    }
                }
            }
            if (validCategoryId == null && attempt.getCategoryName() != null
                && !attempt.getCategoryName().equalsIgnoreCase("All Categories")
                && !attempt.getCategoryName().trim().isEmpty()) {
                try (PreparedStatement psCatName = conn.prepareStatement("SELECT id FROM categories WHERE name = ? COLLATE NOCASE LIMIT 1;")) {
                    psCatName.setString(1, attempt.getCategoryName().trim());
                    try (ResultSet rs = psCatName.executeQuery()) {
                        if (rs.next()) {
                            validCategoryId = rs.getInt("id");
                        }
                    }
                }
            }

            try (PreparedStatement psAttempt = conn.prepareStatement(insertAttemptSql, Statement.RETURN_GENERATED_KEYS)) {
                if (validUserId != null) {
                    psAttempt.setInt(1, validUserId);
                } else {
                    psAttempt.setNull(1, java.sql.Types.INTEGER);
                }

                if (validCategoryId != null) {
                    psAttempt.setInt(2, validCategoryId);
                } else {
                    psAttempt.setNull(2, java.sql.Types.INTEGER);
                }

                psAttempt.setInt(3, attempt.getScore());
                psAttempt.setInt(4, attempt.getTotal());
                psAttempt.setInt(5, attempt.getTimeTakenSec());
                psAttempt.executeUpdate();

                int attemptId = 0;
                try (ResultSet rs = psAttempt.getGeneratedKeys()) {
                    if (rs.next()) {
                        attemptId = rs.getInt(1);
                        attempt.setId(attemptId);
                    }
                }

                if (attemptId > 0 && attempt.getAnswers() != null && !attempt.getAnswers().isEmpty()) {
                    boolean hasTextCols = false;
                    try (Statement checkStmt = conn.createStatement();
                         ResultSet rsCols = checkStmt.executeQuery("PRAGMA table_info(attempt_answers);")) {
                        while (rsCols.next()) {
                            if ("question_text".equalsIgnoreCase(rsCols.getString("name"))) {
                                hasTextCols = true;
                                break;
                            }
                        }
                    }

                    String insertAnswerSql = hasTextCols
                        ? "INSERT INTO attempt_answers (attempt_id, question_id, question_text, correct_answer, user_answer, is_correct) VALUES (?, ?, ?, ?, ?, ?);"
                        : "INSERT INTO attempt_answers (attempt_id, question_id, user_answer, is_correct) VALUES (?, ?, ?, ?);";

                    try (PreparedStatement psAnswer = conn.prepareStatement(insertAnswerSql)) {
                        for (AttemptAnswer ans : attempt.getAnswers()) {
                            psAnswer.setInt(1, attemptId);

                            // Verify question_id exists in questions table to avoid foreign key failure
                            Integer validQId = null;
                            if (ans.getQuestionId() > 0) {
                                try (PreparedStatement psQCheck = conn.prepareStatement("SELECT id FROM questions WHERE id = ?;")) {
                                    psQCheck.setInt(1, ans.getQuestionId());
                                    try (ResultSet rs = psQCheck.executeQuery()) {
                                        if (rs.next()) {
                                            validQId = ans.getQuestionId();
                                        }
                                    }
                                }
                            }

                            if (validQId != null) {
                                psAnswer.setInt(2, validQId);
                            } else {
                                psAnswer.setNull(2, java.sql.Types.INTEGER);
                            }

                            if (hasTextCols) {
                                psAnswer.setString(3, ans.getQuestionText());
                                psAnswer.setString(4, ans.getCorrectAnswer());
                                psAnswer.setString(5, ans.getUserAnswer());
                                psAnswer.setInt(6, ans.isCorrect() ? 1 : 0);
                            } else {
                                psAnswer.setString(3, ans.getUserAnswer());
                                psAnswer.setInt(4, ans.isCorrect() ? 1 : 0);
                            }
                            psAnswer.addBatch();
                        }
                        psAnswer.executeBatch();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error saving attempt: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Attempt> getAllAttempts(Integer categoryFilterId) {
        List<Attempt> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT a.id, a.user_id, COALESCE(u.name, 'Player') as user_name,
                   a.category_id, COALESCE(c.name, 'All Categories') as category_name,
                   a.score, a.total, a.time_taken_sec, a.taken_at
            FROM attempts a
            LEFT JOIN users u ON a.user_id = u.id
            LEFT JOIN categories c ON a.category_id = c.id
        """);

        if (categoryFilterId != null && categoryFilterId > 0) {
            sql.append(" WHERE a.category_id = ?");
        }
        sql.append(" ORDER BY a.id DESC;");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (categoryFilterId != null && categoryFilterId > 0) {
                ps.setInt(1, categoryFilterId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attempt a = new Attempt(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("user_name"),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getInt("score"),
                        rs.getInt("total"),
                        rs.getInt("time_taken_sec"),
                        rs.getString("taken_at")
                    );
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching attempts: " + e.getMessage());
        }
        return list;
    }

    public List<AttemptAnswer> getAnswersForAttempt(int attemptId) {
        List<AttemptAnswer> list = new ArrayList<>();
        String sql = """
            SELECT aa.id, aa.attempt_id, aa.question_id, aa.user_answer, aa.is_correct,
                   COALESCE(aa.question_text, q.text, 'Question text not available') as question_text,
                   COALESCE(aa.correct_answer, q.correct_answer, '') as correct_answer
            FROM attempt_answers aa
            LEFT JOIN questions q ON aa.question_id = q.id
            WHERE aa.attempt_id = ?
            ORDER BY aa.id ASC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AttemptAnswer(
                        rs.getInt("id"),
                        rs.getInt("attempt_id"),
                        rs.getInt("question_id"),
                        rs.getString("question_text"),
                        rs.getString("user_answer"),
                        rs.getString("correct_answer"),
                        rs.getInt("is_correct") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading attempt answers: " + e.getMessage());
        }
        return list;
    }

    public Map<String, Object> getHistoryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        String sql = """
            SELECT COUNT(*) as total_attempts,
                   MAX(ROUND(CAST(score AS FLOAT) / total * 100, 1)) as best_pct,
                   AVG(ROUND(CAST(score AS FLOAT) / total * 100, 1)) as avg_pct
            FROM attempts
            WHERE total > 0;
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("totalAttempts", rs.getInt("total_attempts"));
                stats.put("bestPct", rs.getDouble("best_pct"));
                stats.put("avgPct", rs.getDouble("avg_pct"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting stats: " + e.getMessage());
        }

        // Category breakdown
        Map<String, Integer> categoryAttempts = new HashMap<>();
        String catSql = """
            SELECT COALESCE(c.name, 'General') as cat_name, COUNT(*) as cnt
            FROM attempts a
            LEFT JOIN categories c ON a.category_id = c.id
            GROUP BY a.category_id
            ORDER BY cnt DESC;
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(catSql)) {
            while (rs.next()) {
                categoryAttempts.put(rs.getString("cat_name"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting category breakdown: " + e.getMessage());
        }
        stats.put("categoryCounts", categoryAttempts);

        return stats;
    }

    public boolean clearHistory() {
        String sql = "DELETE FROM attempts;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.err.println("Error clearing history: " + e.getMessage());
            return false;
        }
    }
}
