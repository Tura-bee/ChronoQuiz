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

        String insertAnswerSql = """
            INSERT INTO attempt_answers (attempt_id, question_id, user_answer, is_correct)
            VALUES (?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psAttempt = conn.prepareStatement(insertAttemptSql, Statement.RETURN_GENERATED_KEYS)) {
                psAttempt.setInt(1, attempt.getUserId());
                psAttempt.setInt(2, attempt.getCategoryId());
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
                    try (PreparedStatement psAnswer = conn.prepareStatement(insertAnswerSql)) {
                        for (AttemptAnswer ans : attempt.getAnswers()) {
                            psAnswer.setInt(1, attemptId);
                            psAnswer.setInt(2, ans.getQuestionId());
                            psAnswer.setString(3, ans.getUserAnswer());
                            psAnswer.setInt(4, ans.isCorrect() ? 1 : 0);
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
                   COALESCE(q.text, 'Question text not available') as question_text,
                   COALESCE(q.correct_answer, '') as correct_answer
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
