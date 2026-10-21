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
    // Android Material Design recommendations (en dp, convertis en px dans le constructeur)
    private static final float MARGIN_DP = 16f;
    private static final float TEXT_SIZE_SP = 20f;
    private final float density;
    private final float margin;

    // ---- Global Game & Constants ----
    private GameState gameState = GameState.START;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int screenWidth;
    private int screenHeight;

    // ---- Sensors ----
    private final TiltSensorManager tiltSensor;

    // ---- Game objects ----
    private final Boat boat;
    private final List<Rock> rocks = new ArrayList<>();


    // ---- Game variables ----
    private float gameSpeed = 8f;           // == difficulté de base, qui augmente avec le temps
    private static final int NB_ROCKS = 5;
    private int rockSpacing;          // distance verticale entre deux rochers
    private long startTime;
    private float score;
    private int lives;
    private boolean boatHasMoved;
    private float boatMoveDist;
    private float windCharge;
    private float krakenProgress;

    // ---- Elements values ----
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
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP, getResources().getDisplayMetrics()));
        textPaint.setColor(Color.rgb(255, 255, 255));
        textPaint.setTextAlign(Paint.Align.CENTER);
        tiltSensor = new TiltSensorManager(context);
        boat = new Boat(density);
        initVal();
    }

    private void initRocks() {
        rocks.clear();
        if (screenWidth == 0 || screenHeight == 0) return;
        rockSpacing = screenHeight / NB_ROCKS;
        for (int i = 0; i < NB_ROCKS; i++) {
            rocks.add(new Rock(margin, screenWidth - margin, i * rockSpacing, density));
        }
    }

    private void initVal() {
        // ToDo : To Complete
        score = 0;
        lives = 2;
        startTime = System.currentTimeMillis();
        initRocks();
        boat.reset(screenWidth, screenHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        initRocks();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

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
            case HELP :
                // ToDo
                break;
            case GAME_OVER:
                drawGameOver(canvas);
                break;
        }
    }

    private void calcScore() {
        score = (System.currentTimeMillis() - startTime) * gameSpeed / 10000;
    }

    private String formatScore() {
        return String.format(Locale.getDefault(), "%.1f", score);
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
        // Drow score
        canvas.drawText(formatScore(), screenWidth / 2f, screenHeight / 2f, textPaint);
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawText("GAME OVER", screenWidth / 2f, getPaddingTop() + 200, textPaint);
        canvas.drawText("Score : " + formatScore(), screenWidth / 2f, screenHeight / 2f, textPaint);
    }

    private void update() {
        calcScore();

        boat.update(tiltSensor.getTilt(), screenWidth);

        for (Rock rock : rocks) {
            rock.update(gameSpeed, screenHeight);

            if (rock.collidesWith(boat)) {
                lives--;
                rock.markHit();
                boat.hit();
                if (lives <= 0) {
                    gameOver();
                    return;
                }
            }
        }
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            update();
            invalidate();
            if (gameState == GameState.PLAYING) {
                postDelayed(gameLoop, 16);
            }
        }
    };

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
                    gameOver();
                    return true;
                case GAME_OVER:
                    gameState = GameState.START;
                    return true;
            }
        }
        return true;
    }
    private void gameOver() {
        gameState = GameState.GAME_OVER;
        invalidate();
        stopGame();
    }
    public void startGame() {
        tiltSensor.start();
        removeCallbacks(gameLoop); // garde-fou
        post(gameLoop);
    }
    public void stopGame() {
        tiltSensor.stop();
        removeCallbacks(gameLoop);
    }
}