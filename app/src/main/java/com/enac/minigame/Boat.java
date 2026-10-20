package com.enac.minigame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Boat {

    private static final float BOAT_ACCELERATION = 0.08f;   // effet de l'inclinaison sur la vitesse
    private static final float WATER_DRAG = 0.98f;          // frottement de l'eau (1 = aucun, 0.9 = très freiné)
    private static final float MAX_BOAT_SPEED = 15f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius;

    private float x;
    private float y;
    private float tiltSpeed;

    public Boat(float radius) {
        this.radius = radius;
        paint.setColor(Color.rgb(255, 0, 0));
    }

    public void reset(int screenWidth, int screenHeight) {
        x = screenWidth / 2f;
        y = screenHeight * 0.75f;
        tiltSpeed = 0f;
    }

    public void update(float tilt, int screenWidth) {
        // L'inclinaison modifie la vitesse, pas directement la position
        tiltSpeed += tilt * BOAT_ACCELERATION;
        // L'eau freine le bateau un peu à chaque frame
        tiltSpeed *= WATER_DRAG;

        if (tiltSpeed > MAX_BOAT_SPEED) tiltSpeed = MAX_BOAT_SPEED;
        if (tiltSpeed < -MAX_BOAT_SPEED) tiltSpeed = -MAX_BOAT_SPEED;

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

    public void draw(Canvas canvas) {
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