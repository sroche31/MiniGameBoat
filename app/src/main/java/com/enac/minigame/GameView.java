package com.enac.minigame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {
    private Paint paint = new Paint();
    private int score = 0;
    private float x;
    private float y;
    private int width;
    private int height;
    private boolean right = true;
    private int diff = 0;

    enum GameState {
        START,
        PLAYING,
        PAUSED,
        GAME_OVER
    }
    private GameState gameState = GameState.START;

    public GameView(Context context) {
        super(context);
        paint.setTextSize(60);
        initVal();
    }

    private void initVal() {
        right = true;
        score = 0;
        diff = 0;
        x = 200;
        y = 300;
    }
    private float radius = 40;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        width = this.getWidth();
        height = this.getHeight();

        // Game State
        switch (gameState) {
            case START:
                drawStartScreen(canvas);
                break;
            case PLAYING:
                drawGame(canvas);
                break;
//            case PAUSED:
//                drawPauseScreen(canvas);
//                break;
            case GAME_OVER:
                drawGameOver(canvas);
                break;
        }
    }

    private int calcScore() {
        return 20 + diff;
    }

    private void drawStartScreen(Canvas canvas) {
        canvas.drawText("MINI-JEU", 100, 200, paint);
        canvas.drawText("Toucher pour jouer", 100, 300, paint);
    }

    private void drawGame(Canvas canvas) {
        canvas.drawCircle(x, y, radius, paint);
//        canvas.drawLine(x, y, x + 200, y + 100, paint);
        canvas.drawText(String.valueOf(score), width / 2, height / 2, paint);
        invalidate();
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawText("GAME OVER", 100, 200, paint);
        canvas.drawText("Score : " + score, width / 2, height / 2, paint);
        invalidate();
    }

    private void update() {
        int deplacement = calcScore();

        if (right) {
            x += deplacement;
        } else {
            x -= deplacement;
        }

        score += deplacement;
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            update();
            invalidate(); // demande un nouveau dessin
            postDelayed(gameLoop, 16);

            if ((x + radius > width || x - radius < 0) && gameState == gameState.PLAYING) {
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
                        diff += 5;
                        if (right) {
                            right = false;
                        } else {
                            right = true;
                        }
//                        gameState = gameState.GAME_OVER;
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
