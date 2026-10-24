package com.enac.minigame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

public class Rock {
    private static final float RADIUS_DP = 24f;
    private static final Random rand = new Random();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float startScene, endScene;
    private final float radius;
    private float x, y;
    private boolean hit = false;

    public Rock(float startScene, float endScene, int gap, float density) {
        paint.setColor(Color.rgb(255, 225, 0));
        this.radius = RADIUS_DP * density;
        this.startScene = startScene;
        this.endScene = endScene;
        reset(gap);
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle(x, y, radius, paint);
    }

    public void reset() { reset(0); }

    public void reset(int gap) {
        float minDisplay = startScene + radius;
        float maxDisplay = endScene - radius;
        x = minDisplay + rand.nextFloat() * (maxDisplay - minDisplay);
        y = -radius - gap; // au dessus de l'écran pour ne pas le voir apparaître
        hit = false;
        paint.setAlpha(255); // redevient opaque
    }

    public void update(float gameSpeed, float screenHeight) {
        y += gameSpeed;
        if (y - radius > screenHeight) {
            reset(); // il réapparaît en haut à une nouvelle position
        }
    }

    public boolean collidesWith(Boat boat) {
        if (hit || boat.isInvincible() ) return false;
        float deltaX = x - boat.getX();
        float deltaY = y - boat.getY();
        float minDist = radius + boat.getRadius();
        return deltaX * deltaX + deltaY * deltaY < minDist * minDist;
    }

    public void markHit() {
        hit = true;
        paint.setAlpha(150); // on applique de la transparence
    }

}
