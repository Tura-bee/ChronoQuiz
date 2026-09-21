package com.chronoquiz.db;

import com.chronoquiz.model.Category;
import com.chronoquiz.model.MultipleChoiceQuestion;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.ShortAnswerQuestion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Question, Option, and Category entities.
 */
public class QuestionDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY name ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
        return list;
    }

    public int getOrCreateCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "General Knowledge";
        }
        categoryName = categoryName.trim();

        String selectSql = "SELECT id FROM categories WHERE name = ? COLLATE NOCASE;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, categoryName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding category: " + e.getMessage());
        }

        String insertSql = "INSERT INTO categories (name) VALUES (?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoryName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating category: " + e.getMessage());
        }
        return 1;
    }

    public boolean questionExistsByText(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String sql = "SELECT id FROM questions WHERE text = ? COLLATE NOCASE LIMIT 1;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, text.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking question existence: " + e.getMessage());
            return false;
        }
    }

    public List<Question> getAllQuestions() {
        List<Question> questions = new ArrayList<>();
        String sql = """
            SELECT q.id, q.category_id, c.name as category_name, q.type, q.text,
                   q.difficulty, q.correct_answer, q.accepted_answers
            FROM questions q
            JOIN categories c ON q.category_id = c.id
            ORDER BY q.id DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                questions.add(mapResultSetToQuestion(conn, rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all questions: " + e.getMessage());
        }
        return questions;
    }

    public List<Question> getQuestionsForQuiz(int categoryId, String difficulty, int count) {
        List<Question> questions = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT q.id, q.category_id, c.name as category_name, q.type, q.text,
                   q.difficulty, q.correct_answer, q.accepted_answers
            FROM questions q
            JOIN categories c ON q.category_id = c.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (categoryId > 0) {
            sql.append(" AND q.category_id = ?");
            params.add(categoryId);
        }
        if (difficulty != null && !difficulty.equalsIgnoreCase("any") && !difficulty.trim().isEmpty()) {
            sql.append(" AND q.difficulty = ? COLLATE NOCASE");
            params.add(difficulty.toLowerCase().trim());
        }

        sql.append(" ORDER BY RANDOM() LIMIT ?;");
        params.add(count);

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = mapResultSetToQuestion(conn, rs);
                    if (q instanceof MultipleChoiceQuestion mcq) {
                        mcq.shuffleOptions();
                    }
                    questions.add(q);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading quiz questions: " + e.getMessage());
        }
        return questions;
    }

    public boolean addQuestion(Question question) {
        String sql = """
            INSERT INTO questions (category_id, type, text, difficulty, correct_answer, accepted_answers)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, question.getCategoryId());
                ps.setString(2, question.getType());
                ps.setString(3, question.getQuestionText());
                ps.setString(4, question.getDifficulty());
                ps.setString(5, question.getCorrectAnswerDisplay());
                ps.setString(6, question.getAcceptedAnswersString());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int qId = rs.getInt(1);
                        question.setId(qId);

                        if ("multiple".equalsIgnoreCase(question.getType()) && question.getOptions() != null) {
                            String optSql = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?);";
                            try (PreparedStatement optPs = conn.prepareStatement(optSql)) {
                                for (String opt : question.getOptions()) {
                                    optPs.setInt(1, qId);
                                    optPs.setString(2, opt);
                                    optPs.setInt(3, question.checkAnswer(opt) ? 1 : 0);
                                    optPs.addBatch();
                                }
                                optPs.executeBatch();
                            }
                        }
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
            System.err.println("Error adding question: " + e.getMessage());
            return false;
        }
    }

    public boolean updateQuestion(Question question) {
        String sql = """
            UPDATE questions
            SET category_id = ?, type = ?, text = ?, difficulty = ?, correct_answer = ?, accepted_answers = ?
            WHERE id = ?;
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, question.getCategoryId());
                ps.setString(2, question.getType());
                ps.setString(3, question.getQuestionText());
                ps.setString(4, question.getDifficulty());
                ps.setString(5, question.getCorrectAnswerDisplay());
                ps.setString(6, question.getAcceptedAnswersString());
                ps.setInt(7, question.getId());
                ps.executeUpdate();

                // Replace options if multiple choice
                try (PreparedStatement delOpts = conn.prepareStatement("DELETE FROM options WHERE question_id = ?;")) {
                    delOpts.setInt(1, question.getId());
                    delOpts.executeUpdate();
                }

                if ("multiple".equalsIgnoreCase(question.getType()) && question.getOptions() != null) {
                    String optSql = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?);";
                    try (PreparedStatement optPs = conn.prepareStatement(optSql)) {
                        for (String opt : question.getOptions()) {
                            optPs.setInt(1, question.getId());
                            optPs.setString(2, opt);
                            optPs.setInt(3, question.checkAnswer(opt) ? 1 : 0);
                            optPs.addBatch();
                        }
                        optPs.executeBatch();
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
            System.err.println("Error updating question: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting question: " + e.getMessage());
            return false;
        }
    }

    private Question mapResultSetToQuestion(Connection conn, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int categoryId = rs.getInt("category_id");
        String categoryName = rs.getString("category_name");
        String type = rs.getString("type");
        String text = rs.getString("text");
        String difficulty = rs.getString("difficulty");
        String correctAnswer = rs.getString("correct_answer");
        String acceptedAnswers = rs.getString("accepted_answers");

        if ("short".equalsIgnoreCase(type)) {
            ShortAnswerQuestion saq = new ShortAnswerQuestion(id, categoryId, text, difficulty, correctAnswer, null);
            saq.setCategoryName(categoryName);
            if (acceptedAnswers != null && !acceptedAnswers.isEmpty()) {
                saq.setAcceptedAnswersFromString(acceptedAnswers);
            }
            return saq;
        } else {
            // Multiple Choice
            List<String> options = loadOptionsForQuestion(conn, id);
            MultipleChoiceQuestion mcq = new MultipleChoiceQuestion(id, categoryId, text, difficulty, correctAnswer, options);
            mcq.setCategoryName(categoryName);
            return mcq;
        }
    }

    private List<String> loadOptionsForQuestion(Connection conn, int questionId) {
        List<String> options = new ArrayList<>();
        String sql = "SELECT option_text FROM options WHERE question_id = ? ORDER BY id ASC;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    options.add(rs.getString("option_text"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading options: " + e.getMessage());
        }
        return options;
    }
}
