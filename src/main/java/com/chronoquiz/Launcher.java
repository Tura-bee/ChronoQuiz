package com.chronoquiz;

import javafx.application.Application;

/**
 * Main launcher entry point.
 * Calling Application.launch from a class that doesn't extend Application
 * avoids JavaFX runtime components missing errors when launched directly.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(ChronoQuizApp.class, args);
    }
}
