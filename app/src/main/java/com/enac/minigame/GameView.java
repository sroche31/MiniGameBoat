package com.enac.minigame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    // ---- Design values ----
    // Android Material Design recommendations
    private final int margin = 16;

    // ---- Global Game & Constants ----
    private GameState gameState = GameState.START;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private int screenWidth;
    private int screenHeight;

    // ---- Sensors ----
    private final TiltSensorManager tiltSensor;

    // ---- Game objects ----
    private final Boat boat;

    // ---- Game variables ----
    private float score;
    private int lives;
    private float boatSpeed;
    private boolean boatHasMoved;
    private float boatMoveDist;
    private float windCharge;
    private float krakenProgress;

    // ---- Entry ----
    private float boatTilt;

    // ---- Loop ----
    public boolean isRunning;

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
        bitmapPaint.setTextSize(60);
        bitmapPaint.setColor(Color.rgb(255, 255, 255));
        tiltSensor = new TiltSensorManager(context);
        boat = new Boat(40);
        initVal();
    }

    private void initVal() {
        // ToDo
        score = 0;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        boat.reset(screenWidth, screenHeight);
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
        // ToDo
    }

    private void drawStartScreen(Canvas canvas) {
        canvas.drawText("MINI-JEU", 100, 200, bitmapPaint);
        canvas.drawText("Toucher pour jouer", 100, 300, bitmapPaint);
        invalidate();
    }

    private void drawGame(Canvas canvas) {
        // Draw boat simplified
        boat.draw(canvas);
        // Drow score
        canvas.drawText(String.valueOf(score), screenWidth / 2, screenHeight / 2, bitmapPaint);
        invalidate();
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawText("GAME OVER", 100, 200, bitmapPaint);
        canvas.drawText("Score : " + score, screenWidth / 2, screenHeight / 2, bitmapPaint);
        invalidate();
    }

    private void update() {
        boat.update(tiltSensor.getTilt(), screenWidth);
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            update();
            invalidate();
            postDelayed(gameLoop, 16);
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
                    removeCallbacks(gameLoop);
                    gameState = gameState.GAME_OVER;
                    return true;
                case GAME_OVER:
                    gameState = gameState.START;
                    return true;
            }
        }
        return true;
    }

    public void startGame() {
        tiltSensor.start();
        post(gameLoop);
    }
    public void stopGame() {
        tiltSensor.stop();
        removeCallbacks(gameLoop);
    }
}