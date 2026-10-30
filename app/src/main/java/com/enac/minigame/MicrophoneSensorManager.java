package com.enac.minigame;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Écoute le micro et détecte les sons nettement plus forts que le bruit ambiant.
 *
 * 1. On mesure le volume du son x ms.
 * 2. On entretient une moyenne glissante de ce volume (le "bruit de fond").
 * 3. delta = volume actuel - moyenne.
 * 4. Si delta dépasse le seuil, un déclenchement est signalé au jeu.
 */
public class MicrophoneSensorManager {

    private static final String TAG = "MicrophoneSensor";

    // ---------- Réglages audio ----------

    private static final int SAMPLE_RATE = 44100;
    // 1024 échantillons à 44100 Hz ≈ 23 ms de son par mesure
    private static final int SAMPLES_PER_READ = 1024;
    private static final float BLOCK_DURATION_SECONDS = (float) SAMPLES_PER_READ / SAMPLE_RATE;
    // Un échantillon 16 bits va de -32768 à +32767
    private static final float MAX_SAMPLE_VALUE = 32768f;
    // Volume le plus bas qu'on considère (silence total)
    private static final float MIN_DB = -90f;

    // ---------- Réglages de détection par défaut ----------

    // De combien de dB le son doit dépasser la moyenne pour déclencher
    private static final float DEFAULT_TRIGGER_THRESHOLD_DB = 25f;
    // Durée sur laquelle la moyenne "se souvient" du son
    private static final float DEFAULT_AVERAGE_WINDOW_SECONDS = 6f;
    // Délai minimum entre deux déclenchements
    private static final long DEFAULT_COOLDOWN_MS = 6000;
    // Temps d'écoute au démarrage avant d'autoriser un déclenchement (la moyenne se stabilise)
    private static final float WARMUP_SECONDS = 1f;
    // Volume minimum absolu pour déclencher (un souffle dans le micro est très fort, la TV non)
    private static final float MIN_TRIGGER_DB = -20f;
    // Nombre de mesures fortes d'affilée pour déclencher (4 x 23 ms ≈ 90 ms : ignore les bruits brefs)
    private static final int MIN_LOUD_BLOCKS = 4;
    // Pour redéclencher, le delta doit d'abord redescendre sous (seuil x ce ratio)
    private static final float REARM_RATIO = 0.5f;
    // Pendant un son fort, la moyenne évolue X fois plus lentement (pour ne pas être faussée)
    private static final float LOUD_SOUND_AVERAGE_SLOWDOWN = 10f;

    public enum MicError { NONE, NO_MICROPHONE, NO_PERMISSION, BAD_CONFIG, INIT_FAILED, START_FAILED, READ_FAILED }

    // ---------- Paramètres réglables ----------

    private volatile float triggerThresholdDb = DEFAULT_TRIGGER_THRESHOLD_DB;
    private volatile float averageWindowSeconds = DEFAULT_AVERAGE_WINDOW_SECONDS;
    private volatile long cooldownMs = DEFAULT_COOLDOWN_MS;

    // ---------- Ressources audio ----------

    private final Context context;
    private AudioRecord audioRecord;
    private Thread readThread;
    private volatile boolean running = false;
    private volatile MicError lastError = MicError.NONE;

    // ---------- État de la mesure ----------

    private volatile float currentDb = MIN_DB;
    private volatile float averageDb = MIN_DB;
    private volatile float deltaDb = 0f;

    private boolean averageInitialized = false;
    private float listeningSeconds = 0f;
    private boolean armed = true;
    private int loudBlocks = 0;
    private long lastTriggerTimeMs = 0;
    private final AtomicBoolean triggerPending = new AtomicBoolean(false);

    public MicrophoneSensorManager(Context context) {
        this.context = context.getApplicationContext();
    }

    // ---------- Getters / setters ----------

    public float getTriggerThresholdDb() { return triggerThresholdDb; }
    public void setTriggerThresholdDb(float thresholdDb) { this.triggerThresholdDb = thresholdDb; }
    public float getAverageWindowSeconds() { return averageWindowSeconds; }
    public void setAverageWindowSeconds(float seconds) { this.averageWindowSeconds = seconds; }
    public long getCooldownMs() { return cooldownMs; }
    public void setCooldownMs(long cooldownMs) { this.cooldownMs = cooldownMs; }

    /** Volume actuel en dB (0 = maximum, -90 = silence). */
    public float getCurrentDb() { return currentDb; }
    /** Moyenne du bruit ambiant en dB. */
    public float getAverageDb() { return averageDb; }
    /** Écart entre le son actuel et la moyenne (> 0 = plus fort que d'habitude). */
    public float getDeltaDb() { return deltaDb; }

    public MicError getLastError() { return lastError; }

    /**
     * À appeler dans la boucle de jeu.
     * Renvoie true une seule fois par déclenchement, puis repasse à false.
     */
    public boolean consumeTrigger() {
        return triggerPending.getAndSet(false);
    }

    public boolean isAvailable() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
                && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ---------- Démarrage / arrêt ----------

    /** @return true si le micro écoute effectivement. */
    public boolean start() {
        if (running) return true;

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            return fail(MicError.NO_MICROPHONE);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return fail(MicError.NO_PERMISSION);
        }

