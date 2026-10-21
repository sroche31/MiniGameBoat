package com.enac.minigame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Boat {

    // ---- CONSTANTS ----
    private static final float BOAT_ACCELERATION = 0.08f;   // effet de l'inclinaison sur la vitesse
    private static final float WATER_DRAG = 0.98f;          // frottement de l'eau (1 = aucun, 0.9 = très freiné)
    private static final float MAX_BOAT_SPEED = 15f;
    private static final long BLINK_DURATION = 1500;
    private static final long BLINK_PHASE = BLINK_DURATION / 6; 
    
    private static final float RADIUS_DP = 14f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius;

    private float x;
    private float y;
    private float tiltSpeed;
    private long hitTime = -BLINK_DURATION; // moment du dernier choc

    public Boat(float density) {
        radius = RADIUS_DP * density;
        paint.setColor(Color.rgb(255, 0, 0));
    }

    public void reset(int screenWidth, int screenHeight) {
        x = screenWidth / 2f;
        y = screenHeight * 0.75f;
        tiltSpeed = 0f;
        hitTime = -BLINK_DURATION;
    }

    public void hit() {
        hitTime = System.currentTimeMillis();
    }

    public void update(float tilt, int screenWidth) {
        // L'inclinaison modifie la vitesse, pas directement la position
        tiltSpeed += tilt * BOAT_ACCELERATION;
        // L'eau freine le bateau un peu à chaque frame
        tiltSpeed *= WATER_DRAG;

        tiltSpeed = Math.max(-MAX_BOAT_SPEED, Math.min(MAX_BOAT_SPEED, tiltSpeed));

        x -= tiltSpeed;

        // Le bateau s'arrête contre le bord
        float minX = radius;
        float maxX = screenWidth - radius;
        if (x < minX) {
            x = minX;
            tiltSpeed = 0;
        } else if (x > maxX) {
            x = maxX;
            tiltSpeed = 0;
        }
    }

    public boolean isInvincible() {
        return System.currentTimeMillis() - hitTime < BLINK_DURATION;
    }

    public void draw(Canvas canvas) {
        long timeElapsed = System.currentTimeMillis() - hitTime;
        // phases paires = 150, phases impaires = 255
        boolean faded = isInvincible() && (timeElapsed / BLINK_PHASE) % 2 == 0;

        paint.setAlpha(faded ? 150 : 255);
        canvas.drawCircle(x, y, radius, paint);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getRadius() {
        return radius;
    }

    public float getTiltSpeed() {
        return tiltSpeed;
    }
}