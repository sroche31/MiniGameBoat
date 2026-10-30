package com.enac.minigame;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AnimatedImageDrawable;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GameView extends View {

    // ---- Design values ----
    // Android Material Design recommendations (in dp, converted to px in the constructor)
    private static final float MARGIN_DP = 16f;
    private static final float TEXT_SIZE_SP = 20f;
    private static final long BOOST_DURATION_MS = 2500L;
    private static final long BOOST_COOLDOWN_MS = 6000L;
    private static final long BRAKE_DURATION_MS = 1000L;
    private static final float BRAKE_FACTOR = 0.15f;
    private static final float GAMESPEED_PROGRESSION = 0.007f;
    private static final int KRAKEN_APPEAR = 560;
    private static final int KRAKEN_GO = 780;
    private static final int KRAKEN_DEATH = 850;
    private static final int KRAKEN_BOOST_SETBACK = 470;
    private static final float KRAKEN_VISIBLE_HEIGHT_DP = 100f;
    private static final float KRAKEN_SMOOTHING = 0.1f;
    private static final float KRAKEN_BAR_WIDTH_DP = 12f;
    private static final float KRAKEN_BAR_TOP = 0.35f;
    private static final float KRAKEN_BAR_BOTTOM = 0.8f;
    private static final long FRAME_DELAY_MS = 16L;
    // Speeds are in px/frame for a screen of this height (px), then scaled to the real screen height
    private static final float REFERENCE_SCREEN_HEIGHT = 2400f;
    private static final float START_SPEED = 8f;
    private static final float MAX_SPEED = 46.5f;
    private static final float HIT_SPEED_LOSS = 0.1f;
    private static final float START_SPACING_FACTOR = 1.6f;
    private static final float MIN_X_GAP_RATIO = 0.75f;
    private static final int MAX_ROCKS_AWAY_FROM_BOAT = 4;
    private static final int START_LIVES = 2;
    private static final float SCORE_FACTOR = 0.008f;
    private static final float PAUSE_BUTTON_SIZE_DP = 36f;
    private static final float BOOST_BUTTON_SIZE_DP = 56f;
    private static final float HEART_PIXEL_DP = 3f;
    private static final String[] HEART = {
            "..BBB...BBB..",
            ".BRRRB.BRRRB.",
            "BRRRRRBRRRRRB",
            "BRRRRRRRRRRRB",
            "BRRRRRRRRRRRB",
            ".BRRRRRRRRRB.",
            "..BRRRRRRRB..",
            "...BRRRRRB...",
            "....BRRRB....",
            ".....BRB.....",
            "......B......",
    };
    private static final float TILT_HINT_THRESHOLD = 2f;
    private static final float TUTORIAL_SIZE = 0.6f;
    private static final float BOOST_PULSE_SCALE = 0.2f;
    private static final long BOOST_PULSE_PERIOD_MS = 1000L;
    private static final long BOOST_TUTORIAL_DELAY_MS = 3000L;
    private static final String HAS_PLAYED_KEY = "hasPlayed";
    private static final String BEST_SCORE_KEY = "bestScore";
    private AnimatedImageDrawable tiltTutorialGif;
    private AnimatedImageDrawable micTutorialGif;
    private final Path tutorialClip = new Path();
    private final float density;
    private final float margin;

    // ---- Global Game & Constants ----
    private GameState gameState = GameState.START;
    private GameState stateBeforePause = GameState.PLAYING;
    private int screenWidth;
    private int screenHeight;

    // ---- Paints ----
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint krakenBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint heartPaint = new Paint();
    private final Paint popupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadePaint = new Paint();
    private final Paint krakenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---- Sensors ----
    private final TiltSensorManager tiltSensor;
    private final MicrophoneSensorManager micSensor;
    private final Runnable requestMicPermission;

    // ---- Saved data ----
    private final SharedPreferences prefs;
    private boolean hasPlayed;
    private float bestScore;

    // ---- Game objects ----
    private final Boat boat;
    private final List<Rock> rocks = new ArrayList<>();


    // ---- Game variables ----
    private float gameSpeed;    // == base difficulty, goes up over time
    private static final int NB_ROCKS = 5;
    private long boostEndTime;
    private long boostChargeStart;
    private float boostSpeed;
    private long startTime;
    private float score;
    private int lives;
    private int krakenProgress;
    private DeathCause death;
    private boolean useMic = true;
    private boolean useTilt = true;
    private float fingerX;
    private boolean tiltHintDone;
    private float lastRockX;
    private int rocksAwayFromBoat;
    private long readyTime;
    private long gameTime;      // ms, only advances while PLAYING
    private long lastFrameTime;
    private float speedScale = 1f;

    // ---- Elements values ----
    private float topKrakenY;
    private final RectF windProgressBar = new RectF();
    private final RectF pauseButton = new RectF();
    private final RectF krakenProgressBar = new RectF();
    private final RectF gameZone = new RectF();
    private final RectF boostButton = new RectF();
    // krakenDisplay ToDo


    public GameView(Context context, Runnable requestMicPermission) {
        super(context);
        this.requestMicPermission = requestMicPermission;
        density = getResources().getDisplayMetrics().density;
        margin = MARGIN_DP * density;
        // Paints
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP, getResources().getDisplayMetrics()));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        krakenPaint.setColor(Color.RED);
        barPaint.setColor(Color.WHITE);
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStrokeWidth(2 * density);
        popupPaint.setColor(Color.rgb(40, 40, 40));
        shadePaint.setColor(Color.argb(160, 0, 0, 0));
        // Sensors
        tiltSensor = new TiltSensorManager(context);
        micSensor = new MicrophoneSensorManager(context);
        micSensor.setCooldownMs(0); // the boost cooldown is handled by the game (boostCharge)
        // Saved data
        prefs = context.getSharedPreferences("minigame", Context.MODE_PRIVATE);
        hasPlayed = prefs.getBoolean(HAS_PLAYED_KEY, false);
        bestScore = prefs.getFloat(BEST_SCORE_KEY, 0f);
        // Init
        boat = new Boat(density);
        initVal();
        // Load tutorials
        new Thread(() -> {
            try {
                AnimatedImageDrawable tiltGif = (AnimatedImageDrawable) ImageDecoder.decodeDrawable(
                        ImageDecoder.createSource(getResources(), R.raw.phone_tilt_tutorial));
                AnimatedImageDrawable micGif = (AnimatedImageDrawable) ImageDecoder.decodeDrawable(
                        ImageDecoder.createSource(getResources(), R.raw.mic_blow_tutorial));
                post(() -> {
                    tiltTutorialGif = tiltGif;
                    micTutorialGif = micGif;
                });
            } catch (IOException e) {
                Log.w("GameView", "Tutorial GIF loading failed", e);
            }
        }).start();
    }

    private void initVal() {
        score = 0;
        gameSpeed = START_SPEED;
        lives = START_LIVES;
        startTime = System.currentTimeMillis();
        boostEndTime = 0;
        gameTime = 0;
        boostChargeStart = -BOOST_COOLDOWN_MS; // boost ready at start
        krakenProgress = 0;
        topKrakenY = screenHeight;
        death = null;
        fingerX = gameZone.centerX();
        tiltHintDone = false;
        boat.reset(gameZone);
        initRocks();
    }

    private void initRocks() {
        rocks.clear();
        if (screenWidth == 0 || screenHeight == 0) return;
        for (int i = 0; i < NB_ROCKS; i++) {
            rocks.add(new Rock(gameZone.left, gameZone.right, (int) (i * rockSpacing(START_SPEED)), density));
        }
        rocks.get(0).centerOn(gameZone.centerX());
        lastRockX = gameZone.centerX();
        rocksAwayFromBoat = 0;
        for (int i = 1; i < NB_ROCKS; i++) {
            placeRock(rocks.get(i));
        }
    }

    private void placeRock(Rock rock) {
        float min = gameZone.left + rock.getRadius();
        float max = gameZone.right - rock.getRadius();
        float x;
        if (rocksAwayFromBoat >= MAX_ROCKS_AWAY_FROM_BOAT) {
            x = Math.max(min, Math.min(max, boat.getX()));
        } else {
            do {
                x = min + (float) Math.random() * (max - min);
            } while (Math.abs(x - lastRockX) < MIN_X_GAP_RATIO * 2 * rock.getRadius());
        }
        boolean nearBoat = Math.abs(x - boat.getX()) < rock.getRadius() + boat.getRadius();
        rocksAwayFromBoat = nearBoat ? 0 : rocksAwayFromBoat + 1;
        lastRockX = x;
        rock.centerOn(x);
    }

    // vertical distance between two rocks : x START_SPACING_FACTOR at START_SPEED, x1 at MAX_SPEED
    private float rockSpacing(float speed) {
        float ratio = Math.min(1f, Math.max(0f, (speed - START_SPEED) / (MAX_SPEED - START_SPEED)));
        return (float) screenHeight / NB_ROCKS * (1 + (START_SPACING_FACTOR - 1) * (1 - ratio));
    }

    private float topRockY() {
        float top = 0;
        for (Rock rock : rocks) {
            top = Math.min(top, rock.getY());
        }
        return top;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        speedScale = h / REFERENCE_SCREEN_HEIGHT;
        krakenProgressBar.set(margin, h * KRAKEN_BAR_TOP,
                margin + KRAKEN_BAR_WIDTH_DP * density, h * KRAKEN_BAR_BOTTOM);
        gameZone.set(krakenProgressBar.right + margin, 0, w, h);

        float right = w - getPaddingRight() - margin;
        float pauseSize = PAUSE_BUTTON_SIZE_DP * density;
        float pauseTop = getPaddingTop() + margin;
        pauseButton.set(right - pauseSize, pauseTop, right, pauseTop + pauseSize);
        windProgressBar.set(margin, pauseButton.top, pauseButton.left - margin, pauseButton.bottom);

        float boostSize = BOOST_BUTTON_SIZE_DP * density;
        float boostBottom = h - KRAKEN_VISIBLE_HEIGHT_DP * density - margin;
        boostButton.set(right - boostSize, boostBottom - boostSize, right, boostBottom);

        if (gameState == GameState.START || gameState == GameState.GAME_OVER) {
            initRocks();
        }
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            if (gameState == GameState.PLAYING) {
                gameTime += now - lastFrameTime;
                update();
            } else if (gameState == GameState.READY) {
                if (micSensor.consumeTrigger()) {
                    launch();
                }
            } else if (Math.abs(tiltSensor.getTilt()) > TILT_HINT_THRESHOLD) {
                tiltHintDone = true;
                gameState = GameState.PLAYING;
            }
            lastFrameTime = now;
            invalidate();
            if (gameState == GameState.PLAYING || gameState == GameState.READY
                    || gameState == GameState.TILT_HINT) {
                postDelayed(gameLoop, FRAME_DELAY_MS);
            }
        }
    };

    private void update() {
        Rock firstRock = rocks.get(0);
        if (Math.abs(boat.getX() - gameZone.centerX()) > boat.getRadius()) {
            tiltHintDone = true;
        }
        if (!tiltHintDone && boat.getY() - firstRock.getY() < boat.getRadius() + 3 * firstRock.getRadius()) {
            gameState = GameState.TILT_HINT;
            return;
        }

        updateGameValues();

        if (useTilt) {
            boat.update(tiltSensor.getTilt(), gameZone, speedScale);
        } else {
            boat.follow(fingerX, gameZone);
        }
        if (micSensor.consumeTrigger() && boostReady()) {
            boost();
        }

        float speed = currentSpeed() * speedScale;
        for (Rock rock : rocks) {
            if (rock.update(speed, screenHeight, topRockY() - rockSpacing(gameSpeed))) {
                placeRock(rock);
            }

            if (rock.collidesWith(boat)) {
                lives--;
                rock.markHit();
                boat.hit();
                boat.bounceFrom(rock.getX());
                gameSpeed = Math.max(START_SPEED, gameSpeed * (1 - HIT_SPEED_LOSS));
                if (lives <= 0) {
                    death = DeathCause.ROCKS;
                    gameOver();
                    return;
                }
            }
        }

        float target;
        if (krakenProgress >= KRAKEN_GO) {
            target = 0f; // kraken goes on the boat when you lose
            gameSpeed = 0f;
        } else if (krakenProgress >= KRAKEN_APPEAR) {
            target = screenHeight - KRAKEN_VISIBLE_HEIGHT_DP * density;
        } else {
            target = screenHeight;
        }
        topKrakenY += (target - topKrakenY) * KRAKEN_SMOOTHING;

        if (krakenProgress >= KRAKEN_DEATH) {
            death = DeathCause.KRAKEN;
            gameOver();
        }
    }

    private void updateGameValues() {
        // up difficulty
        gameSpeed = Math.min(gameSpeed + GAMESPEED_PROGRESSION, MAX_SPEED);

        // up score
        if (krakenProgress < KRAKEN_GO) {
            score += currentSpeed() * SCORE_FACTOR;
        }

        // up kraken
        krakenProgress++;
    }

    private float currentSpeed() {
        long remaining = boostEndTime - gameTime;
        if (remaining > 0) return gameSpeed + boostSpeed;
        if (remaining > -BRAKE_DURATION_MS) {
            float t = (float) -remaining / BRAKE_DURATION_MS;
            return gameSpeed + boostSpeed * (1 - t) * (1 - t)
                    - gameSpeed * BRAKE_FACTOR * (float) Math.sin(Math.PI * t);
        }
        return gameSpeed;
    }

    private void boost() {
        if (krakenProgress >= KRAKEN_GO) return;

        // ToDo : Adjust ranges and boost values
        if (gameSpeed < 15) {
            boostSpeed = 8;
        } else if (gameSpeed < 25) {
            boostSpeed = 12;
        } else {
            boostSpeed = 16;
        }

        boostEndTime = gameTime + BOOST_DURATION_MS;
        boostChargeStart = gameTime;

        // Kraken is overtaken
        krakenProgress -= KRAKEN_BOOST_SETBACK;
        if (krakenProgress < 0) { krakenProgress = 0; }
    }

    private float boostCharge() {
        return Math.min(1f, (float) (gameTime - boostChargeStart) / BOOST_COOLDOWN_MS);
    }

    private boolean boostReady() {
        return boostCharge() >= 1f;
    }

    private void launch() {
        boost();
        gameState = GameState.PLAYING;
        hasPlayed = true;
        prefs.edit().putBoolean(HAS_PLAYED_KEY, true).apply();
    }

    private boolean boostTutorialVisible() {
        return !hasPlayed || System.currentTimeMillis() - readyTime > BOOST_TUTORIAL_DELAY_MS;
    }

    public void pause() {
        if (gameState != GameState.PLAYING && gameState != GameState.READY
                && gameState != GameState.TILT_HINT) return;
        stateBeforePause = gameState;
        gameState = GameState.PAUSED;
        stopGame();
        invalidate();

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_pause, null);
        MaterialSwitch micSwitch = dialogView.findViewById(R.id.switch_mic);
        MaterialSwitch tiltSwitch = dialogView.findViewById(R.id.switch_tilt);
        micSwitch.setChecked(useMic);
        micSwitch.setEnabled(getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE));
        tiltSwitch.setChecked(useTilt);
        tiltSwitch.setEnabled(tiltSensor.isAvailable());

        micSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked && !micSensor.isAvailable()) {
                requestMicPermission.run();
            }
        });

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.pause_title)
                .setView(dialogView)
                .setPositiveButton(R.string.pause_resume, (dialog, which) -> {
                    useMic = micSwitch.isChecked();
                    useTilt = tiltSwitch.isChecked();
                    resume();
                })
                .setNegativeButton(R.string.pause_quit, (dialog, which) -> {
                    death = DeathCause.SUICIDE;
                    gameOver();
                })
                .setCancelable(false)
                .show();
    }

    private void resume() {
        gameState = stateBeforePause;
        fingerX = boat.getX();
        startGame();
    }

    private void gameOver() {
        if (score > bestScore) {
            bestScore = score;
            prefs.edit().putFloat(BEST_SCORE_KEY, bestScore).apply();
        }
        gameState = GameState.GAME_OVER;
        invalidate();
        stopGame();
    }

    public void startGame() {
        useTilt = useTilt && tiltSensor.isAvailable();
        if (useTilt) {
            tiltSensor.start();
        }
        useMic = useMic && micSensor.start();
        lastFrameTime = SystemClock.uptimeMillis(); // the pause duration is not counted in gameTime
        removeCallbacks(gameLoop); // safety
        post(gameLoop);
    }

    public void stopGame() {
        tiltSensor.stop();
        micSensor.stop();
        removeCallbacks(gameLoop);
        if (tiltTutorialGif != null) tiltTutorialGif.stop();
        if (micTutorialGif != null) micTutorialGif.stop();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopGame();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        float x = event.getX(index);
        float y = event.getY(index);
        boolean down = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN;

        if (gameState == GameState.READY) {
            if (down && pauseButton.contains(x, y)) {
                pause();
            } else if (down && !useMic && boostButton.contains(x, y)) {
                launch();
            }
            return true;
        }

        if (gameState == GameState.TILT_HINT && down) {
            if (pauseButton.contains(x, y)) {
                pause();
                return true;
            }
            if (!useTilt) {
                tiltHintDone = true;
                gameState = GameState.PLAYING;
            }
        }

        if (gameState == GameState.PLAYING) {
            if (pauseButton.contains(x, y)) {
                if (down) {
                    pause();
                }
            } else if (!useMic && boostButton.contains(x, y)) {
                if (down && boostReady()) {
                    boost();
                }
            } else if (!useTilt) {
                fingerX = x;
            }
            return true;
        }

        if (down) {
            switch (gameState) {
                case START:
                    gameState = GameState.READY;
                    readyTime = System.currentTimeMillis();
                    initVal();
                    startGame();
                    return true;
                case GAME_OVER:
                    gameState = GameState.START;
                    invalidate();
                    return true;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // For now boost is visible by changing the background color
        boolean boostActive = gameTime < boostEndTime;
        canvas.drawColor(boostActive ? Color.rgb(60, 60, 60) : Color.BLACK);

        // Game State
        switch (gameState) {
            case START:
                drawStartScreen(canvas);
                break;
            case READY:
                // Waiting for user to blow wind
                drawGame(canvas);
                if (!boostTutorialVisible()) break;
                canvas.drawRect(0, 0, screenWidth, screenHeight, shadePaint);
                if (useMic) {
                    drawGifTutorial(canvas, micTutorialGif);
                } else {
                    drawTextTutorial(canvas, "Cliquer sur le bouton de boost", "pour avancer");
                    drawBoostButton(canvas, boostPulse());
                }
                drawPauseButton(canvas);
                break;
            case PLAYING:
            case PAUSED:
                drawGame(canvas);
                break;
            case TILT_HINT:
                // Display if the player didn't already moved
                drawGame(canvas);
                canvas.drawRect(0, 0, screenWidth, screenHeight, shadePaint);
                if (useTilt) {
                    drawGifTutorial(canvas, tiltTutorialGif);
                } else {
                    drawTextTutorial(canvas, "Glisser le doigt pour diriger");
                }
                drawPauseButton(canvas);
                break;
            case SETTINGS:
                // ToDo
                break;
            case HELP:
                // ToDo
                break;
            case GAME_OVER:
                drawGameOver(canvas);
                break;
        }
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
        drawKrakenBar(canvas);
        drawWindBar(canvas);
        // Draw score
        canvas.drawText(formatScore(score), screenWidth / 2f, screenHeight / 2f, textPaint);
        drawPauseButton(canvas);
        drawBoostButton(canvas, 1f);
        drawHearts(canvas);
        // Kraken rect
        canvas.drawRect(0f, topKrakenY, screenWidth, screenHeight, krakenPaint);
    }

    private void drawGifTutorial(Canvas canvas, AnimatedImageDrawable gif) {
        if (gif == null) return;
        if (!gif.isRunning()) gif.start();
        int size = (int) (screenWidth * TUTORIAL_SIZE);
        int left = (screenWidth - size) / 2;
        int top = (screenHeight - size) / 2;
        gif.setBounds(left, top, left + size, top + size);
        tutorialClip.reset();
        tutorialClip.addRoundRect(left, top, left + size, top + size, margin, margin, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(tutorialClip);
        gif.draw(canvas);
        canvas.restore();
        canvas.drawRoundRect(left, top, left + size, top + size, margin, margin, barPaint);
    }

    private void drawTextTutorial(Canvas canvas, String... lines) {
        float lineHeight = textPaint.descent() - textPaint.ascent();
        float textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, textPaint.measureText(line));
        }
        float halfWidth = textWidth / 2 + margin;
        float halfHeight = lines.length * lineHeight / 2 + margin;
        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        canvas.drawRoundRect(cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight,
                margin, margin, popupPaint);
        canvas.drawRoundRect(cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight,
                margin, margin, barPaint);
        float y = cy - lines.length * lineHeight / 2 - textPaint.ascent();
        for (String line : lines) {
            canvas.drawText(line, cx, y, textPaint);
            y += lineHeight;
        }
    }

    private void drawKrakenBar(Canvas canvas) {
        RectF bar = krakenProgressBar;
        float ratio = Math.max(0f, Math.min(1f, (float) krakenProgress / KRAKEN_GO));

        krakenBarPaint.setColor(krakenProgress >= KRAKEN_APPEAR ? Color.RED : Color.rgb(255, 140, 0));
        canvas.drawRect(bar.left, bar.bottom - ratio * bar.height(), bar.right, bar.bottom, krakenBarPaint);
        canvas.drawRect(bar, barPaint);

        float markerY = bar.bottom - (float) KRAKEN_APPEAR / KRAKEN_GO * bar.height();
        canvas.drawLine(bar.left - margin / 2, markerY, bar.right + margin / 2, markerY, barPaint);
    }

    private void drawWindBar(Canvas canvas) {
        RectF bar = windProgressBar;
        windBarPaint.setColor(boostReady() ? Color.rgb(0, 170, 0) : Color.rgb(140, 200, 140));
        canvas.drawRect(bar.left, bar.top, bar.left + boostCharge() * bar.width(), bar.bottom, windBarPaint);
        canvas.drawRect(bar, barPaint);
        float textY = bar.centerY() - (textPaint.ascent() + textPaint.descent()) / 2;
        canvas.drawText("BOOST", bar.centerX(), textY, textPaint);
    }

    private void drawPauseButton(Canvas canvas) {
        float barWidth = pauseButton.width() / 3;
        canvas.drawRect(pauseButton.left, pauseButton.top,
                pauseButton.left + barWidth, pauseButton.bottom, textPaint);
        canvas.drawRect(pauseButton.right - barWidth, pauseButton.top,
                pauseButton.right, pauseButton.bottom, textPaint);
    }

    private void drawBoostButton(Canvas canvas, float scale) {
        if (!useMic) {
            canvas.drawCircle(boostButton.centerX(), boostButton.centerY(), boostButton.width() / 2 * scale,
                    boostReady() ? textPaint : barPaint);
        }
    }

    private float boostPulse() {
        double phase = 2 * Math.PI * (System.currentTimeMillis() % BOOST_PULSE_PERIOD_MS) / BOOST_PULSE_PERIOD_MS;
        return 1f + BOOST_PULSE_SCALE * (1 - (float) Math.cos(phase)) / 2;
    }

    private void drawHearts(Canvas canvas) {
        float pixel = HEART_PIXEL_DP * density;
        float top = krakenProgressBar.top - margin - HEART.length * pixel;
        for (int i = 0; i < START_LIVES; i++) {
            drawHeart(canvas, margin + i * (HEART[0].length() + 2) * pixel, top, i < lives);
        }
    }

    private void drawHeart(Canvas canvas, float left, float top, boolean full) {
        float pixel = HEART_PIXEL_DP * density;
        for (int row = 0; row < HEART.length; row++) {
            for (int col = 0; col < HEART[row].length(); col++) {
                char c = HEART[row].charAt(col);
                if (c == 'B' || (c == 'R' && full)) {
                    heartPaint.setColor(c == 'B' ? Color.BLACK : Color.RED);
                    float x = left + col * pixel;
                    float y = top + row * pixel;
                    canvas.drawRect(x, y, x + pixel, y + pixel, heartPaint);
                }
            }
        }
    }

    private void drawGameOver(Canvas canvas) {
        String stringDeath = "Naufrage causé par ";
        if (death != null) {
            switch (death) {
                case ROCKS:
                    stringDeath += "les rochers";
                    break;
                case KRAKEN:
                    stringDeath += "le Kraken";
                    break;
                case SUICIDE:
                    stringDeath += "vous-même";
                    break;
            }
        } else { stringDeath += "un naufrage"; }

        canvas.drawText("GAME OVER", screenWidth / 2f, getPaddingTop() + 200, textPaint);
        canvas.drawText(stringDeath, screenWidth / 2f, screenHeight / 4f, textPaint);
        canvas.drawText("Score : " + formatScore(score), screenWidth / 2f, screenHeight / 2f, textPaint);
        canvas.drawText("Meilleur score : " + formatScore(bestScore), screenWidth / 2f,
                screenHeight / 2f + 2 * textPaint.getTextSize(), textPaint);
        canvas.drawText("Toucher l'écran pour recommencer", screenWidth / 2f, screenHeight * 3 / 4f, textPaint);
    }

    private String formatScore(float value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }
}