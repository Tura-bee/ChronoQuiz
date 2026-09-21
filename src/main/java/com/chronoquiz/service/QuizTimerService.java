package com.chronoquiz.service;

import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service managing background countdown timer for quizzes using ScheduledExecutorService.
 * Ensures UI updates are dispatched safely onto the JavaFX Application Thread.
 */
public class QuizTimerService {
    public interface TimerCallback {
        void onTick(int remainingSeconds, double progressFraction);
        void onTimeExpired();
    }

    private final int totalSeconds;
    private final AtomicInteger remainingSeconds;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerHandle;
    private TimerCallback callback;

    public QuizTimerService(int totalSeconds, TimerCallback callback) {
        this.totalSeconds = Math.max(1, totalSeconds);
        this.remainingSeconds = new AtomicInteger(this.totalSeconds);
        this.callback = callback;
    }

    public synchronized void start() {
        if (isRunning.get()) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ChronoQuiz-Timer-Thread");
            t.setDaemon(true); // Don't block JVM shutdown
            return t;
        });

        isRunning.set(true);
        isPaused.set(false);

        timerHandle = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    private void tick() {
        if (isPaused.get() || !isRunning.get()) {
            return;
        }

        int current = remainingSeconds.decrementAndGet();
        double progress = (double) current / totalSeconds;

        if (current <= 0) {
            stop();
            if (callback != null) {
                Platform.runLater(callback::onTimeExpired);
            }
        } else {
            if (callback != null) {
                Platform.runLater(() -> callback.onTick(current, Math.max(0.0, Math.min(1.0, progress))));
            }
        }
    }

    public synchronized void pause() {
        isPaused.set(true);
    }

    public synchronized void resume() {
        isPaused.set(false);
    }

    public synchronized void stop() {
        isRunning.set(false);
        if (timerHandle != null) {
            timerHandle.cancel(false);
            timerHandle = null;
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public int getRemainingSeconds() {
        return remainingSeconds.get();
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public int getElapsedSeconds() {
        return totalSeconds - remainingSeconds.get();
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