        int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize <= 0) {
            return fail(MicError.BAD_CONFIG);
        }

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(minBufferSize * 2, SAMPLES_PER_READ * 2));
        } catch (SecurityException | IllegalArgumentException e) {
            Log.w(TAG, "Création de l'AudioRecord impossible", e);
            return fail(MicError.INIT_FAILED);
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            releaseAudioRecord();
            return fail(MicError.INIT_FAILED);   // le plus souvent : micro occupé
        }

        audioRecord.startRecording();
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            releaseAudioRecord();
            return fail(MicError.START_FAILED);
        }

        resetMeasurements();
        lastError = MicError.NONE;
        running = true;
        readThread = new Thread(this::readLoop, "MicrophoneSensorThread");
        readThread.start();
        return true;
    }

    public void stop() {
        running = false;
        if (readThread != null) {
            try { readThread.join(200); } catch (InterruptedException ignored) { }
            readThread = null;
        }
        if (audioRecord != null) {
            audioRecord.stop();
            releaseAudioRecord();
        }
        resetMeasurements();
    }

    // ---------- Boucle de lecture ----------

    private void readLoop() {
        short[] samples = new short[SAMPLES_PER_READ];

        while (running) {
            int samplesRead = audioRecord.read(samples, 0, samples.length);

            if (samplesRead == AudioRecord.ERROR_DEAD_OBJECT
                    || samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.w(TAG, "Micro perdu pendant la lecture : " + samplesRead);
                lastError = MicError.READ_FAILED;
                running = false;
                break;
            }
            if (samplesRead <= 0) {
                continue;   // rien lu cette fois-ci
            }

            float soundDb = measureDecibels(samples, samplesRead);

            // Première mesure : la moyenne part directement de ce niveau
            if (!averageInitialized) {
                averageDb = soundDb;
                averageInitialized = true;
            }

            float delta = soundDb - averageDb;
            listeningSeconds += BLOCK_DURATION_SECONDS;

            checkForTrigger(soundDb, delta);
            updateAverage(soundDb, delta);

            currentDb = soundDb;
            deltaDb = delta;
        }
    }

    // ---------- Étapes du calcul ----------

    /** Volume du bloc en dB (0 = maximum, -90 = silence). */
    private float measureDecibels(short[] samples, int samplesRead) {
        // Moyenne quadratique (RMS) : mesure l'énergie du son, entre 0 et 1
        double sumOfSquares = 0;
        for (int i = 0; i < samplesRead; i++) {
            double normalizedSample = samples[i] / MAX_SAMPLE_VALUE;
            sumOfSquares += normalizedSample * normalizedSample;
        }
        double rmsAmplitude = Math.sqrt(sumOfSquares / samplesRead);

        // Conversion en décibels (échelle logarithmique, comme l'oreille)
        float decibels = (float) (20 * Math.log10(Math.max(rmsAmplitude, 1e-9)));
        return Math.max(decibels, MIN_DB);
    }

    /** Signale un déclenchement si le son dépasse la moyenne du seuil choisi. */
    private void checkForTrigger(float soundDb, float delta) {
        boolean stillWarmingUp = listeningSeconds < WARMUP_SECONDS;
        if (stillWarmingUp) {
            return;
        }

        // Après un déclenchement, on attend que le son redescende avant de réarmer
        if (!armed) {
            if (delta < triggerThresholdDb * REARM_RATIO) {
                armed = true;
            }
            return;
        }

        long now = SystemClock.elapsedRealtime();
        boolean cooldownOver = now - lastTriggerTimeMs >= cooldownMs;

        boolean loud = delta > triggerThresholdDb && soundDb > MIN_TRIGGER_DB;
        loudBlocks = loud ? loudBlocks + 1 : 0;

        if (loudBlocks >= MIN_LOUD_BLOCKS && cooldownOver) {
            triggerPending.set(true);
            armed = false;
            lastTriggerTimeMs = now;
            loudBlocks = 0;
        }
    }

    /**
     * Moyenne glissante : à chaque mesure, la moyenne se rapproche un peu du son actuel.
     * Pendant un son fort (cri, souffle...), elle évolue beaucoup plus lentement
     * pour que l'action du joueur ne soit pas comptée comme du bruit de fond.
     */
    private void updateAverage(float soundDb, float delta) {
        float weight = BLOCK_DURATION_SECONDS / averageWindowSeconds;

        boolean isLoudSound = delta > triggerThresholdDb * REARM_RATIO;
        if (isLoudSound && listeningSeconds >= WARMUP_SECONDS) {
            weight /= LOUD_SOUND_AVERAGE_SLOWDOWN;
        }

        averageDb += (soundDb - averageDb) * weight;
    }

    // ---------- Utilitaires ----------

    private void resetMeasurements() {
        currentDb = MIN_DB;
        averageDb = MIN_DB;
        deltaDb = 0f;
        averageInitialized = false;
        listeningSeconds = 0f;
        armed = true;
        loudBlocks = 0;
        lastTriggerTimeMs = 0;
        triggerPending.set(false);
    }

    private void releaseAudioRecord() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    private boolean fail(MicError error) {
        lastError = error;
        Log.w(TAG, "Micro indisponible : " + error);
        return false;
    }
}