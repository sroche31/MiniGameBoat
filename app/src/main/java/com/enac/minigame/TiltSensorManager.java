package com.enac.minigame;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class TiltSensorManager implements SensorEventListener {

    private static final float DEFAULT_SENSITIVITY = 3.2f;
    private static final float DEFAULT_DEAD_ZONE = 0.8f;
    private float sensitivity;
    private float deadZone;


    private final SensorManager sensorManager;
    private final Sensor accelerometer;

    private float tilt = 0f;

    public TiltSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensitivity = DEFAULT_SENSITIVITY;
        deadZone = DEFAULT_DEAD_ZONE;
    }

    public float getSensitivity() { return sensitivity; }
    public void setSensitivity(float sensitivity) { this.sensitivity = sensitivity; }
    public float getDeadZone() { return deadZone; }
    public void setDeadZone(float deadZone) { this.deadZone = deadZone; }


    // Plug le sensor
    public void start() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    // Détache le sensor
    public void stop() {
        sensorManager.unregisterListener(this);
        tilt = 0f;
    }

    /**
     * > 0 : penché à gauche -
     * < 0 : penché à droite -
     * = 0 : à plat
     */
    public float getTilt() {
        return tilt;
    }

    public boolean isAvailable() {
        return accelerometer != null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // values[0] = axe gauche/droite.
        float raw = event.values[0];

        // Avoid boat speed to jump from 0 to "deadZone * sensitivity" when moving just after the DZ
        float magnitude = Math.abs(raw) - deadZone;
        tilt = magnitude > 0 ? Math.signum(raw) * magnitude * sensitivity : 0f;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}