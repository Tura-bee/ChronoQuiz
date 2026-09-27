package com.chronoquiz;

import com.chronoquiz.db.DatabaseManager;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.Category;
import com.chronoquiz.model.Question;

import java.util.List;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("Testing Database initialization...");
        DatabaseManager.getInstance().initializeDatabase();

        QuestionDAO dao = new QuestionDAO();
        List<Category> categories = dao.getAllCategories();
        System.out.println("Loaded " + categories.size() + " categories:");
        for (Category c : categories) {
            System.out.println(" - " + c.getName());
        }

        List<Question> questions = dao.getAllQuestions();
        System.out.println("Loaded " + questions.size() + " questions from SQLite question bank:");
        for (Question q : questions) {
            System.out.println(" - [" + q.getType() + "] " + q.getQuestionText() + " -> " + q.getCorrectAnswerDisplay());
        }

        System.out.println("Testing Admin authentication...");
        com.chronoquiz.db.AdminDAO adminDAO = new com.chronoquiz.db.AdminDAO();
        boolean validAuth = adminDAO.authenticate("admin", "admin123");
        boolean invalidAuth = adminDAO.authenticate("admin", "wrongPass");
        System.out.println("Admin auth with correct password: " + validAuth);
        System.out.println("Admin auth with wrong password: " + invalidAuth);
        if (!validAuth || invalidAuth) {
            throw new RuntimeException("AdminDAO verification failed!");
        }

        System.out.println("Testing Attempt saving...");
        com.chronoquiz.db.AttemptDAO attemptDAO = new com.chronoquiz.db.AttemptDAO();
        com.chronoquiz.model.Player p = new com.chronoquiz.db.UserDAO().getOrCreateUser("TestPlayer");
        com.chronoquiz.model.Question testQ = questions.get(0);
        testQ.setUserAnswer("TestAnswer");
        com.chronoquiz.model.Quiz quiz = new com.chronoquiz.model.Quiz(p, 0, "All Categories", "easy", 60, java.util.List.of(testQ));
        com.chronoquiz.model.Attempt attempt = quiz.toAttempt();
        boolean saved = attemptDAO.saveAttempt(attempt);
        System.out.println("Attempt save result: " + saved);

        List<com.chronoquiz.model.Attempt> history = attemptDAO.getAllAttempts(null);
        System.out.println("Loaded " + history.size() + " attempts from History table:");
        for (com.chronoquiz.model.Attempt a : history) {
            System.out.println(" - Attempt #" + a.getId() + " by " + a.getUserName() + " (" + a.getCategoryName() + "): " + a.getScore() + "/" + a.getTotal());
        }

        if (!history.isEmpty()) {
            List<com.chronoquiz.model.AttemptAnswer> answers = attemptDAO.getAnswersForAttempt(history.get(0).getId());
            System.out.println("Loaded " + answers.size() + " answers for attempt #" + history.get(0).getId());
        }

        // Clean up test attempts so database is clean for actual user quizzes
        attemptDAO.clearHistory();

        System.out.println("Database and Admin verification completed successfully!");
        System.exit(0);
    }
}
