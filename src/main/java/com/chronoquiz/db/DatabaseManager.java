package com.chronoquiz.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connections, schema creation, and default seed data.
 * Seeds curriculum topics: Numerical Methods, Advanced Programming,
 * Algorithm Analysis, and Digital Electronics.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chronoquiz.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        }
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
     * Initializes tables and seeds default data if tables are newly created or missing topics.
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
                    question_text TEXT,
                    correct_answer TEXT,
                    user_answer TEXT,
                    is_correct INTEGER,
                    FOREIGN KEY(attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
                    FOREIGN KEY(question_id) REFERENCES questions(id)
                );
            """);

            // Migration for existing databases
            try {
                stmt.execute("ALTER TABLE attempt_answers ADD COLUMN question_text TEXT;");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE attempt_answers ADD COLUMN correct_answer TEXT;");
            } catch (SQLException ignored) {}

            // 7. admins table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Seed default admin and target topics
            seedInitialData(conn);

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        // Seed default admin account if none exists (username: admin, password: admin123)
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admins;")) {
            if (!rs.next() || rs.getInt(1) == 0) {
                String defaultHash = AdminDAO.hashPassword("admin123");
                try (PreparedStatement psAdmin = conn.prepareStatement("INSERT INTO admins (username, password_hash) VALUES (?, ?);")) {
                    psAdmin.setString(1, "admin");
                    psAdmin.setString(2, defaultHash);
                    psAdmin.executeUpdate();
                }
            }
        }
        // Clean up legacy placeholder categories if present
        String[] legacyCategories = {"Computer Science & Java", "General Science", "World History", "Geography"};
        for (String legacy : legacyCategories) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE name = ?;")) {
                ps.setString(1, legacy);
                ps.executeUpdate();
            }
        }

        // Target Topics
        String[] categories = {
            "Numerical Methods",
            "Advanced Programming",
            "Algorithm Analysis",
            "Digital Electronics"
        };

        for (String cat : categories) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO categories (name) VALUES (?);")) {
                ps.setString(1, cat);
                ps.executeUpdate();
            }
        }

        // Seed questions for each topic if not already populated
        seedNumericalMethodsQuestions(conn);
        seedAdvancedProgrammingQuestions(conn);
        seedAlgorithmAnalysisQuestions(conn);
        seedDigitalElectronicsQuestions(conn);
    }

    private void seedNumericalMethodsQuestions(Connection conn) throws SQLException {
        int catId = getCategoryIdByName(conn, "Numerical Methods");

        // 1. Multiple Choice: Newton-Raphson
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which iterative numerical method uses tangent lines to approximate the roots of a differentiable function f(x) = 0?",
            "medium", "Newton-Raphson Method", null,
            new String[]{"Newton-Raphson Method", "Bisection Method", "Regula Falsi Method", "Fixed-Point Iteration"});

        // 2. Multiple Choice: Order of Convergence
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "What is the order of convergence for the Newton-Raphson method for a simple root?",
            "medium", "2 (Quadratic)", null,
            new String[]{"2 (Quadratic)", "1 (Linear)", "1.618 (Superlinear)", "3 (Cubic)"});

        // 3. Multiple Choice: Simpson's Rule
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which numerical integration technique approximates definite integrals by fitting parabolic segments over pairs of subintervals?",
            "easy", "Simpson's 1/3 Rule", null,
            new String[]{"Simpson's 1/3 Rule", "Trapezoidal Rule", "Euler's Method", "Midpoint Rule"});

        // 4. Multiple Choice: RK4
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which 4th-order step-by-step method is widely regarded as the workhorse algorithm for numerically solving Ordinary Differential Equations (ODEs)?",
            "hard", "Runge-Kutta 4th Order (RK4)", null,
            new String[]{"Runge-Kutta 4th Order (RK4)", "Euler's Forward Method", "Gauss-Seidel Method", "Secant Method"});

        // 5. Short Answer: Bisection Method
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "Which bracketing method for finding roots repeatedly halves an interval [a, b] where f(a) and f(b) have opposite signs?",
            "easy", "Bisection Method", "Bisection Method, Bisection, Binary chopping, Bolzano method",
            new String[]{});

        // 6. Short Answer: Gauss-Seidel
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "In numerical linear algebra, what iterative method improves upon the Jacobi method by immediately using newly computed values within the current iteration?",
            "medium", "Gauss-Seidel Method", "Gauss-Seidel Method, Gauss-Seidel, Gauss Seidel",
            new String[]{});
    }

    private void seedAdvancedProgrammingQuestions(Connection conn) throws SQLException {
        int catId = getCategoryIdByName(conn, "Advanced Programming");

        // 1. Multiple Choice: Deadlock
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "In concurrent programming, what condition occurs when two or more threads are permanently blocked waiting for locks held by each other?",
            "easy", "Deadlock", null,
            new String[]{"Deadlock", "Livelock", "Race Condition", "Thread Starvation"});

        // 2. Multiple Choice: Volatile keyword
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which Java keyword establishes a happens-before relationship and guarantees immediate visibility of variable changes across CPU threads without synchronized locks?",
            "medium", "volatile", null,
            new String[]{"volatile", "transient", "synchronized", "atomic"});

        // 3. Multiple Choice: Singleton Pattern
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which Creational Design Pattern ensures that a class has only one instance and provides a global access point to that instance?",
            "easy", "Singleton Pattern", null,
            new String[]{"Singleton Pattern", "Factory Method", "Prototype Pattern", "Observer Pattern"});

        // 4. Multiple Choice: ScheduledExecutorService
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "In Java Concurrency, which executor class is specifically designed to run tasks periodically or after a specified delay?",
            "medium", "ScheduledExecutorService", null,
            new String[]{"ScheduledExecutorService", "ForkJoinPool", "ThreadPoolExecutor", "FixedThreadPool"});

        // 5. Short Answer: Callable Interface
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "Which functional interface in java.util.concurrent represents a task that can return a value and throw a checked exception?",
            "easy", "Callable", "Callable, java.util.concurrent.Callable",
            new String[]{});

        // 6. Short Answer: MVC Pattern
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "What architectural design pattern separates an interactive application into three components: data representation, visual user interface, and user input handling?",
            "easy", "MVC", "MVC, Model View Controller, Model-View-Controller",
            new String[]{});
    }

    private void seedAlgorithmAnalysisQuestions(Connection conn) throws SQLException {
        int catId = getCategoryIdByName(conn, "Algorithm Analysis");

        // 1. Multiple Choice: QuickSort worst case
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "What is the worst-case time complexity of QuickSort when the pivot consistently partitions the array into sizes 0 and n - 1?",
            "easy", "O(n^2)", null,
            new String[]{"O(n^2)", "O(n log n)", "O(n)", "O(log n)"});

        // 2. Multiple Choice: Dynamic Programming
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which algorithmic design paradigm solves subproblems only once and stores their solutions in a table or array to avoid redundant computation?",
            "easy", "Dynamic Programming", null,
            new String[]{"Dynamic Programming", "Greedy Approach", "Divide and Conquer", "Branch and Bound"});

        // 3. Multiple Choice: Big-Theta
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which asymptotic notation formally denotes both an asymptotic upper and lower bound (i.e. tight bound) for an algorithm's running time?",
            "medium", "Big-Theta (Θ)", null,
            new String[]{"Big-Theta (Θ)", "Big-O (O)", "Big-Omega (Ω)", "Little-o (o)"});

        // 4. Multiple Choice: Dijkstra's Complexity
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "What is the tightest worst-case time complexity of Dijkstra's algorithm for finding single-source shortest paths using a Min-Heap / Priority Queue?",
            "hard", "O((V + E) log V)", null,
            new String[]{"O((V + E) log V)", "O(V^2)", "O(V * E)", "O(V^3)"});

        // 5. Short Answer: Master Theorem
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "Which fundamental theorem provides an asymptotic cookbook solution for divide-and-conquer recurrences of the form T(n) = a*T(n/b) + f(n)?",
            "medium", "Master Theorem", "Master Theorem, The Master Theorem, Master Method",
            new String[]{});

        // 6. Short Answer: Balanced BST search
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "What is the average-case time complexity for searching, inserting, and deleting in a balanced Binary Search Tree (such as an AVL or Red-Black tree)?",
            "easy", "O(log n)", "O(log n), O(logn), log n, logarithmic, O(log(n))",
            new String[]{});
    }

    private void seedDigitalElectronicsQuestions(Connection conn) throws SQLException {
        int catId = getCategoryIdByName(conn, "Digital Electronics");

        // 1. Multiple Choice: XOR Gate
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which logic gate produces an output of HIGH (1) if and only if an odd number of its inputs are HIGH (or when two inputs are different)?",
            "easy", "XOR Gate", null,
            new String[]{"XOR Gate", "XNOR Gate", "NAND Gate", "OR Gate"});

        // 2. Multiple Choice: JK Flip-Flop
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "Which flip-flop avoids the undefined/forbidden input condition (S=1, R=1) by toggling its output state when both inputs are HIGH?",
            "medium", "JK Flip-Flop", null,
            new String[]{"JK Flip-Flop", "SR Flip-Flop", "D Flip-Flop", "T Flip-Flop"});

        // 3. Multiple Choice: Karnaugh Map
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "What graphical method utilizes Gray code ordering to simplify boolean algebraic expressions up to 4 to 6 variables?",
            "easy", "Karnaugh Map (K-map)", null,
            new String[]{"Karnaugh Map (K-map)", "Venn Diagram", "State Diagram", "Bode Plot"});

        // 4. Multiple Choice: Multiplexer Select Lines
        insertSeedQuestionIfNotExists(conn, catId, "multiple",
            "How many select/address lines are required for a 16-to-1 Multiplexer (MUX)?",
            "easy", "4", null,
            new String[]{"4", "2", "8", "16"});

        // 5. Short Answer: Universal Gates
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "Which two logic gates are known as 'Universal Gates' because any other logic gate or circuit can be implemented using only them?",
            "easy", "NAND and NOR", "NAND and NOR, NAND, NOR, NOR and NAND, NAND / NOR",
            new String[]{});

        // 6. Short Answer: Binary representation
        insertSeedQuestionIfNotExists(conn, catId, "short",
            "What is the binary representation of the decimal number 13?",
            "easy", "1101", "1101, 00001101, 0b1101",
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

    private void insertSeedQuestionIfNotExists(Connection conn, int categoryId, String type, String text,
                                               String difficulty, String correctAnswer, String acceptedAnswers,
                                               String[] options) throws SQLException {
        // Check if question text already exists to avoid duplicates
        try (PreparedStatement psCheck = conn.prepareStatement("SELECT id FROM questions WHERE text = ?;")) {
            psCheck.setString(1, text);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    return; // Already exists
                }
            }
        }

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
