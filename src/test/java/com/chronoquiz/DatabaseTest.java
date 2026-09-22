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

        System.out.println("Database and Admin verification completed successfully!");
        System.exit(0);
    }
}
