package com.enac.minigame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    enum GameState {
        START,
        READY,
        PLAYING,
        TILT_HINT,
        PAUSED,
        SETTINGS,
        HELP,
        GAME_OVER
    }
    enum DeathCause {
        KRAKEN,
        ROCKS
    }

    // Android Material Design recommendations
    private final int margin = 16;
    private float radius = 40;

    private GameState gameState = GameState.START;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private int screenWidth;
    private int screenHeight;

    // ---- Game variables ----
    private float score;
    private int lives;
    private float boatX;
    private float boatY;
    private float boatSpeed;
    private boolean boatHasMoved;
    private float boatMoveDist;
    private float windCharge;
    private float krakenProgress;

    // ---- Entry ----
    private float boatTilt;

    // ---- Loop ----
    public boolean isRunning;

    // ---- Elements ----
    private final RectF windProgressBar = new RectF();
    // Pause button ToDo
    // heart1 ToDo
    // heart2 ToDo
    private final RectF krakenProgressBar = new RectF();
    // Game Zone ToDo
    // boatDisplay ToDo
    // bonus button ToDo
    // krakenDisplay ToDo


    public GameView(Context context) {
        super(context);
        bitmapPaint.setTextSize(60);
        initVal();
    }

    private void initVal() {
        score = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        screenWidth = this.getWidth();
        screenHeight = this.getHeight();

        // Game State
        switch (gameState) {
            case START:
                drawStartScreen(canvas);
                break;
            case READY:
                // ToDo
            case PLAYING:
                drawGame(canvas);
                break;
            case TILT_HINT:
                // ToDo
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
        // ToDo
    }

    private void drawStartScreen(Canvas canvas) {
        canvas.drawText("MINI-JEU", 100, 200, bitmapPaint);
        canvas.drawText("Toucher pour jouer", 100, 300, bitmapPaint);
    }

    private void drawGame(Canvas canvas) {
        canvas.drawCircle(boatX, boatY, radius, bitmapPaint);
//        canvas.drawLine(x, y, x + 200, y + 100, bitmapPaint);
        canvas.drawText(String.valueOf(score), screenWidth / 2, screenHeight / 2, bitmapPaint);
        invalidate();
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawText("GAME OVER", 100, 200, bitmapPaint);
        canvas.drawText("Score : " + score, screenWidth / 2, screenHeight / 2, bitmapPaint);
        invalidate();
    }

    private void update() {
        // ToDo
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            update();
            invalidate();
            postDelayed(gameLoop, 16);

            if ((boatX + radius > screenWidth || boatX - radius < 0) && gameState == gameState.PLAYING) {
                removeCallbacks(this);
                gameState = gameState.GAME_OVER;
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                switch (gameState) {
                    case START:
                        gameState = gameState.PLAYING;
                        initVal();
                        postDelayed(gameLoop, 16); // environ 60 mises à jour/s
                        return true;
                    case PLAYING:
                        // ToDo
                        return true;
                    case GAME_OVER:
                        gameState = gameState.START;
                        return true;
                }
            }
        return true;
    }

    public void startGame() {
        post(gameLoop);
    }
    public void stopGame() {
        removeCallbacks(gameLoop);
    }
}
