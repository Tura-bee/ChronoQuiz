package com.chronoquiz.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connections, schema creation, and default seed data.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chronoquiz.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Initializes tables and seeds default data if tables are newly created.
     */
    public void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // 2. categories table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL
                );
            """);

            // 3. questions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_id INTEGER,
                    type TEXT NOT NULL,
                    text TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    correct_answer TEXT NOT NULL,
                    accepted_answers TEXT,
                    FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE
                );
            """);

            // 4. options table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS options (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    question_id INTEGER,
                    option_text TEXT NOT NULL,
                    is_correct INTEGER NOT NULL,
                    FOREIGN KEY(question_id) REFERENCES questions(id) ON DELETE CASCADE
                );
            """);

            // 5. attempts table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attempts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    category_id INTEGER,
                    score INTEGER,
                    total INTEGER,
                    time_taken_sec INTEGER,
                    taken_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id),
                    FOREIGN KEY(category_id) REFERENCES categories(id)
                );
            """);

            // 6. attempt_answers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attempt_answers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    attempt_id INTEGER,
                    question_id INTEGER,
                    user_answer TEXT,
                    is_correct INTEGER,
                    FOREIGN KEY(attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
                    FOREIGN KEY(question_id) REFERENCES questions(id)
                );
            """);

            // Seed default categories and questions if database is brand new
            seedInitialData(conn);

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        // Check if categories exist
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        // Insert Default Categories
        String[] categories = {
            "Computer Science & Java",
            "General Science",
            "World History",
            "Geography"
        };

        for (String cat : categories) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO categories (name) VALUES (?);")) {
                ps.setString(1, cat);
                ps.executeUpdate();
            }
        }

        // Seed Sample Questions
        seedJavaQuestions(conn);
        seedScienceQuestions(conn);
        seedHistoryQuestions(conn);
    }

    private void seedJavaQuestions(Connection conn) throws SQLException {
        int catId = getCategoryIdByName(conn, "Computer Science & Java");

        // 1. Multiple Choice: Java compiler
        insertSeedQuestion(conn, catId, "multiple",
            "Which command compiles Java source code into bytecode?",
            "easy", "javac", null,
            new String[]{"javac", "java", "javap", "javadoc"});

        // 2. Multiple Choice: Thread execution
        insertSeedQuestion(conn, catId, "multiple",
            "Which interface must be implemented to create a task for execution by a Thread?",
            "medium", "Runnable", null,
            new String[]{"Runnable", "Threadable", "Processable", "Synchronized"});

        // 3. Short Answer: Java Platform UI
        insertSeedQuestion(conn, catId, "short",
            "What is the name of the modern Java GUI toolkit featuring Scene Graph and CSS styling?",
            "easy", "JavaFX", "JavaFX, OpenJFX, JFX",
            new String[]{});

        // 4. Multiple Choice: OOP Principle
        insertSeedQuestion(conn, catId, "multiple",
            "Which OOP principle is demonstrated when a subclass provides a specific implementation of a method declared in its superclass?",
            "medium", "Polymorphism", null,
            new String[]{"Polymorphism", "Encapsulation", "Inheritance", "Abstraction"});

        // 5. Short Answer: Java Virtual Machine
        insertSeedQuestion(conn, catId, "short",
            "What acronym represents the execution engine that loads and executes Java bytecode?",
            "easy", "JVM", "JVM, Java Virtual Machine",
            new String[]{});
    }

    private void seedScienceQuestions(Connection conn) throws SQLException {
        int catId = getCategoryIdByName(conn, "General Science");

        // Multiple choice
        insertSeedQuestion(conn, catId, "multiple",
            "What is the chemical symbol for Gold?",
            "easy", "Au", null,
            new String[]{"Au", "Ag", "Fe", "Cu"});

        insertSeedQuestion(conn, catId, "multiple",
            "Which organelle is known as the powerhouse of the eukaryotic cell?",
            "easy", "Mitochondria", null,
            new String[]{"Mitochondria", "Nucleus", "Ribosome", "Endoplasmic Reticulum"});

        // Short Answer
        insertSeedQuestion(conn, catId, "short",
            "What is the closest planet to the Sun in our Solar System?",
            "easy", "Mercury", "Mercury",
            new String[]{});
    }

    private void seedHistoryQuestions(Connection conn) throws SQLException {
        int catId = getCategoryIdByName(conn, "World History");

        insertSeedQuestion(conn, catId, "multiple",
            "In which year did World War II officially end?",
            "medium", "1945", null,
            new String[]{"1945", "1939", "1918", "1950"});

        insertSeedQuestion(conn, catId, "short",
            "What ancient wonder was located in Alexandria and guided sailors with its light?",
            "medium", "Lighthouse of Alexandria", "Lighthouse of Alexandria, Pharos of Alexandria, Pharos",
            new String[]{});
    }

    private int getCategoryIdByName(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM categories WHERE name = ?;")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 1;
    }

    private void insertSeedQuestion(Connection conn, int categoryId, String type, String text,
                                    String difficulty, String correctAnswer, String acceptedAnswers,
                                    String[] options) throws SQLException {
        String sqlQ = "INSERT INTO questions (category_id, type, text, difficulty, correct_answer, accepted_answers) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement psQ = conn.prepareStatement(sqlQ, Statement.RETURN_GENERATED_KEYS)) {
            psQ.setInt(1, categoryId);
            psQ.setString(2, type);
            psQ.setString(3, text);
            psQ.setString(4, difficulty);
            psQ.setString(5, correctAnswer);
            psQ.setString(6, acceptedAnswers);
            psQ.executeUpdate();

            try (ResultSet rs = psQ.getGeneratedKeys()) {
                if (rs.next() && "multiple".equalsIgnoreCase(type) && options != null) {
                    int questionId = rs.getInt(1);
                    String sqlOpt = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?);";
                    for (String opt : options) {
                        try (PreparedStatement psOpt = conn.prepareStatement(sqlOpt)) {
                            psOpt.setInt(1, questionId);
                            psOpt.setString(2, opt);
                            psOpt.setInt(3, opt.equalsIgnoreCase(correctAnswer) ? 1 : 0);
                            psOpt.executeUpdate();
                        }
                    }
                }
            }
        }
    }
}
