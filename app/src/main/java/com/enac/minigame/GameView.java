package com.enac.minigame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GameView extends View {

    // ---- Design values ----
    // Android Material Design recommendations (in dp, converted to px in the constructor)
    private static final float MARGIN_DP = 16f;
    private static final float TEXT_SIZE_SP = 20f;
    private static final long BOOST_DURATION_MS = 2500L;
    private static final float GAMESPEED_PROGRESSION = 0.012f;
    private static final int KRAKEN_APPEAR = 334;
    private static final int KRAKEN_GO = 467;
    private static final int KRAKEN_DEATH = 520;
    private static final int KRAKEN_BOOST_SETBACK = 167;
    private static final float KRAKEN_VISIBLE_HEIGHT_DP = 100f;
    private static final float KRAKEN_SMOOTHING = 0.1f;
    private static final float KRAKEN_BAR_WIDTH_DP = 12f;
    private static final float KRAKEN_BAR_TOP = 0.35f;
    private static final float KRAKEN_BAR_BOTTOM = 0.8f;
    private static final long FRAME_DELAY_MS = 16L;
    private static final float START_SPEED = 8f;
    private static final int START_LIVES = 2;
    private static final float SCORE_FACTOR = 0.008f;
    private final float density;
    private final float margin;

    // ---- Global Game & Constants ----
    private GameState gameState = GameState.START;
    private int screenWidth;
    private int screenHeight;

    // ---- Paints ----
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint krakenBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint krakenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---- Sensors ----
    private final TiltSensorManager tiltSensor;
    private final MicrophoneSensorManager micSensor;

    // ---- Game objects ----
    private final Boat boat;
    private final List<Rock> rocks = new ArrayList<>();


    // ---- Game variables ----
    private float gameSpeed;    // == base difficulty, goes up over time
    private static final int NB_ROCKS = 5;
    private long boostEndTime;
    private float boostSpeed;
    private long startTime;
    private float score;
    private int lives;
    private int krakenProgress;
    private DeathCause death;

    // ---- Elements values ----
    private float topKrakenY;
    private final RectF windProgressBar = new RectF();
    // Pause button ToDo
    // heart1 ToDo
    // heart2 ToDo
    private final RectF krakenProgressBar = new RectF();
    // Game Zone ToDo
    // bonus button ToDo
    // krakenDisplay ToDo


    public GameView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        margin = MARGIN_DP * density;
        // Paints
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP, getResources().getDisplayMetrics()));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        krakenPaint.setColor(Color.RED);
        barPaint.setColor(Color.WHITE);
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStrokeWidth(2 * density);
        // Sensors
        tiltSensor = new TiltSensorManager(context);
        micSensor = new MicrophoneSensorManager(context);
        // Init
        boat = new Boat(density);
        initVal();
    }

    private void initVal() {
        score = 0;
        gameSpeed = START_SPEED;
        lives = START_LIVES;
        startTime = System.currentTimeMillis();
        boostEndTime = 0;
        krakenProgress = 0;
        topKrakenY = screenHeight;
        death = null;
        initRocks();
        boat.reset(screenWidth, screenHeight);
    }

    private void initRocks() {
        rocks.clear();
        if (screenWidth == 0 || screenHeight == 0) return;
        int rockSpacing = screenHeight / NB_ROCKS;          // vertical distance between two rocks
        for (int i = 0; i < NB_ROCKS; i++) {
            rocks.add(new Rock(margin, screenWidth - margin, i * rockSpacing, density));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        krakenProgressBar.set(margin, h * KRAKEN_BAR_TOP,
                margin + KRAKEN_BAR_WIDTH_DP * density, h * KRAKEN_BAR_BOTTOM);
        initRocks();
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            update();
            invalidate();
            if (gameState == GameState.PLAYING) {
                postDelayed(gameLoop, FRAME_DELAY_MS);
            }
        }
    };

    private void update() {
        updateGameValues();

        boat.update(tiltSensor.getTilt(), screenWidth);
        if (micSensor.consumeTrigger()) {
            boost();
        }

        for (Rock rock : rocks) {
            rock.update(currentSpeed(), screenHeight);

            if (rock.collidesWith(boat)) {
                lives--;
                rock.markHit();
                boat.hit();
                if (lives <= 0) {
                    death = DeathCause.ROCKS;
                    gameOver();
                    return;
                }
            }
        }

        float target;
        if (krakenProgress >= KRAKEN_GO) {
            target = 0f; // kraken goes on the boat when you lose
            gameSpeed = 0f;
        } else if (krakenProgress >= KRAKEN_APPEAR) {
            target = screenHeight - KRAKEN_VISIBLE_HEIGHT_DP * density;
        } else {
            target = screenHeight;
        }
        topKrakenY += (target - topKrakenY) * KRAKEN_SMOOTHING;

        if (krakenProgress >= KRAKEN_DEATH) {
            death = DeathCause.KRAKEN;
            gameOver();
        }
    }

    private void updateGameValues() {
        // up difficulty
        gameSpeed += GAMESPEED_PROGRESSION;

        // up score
        if (krakenProgress < KRAKEN_GO) {
            score += currentSpeed() * SCORE_FACTOR;
        }

        // up kraken
        krakenProgress++;
    }

    private float currentSpeed() {
        long remaining = boostEndTime - System.currentTimeMillis();
        if (remaining <= 0) return gameSpeed;
        return gameSpeed + boostSpeed * remaining / BOOST_DURATION_MS;
    }

    private void boost() {
        // ToDo : Adjust ranges and boost values
        if (gameSpeed < 15) {
            boostSpeed = 8;
        } else if (gameSpeed < 25) {
            boostSpeed = 12;
        } else {
            boostSpeed = 16;
        }

        boostEndTime = System.currentTimeMillis() + BOOST_DURATION_MS;

        // Kraken is overtaken
        krakenProgress -= KRAKEN_BOOST_SETBACK;
        if (krakenProgress < 0) { krakenProgress = 0; }
    }

    private void gameOver() {
        gameState = GameState.GAME_OVER;
        invalidate();
        stopGame();
    }

    public void startGame() {
        tiltSensor.start();
        boolean micStarted = micSensor.start();
        System.out.println("____________________ MICRO " + micStarted);
        removeCallbacks(gameLoop); // safety
        post(gameLoop);
    }

    public void stopGame() {
        tiltSensor.stop();
        micSensor.stop();
        removeCallbacks(gameLoop);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (gameState) {
                case START:
                    gameState = GameState.PLAYING;
                    initVal();
                    startGame();
                    return true;
                case PLAYING:
                    // ToDo
                    death = DeathCause.SUICIDE;
                    gameOver();
                    return true;
                case GAME_OVER:
                    gameState = GameState.START;
                    invalidate();
                    return true;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // For now boost is visible by changing the background color
        boolean boostActive = System.currentTimeMillis() < boostEndTime;
        canvas.drawColor(boostActive ? Color.rgb(60, 60, 60) : Color.BLACK);

        // Game State
        switch (gameState) {
            case START:
                drawStartScreen(canvas);
                break;
            case READY:
                // ToDo
                // Waiting for user to blow wind
            case PLAYING:
                drawGame(canvas);
                break;
            case TILT_HINT:
                // ToDo
                // Display if the player didn't already moved
                break;
            case PAUSED:
                // ToDo
                break;
            case SETTINGS:
                // ToDo
                break;
            case HELP:
                // ToDo
                break;
            case GAME_OVER:
                drawGameOver(canvas);
                break;
        }
    }

    private void drawStartScreen(Canvas canvas) {
        canvas.drawText("MINI-JEU", screenWidth / 2f, getPaddingTop() + 200, textPaint);
        canvas.drawText("Toucher pour jouer", screenWidth / 2f, getPaddingTop() + 300, textPaint);
    }

    private void drawGame(Canvas canvas) {
        for (Rock rock : rocks) {
            rock.draw(canvas);
        }
        boat.draw(canvas);
        drawKrakenBar(canvas);
        // Draw score
        canvas.drawText(formatScore(), screenWidth / 2f, screenHeight / 2f, textPaint);
        // Kraken rect
        canvas.drawRect(0f, topKrakenY, screenWidth, screenHeight, krakenPaint);
    }

    private void drawKrakenBar(Canvas canvas) {
        RectF bar = krakenProgressBar;
        float ratio = Math.max(0f, Math.min(1f, (float) krakenProgress / KRAKEN_GO));

        krakenBarPaint.setColor(krakenProgress >= KRAKEN_APPEAR ? Color.RED : Color.rgb(255, 140, 0));
        canvas.drawRect(bar.left, bar.bottom - ratio * bar.height(), bar.right, bar.bottom, krakenBarPaint);
        canvas.drawRect(bar, barPaint);

        float markerY = bar.bottom - (float) KRAKEN_APPEAR / KRAKEN_GO * bar.height();
        canvas.drawLine(bar.left - margin / 2, markerY, bar.right + margin / 2, markerY, barPaint);
    }

    private void drawGameOver(Canvas canvas) {
        String stringDeath = "Shipwreck caused by ";
        switch (death) {
            case ROCKS:
                stringDeath += "rocks";
                break;
            case KRAKEN:
                stringDeath += "the Kraken";
                break;
            case SUICIDE:
                stringDeath += "yourself";
                break;
        }

        canvas.drawText("GAME OVER", screenWidth / 2f, getPaddingTop() + 200, textPaint);
        canvas.drawText(stringDeath, screenWidth / 2f, screenHeight / 4f, textPaint);
        canvas.drawText("Score : " + formatScore(), screenWidth / 2f, screenHeight / 2f, textPaint);
    }

    private String formatScore() {
        return String.format(Locale.getDefault(), "%.1f", score);
    }
}