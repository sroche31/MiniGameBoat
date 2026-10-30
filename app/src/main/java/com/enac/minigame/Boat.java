package com.enac.minigame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Boat {

    // ---- CONSTANTS ----
    private static final float WATER_DRAG = 0.98f;          // frottement de l'eau (1 = aucun, 0.9 = très freiné)
    private static final float MAX_BOAT_SPEED = 50f;
    private static final float FOLLOW_SMOOTHING = 0.2f;
    private static final float BOUNCE = 0.15f;
    private static final long BLINK_DURATION = 2000;
    private static final long BLINK_PHASE = BLINK_DURATION / 6;

    private static final float RADIUS_DP = 24f;

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

    public void reset(RectF zone) {
        x = zone.centerX();
        y = zone.height() * 0.75f;
        tiltSpeed = 0f;
        hitTime = -BLINK_DURATION;
    }

    public void hit() {
        hitTime = System.currentTimeMillis();
    }

    public void update(float tilt, RectF zone, float scale) {
        // L'inclinaison modifie la vitesse, pas directement la position
        tiltSpeed += tilt;
        // L'eau freine le bateau un peu à chaque frame
        tiltSpeed *= WATER_DRAG;

        tiltSpeed = Math.max(-MAX_BOAT_SPEED, Math.min(MAX_BOAT_SPEED, tiltSpeed));

        x -= tiltSpeed * scale; // scale : adapte la vitesse à la hauteur de l'écran

        // Le bateau rebondit contre le bord
        float minX = zone.left + radius;
        float maxX = zone.right - radius;
        if (x < minX) {
            x = minX;
            tiltSpeed = -tiltSpeed * BOUNCE;
        } else if (x > maxX) {
            x = maxX;
            tiltSpeed = -tiltSpeed * BOUNCE;
        }
    }

    public void follow(float targetX, RectF zone) {
        x += (targetX - x) * FOLLOW_SMOOTHING;
        x = Math.max(zone.left + radius, Math.min(zone.right - radius, x));
    }

    public void bounceFrom(float obstacleX) {
        // Rebondit seulement s'il allait vers l'obstacle (tiltSpeed > 0 = vers la gauche)
        if ((obstacleX < x) == (tiltSpeed > 0)) {
            tiltSpeed = -tiltSpeed * BOUNCE;
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