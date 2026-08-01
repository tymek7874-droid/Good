package com.tymek.fireworks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class FireworksView extends View {

    public enum BurstType { BURST, RING, WILLOW, PALM, CROSSETTE, STROBE, DOUBLE_RING }

    private static final float GRAVITY = 260f;      // px/s^2
    private static final float ROCKET_SPEED = 620f;  // px/s
    private static final float CROSSETTE_SPLIT_AGE_MS = 220f;

    private final ArrayList<Rocket> rockets = new ArrayList<>();
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint skyPaint = new Paint();
    private final Random random = new Random();
    private final float density;

    private int selectedColor = Color.parseColor("#FFD60A");
    private boolean rainbowMode = false;
    private BurstType selectedType = BurstType.BURST;
    private boolean randomType = false;
    private boolean autoShow = false;

    private long lastFrameNanos = 0L;
    private float autoShowTimerMs = 0f;
    private float nextAutoLaunchMs = 900f;

    private Vibrator vibrator;
    private SoundPool soundPool;
    private int soundWhoosh, soundBoom, soundCrackle;
    private boolean whooshLoaded, boomLoaded, crackleLoaded;

    public FireworksView(Context context) {
        this(context, null);
    }

    public FireworksView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setClickable(true);
        setupSound(context);
    }

    private void setupSound(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attrs)
                .build();
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status != 0) return;
            if (sampleId == soundWhoosh) whooshLoaded = true;
            if (sampleId == soundBoom) boomLoaded = true;
            if (sampleId == soundCrackle) crackleLoaded = true;
        });
        soundWhoosh = soundPool.load(context, R.raw.launch_whoosh, 1);
        soundBoom = soundPool.load(context, R.raw.explosion_boom, 1);
        soundCrackle = soundPool.load(context, R.raw.explosion_crackle, 1);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, 0, 0, h,
                    Color.parseColor("#0B1230"), Color.parseColor("#05060B"),
                    Shader.TileMode.CLAMP);
            skyPaint.setShader(gradient);
        }
    }

    public void setSelectedColor(int color) {
        this.rainbowMode = false;
        this.selectedColor = color;
    }

    public void setRainbowMode() {
        this.rainbowMode = true;
    }

    public void setSelectedType(BurstType type) {
        this.randomType = false;
        this.selectedType = type;
    }

    public void setRandomType() {
        this.randomType = true;
    }

    public void setAutoShow(boolean enabled) {
        this.autoShow = enabled;
        this.autoShowTimerMs = 0f;
    }

    public void clearAll() {
        rockets.clear();
        sparks.clear();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            launchRocket(event.getX(), event.getY());
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void launchRocket(float targetX, float targetY) {
        Rocket rocket = new Rocket();
        rocket.x = targetX;
        rocket.startY = getHeight() > 0 ? getHeight() : 1600f;
        rocket.y = rocket.startY;
        rocket.targetY = Math.max(targetY, rocket.startY * 0.08f);
        rocket.color = rainbowMode ? randomVividColor() : selectedColor;
        rocket.type = randomType ? randomType() : selectedType;
        rockets.add(rocket);
        vibrateTick();
        playSound(soundWhoosh, whooshLoaded, 0.5f);
        invalidate();
    }

    private BurstType randomType() {
        BurstType[] types = BurstType.values();
        return types[random.nextInt(types.length)];
    }

    private int randomVividColor() {
        float hue = random.nextFloat() * 360f;
        return Color.HSVToColor(new float[]{hue, 0.85f, 1f});
    }

    private void vibrateTick() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void playSound(int soundId, boolean loaded, float volume) {
        if (soundPool == null || !loaded) return;
        soundPool.play(soundId, volume, volume, 1, 0, 1f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, getWidth(), getHeight(), skyPaint);

        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.016f : Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        updateAutoShow(dt);
        updateRockets(dt, canvas);
        updateSparks(dt, canvas);

        if (!rockets.isEmpty() || !sparks.isEmpty() || autoShow) {
            postInvalidateOnAnimation();
        } else {
            lastFrameNanos = 0L;
        }
    }

    private void updateAutoShow(float dt) {
        if (!autoShow) return;
        autoShowTimerMs += dt * 1000f;
        if (autoShowTimerMs >= nextAutoLaunchMs) {
            autoShowTimerMs = 0f;
            nextAutoLaunchMs = 500f + random.nextFloat() * 900f;
            float w = getWidth() > 0 ? getWidth() : 1080f;
            float h = getHeight() > 0 ? getHeight() : 1920f;
            float x = w * (0.15f + random.nextFloat() * 0.7f);
            float y = h * (0.12f + random.nextFloat() * 0.35f);

            Rocket rocket = new Rocket();
            rocket.x = x;
            rocket.startY = h;
            rocket.y = h;
            rocket.targetY = y;
            rocket.color = randomVividColor();
            rocket.type = randomType();
            rockets.add(rocket);
            playSound(soundWhoosh, whooshLoaded, 0.35f);
        }
    }

    private void updateRockets(float dt, Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        for (int i = rockets.size() - 1; i >= 0; i--) {
            Rocket r = rockets.get(i);
            float prevY = r.y;
            r.y -= ROCKET_SPEED * dt;

            // outer glow pass
            paint.setStrokeWidth(9f * density);
            paint.setColor(withAlpha(r.color, 70));
            canvas.drawLine(r.x, prevY, r.x, r.y, paint);
            // bright core pass
            paint.setStrokeWidth(3f * density);
            paint.setColor(withAlpha(r.color, 230));
            canvas.drawLine(r.x, prevY, r.x, r.y, paint);

            if (r.y <= r.targetY) {
                explode(r.x, r.targetY, r.color, r.type);
                rockets.remove(i);
            }
        }
    }

    private void explode(float x, float y, int color, BurstType type) {
        switch (type) {
            case BURST:
                spawnBurst(x, y, color, 55, 130f, 340f, false, 1f, 0.9f, false, false);
                playSound(soundBoom, boomLoaded, 0.8f);
                break;
            case RING:
                spawnRing(x, y, color, 46, 260f, 0.75f, 1f);
                playSound(soundBoom, boomLoaded, 0.75f);
                break;
            case WILLOW:
                spawnBurst(x, y, color, 85, 90f, 240f, true, 1.6f, 0.55f, false, false);
                playSound(soundCrackle, crackleLoaded, 0.7f);
                break;
            case PALM:
                spawnBurst(x, y, color, 22, 260f, 430f, true, 1.15f, 0.7f, false, false);
                playSound(soundBoom, boomLoaded, 0.8f);
                break;
            case CROSSETTE:
                spawnBurst(x, y, color, 34, 140f, 260f, false, 0.9f, 0.65f, true, false);
                playSound(soundCrackle, crackleLoaded, 0.7f);
                break;
            case STROBE:
                spawnBurst(x, y, color, 60, 110f, 260f, false, 0.85f, 0.45f, false, true);
                playSound(soundBoom, boomLoaded, 0.75f);
                break;
            case DOUBLE_RING:
                spawnRing(x, y, color, 42, 300f, 0.7f, 1f);
                spawnRing(x, y, shiftHue(color, 40f), 30, 165f, 0.7f, 0.85f);
                playSound(soundBoom, boomLoaded, 0.85f);
                break;
        }
    }

    private int shiftHue(int color, float degrees) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + degrees) % 360f;
        return Color.HSVToColor(hsv);
    }

    private void spawnBurst(float x, float y, int color, int count, float minSpeed, float maxSpeed,
                             boolean trail, float gravityMul, float fadeRate,
                             boolean crossette, boolean flicker) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
            Spark s = new Spark();
            s.x = x; s.y = y; s.prevX = x; s.prevY = y;
            s.vx = (float) Math.cos(angle) * speed;
            s.vy = (float) Math.sin(angle) * speed;
            s.color = color;
            s.trail = trail;
            s.gravityMul = gravityMul;
            s.fadeRate = fadeRate;
            s.size = trail ? 2.4f : 3.2f;
            s.alpha = 1f;
            s.crossette = crossette;
            s.flicker = flicker;
            s.flickerPhase = random.nextFloat() * (float) (Math.PI * 2);
            sparks.add(s);
        }
    }

    private void spawnRing(float x, float y, int color, int count, float speed, float fadeRate, float sizeMul) {
        for (int i = 0; i < count; i++) {
            float angle = (float) (i * (Math.PI * 2) / count);
            Spark s = new Spark();
            s.x = x; s.y = y; s.prevX = x; s.prevY = y;
            s.vx = (float) Math.cos(angle) * speed;
            s.vy = (float) Math.sin(angle) * speed;
            s.color = color;
            s.trail = false;
            s.gravityMul = 0.6f;
            s.fadeRate = fadeRate;
            s.size = 3f * sizeMul;
            s.alpha = 1f;
            sparks.add(s);
        }
    }

    private void spawnCrossetteChildren(Spark parent) {
        float baseAngle = (float) Math.atan2(parent.vy, parent.vx);
        float baseSpeed = (float) Math.hypot(parent.vx, parent.vy) * 0.6f;
        float[] offsets = {-0.5f, 0f, 0.5f};
        for (float offset : offsets) {
            Spark child = new Spark();
            float angle = baseAngle + offset;
            child.x = parent.x; child.y = parent.y;
            child.prevX = parent.x; child.prevY = parent.y;
            child.vx = (float) Math.cos(angle) * baseSpeed;
            child.vy = (float) Math.sin(angle) * baseSpeed;
            child.color = parent.color;
            child.trail = false;
            child.gravityMul = 0.9f;
            child.fadeRate = 1.1f;
            child.size = 2.2f;
            child.alpha = 1f;
            sparks.add(child);
        }
    }

    private void updateSparks(float dt, Canvas canvas) {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.prevX = s.x;
            s.prevY = s.y;
            s.vy += GRAVITY * s.gravityMul * dt;
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.alpha -= s.fadeRate * dt;
            s.ageMs += dt * 1000f;

            if (s.crossette && !s.splitDone && s.ageMs >= CROSSETTE_SPLIT_AGE_MS) {
                s.splitDone = true;
                spawnCrossetteChildren(s);
            }

            if (s.alpha <= 0f) {
                sparks.remove(i);
                continue;
            }

            float renderAlpha = Math.max(0f, Math.min(1f, s.alpha));
            if (s.flicker) {
                float flick = 0.35f + 0.65f * Math.abs((float) Math.sin(s.ageMs / 1000f * 7f * Math.PI + s.flickerPhase));
                renderAlpha *= flick;
            }
            int alphaInt = (int) (renderAlpha * 255);

            if (s.trail) {
                paint.setStyle(Paint.Style.STROKE);
                // outer glow
                paint.setStrokeWidth(s.size * density * 2.4f);
                paint.setColor(withAlpha(s.color, (int) (alphaInt * 0.3f)));
                canvas.drawLine(s.prevX, s.prevY, s.x, s.y, paint);
                // bright core
                paint.setStrokeWidth(s.size * density);
                paint.setColor(withAlpha(s.color, alphaInt));
                canvas.drawLine(s.prevX, s.prevY, s.x, s.y, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                float coreRadius = s.size * density * 0.6f;
                paint.setColor(withAlpha(s.color, (int) (alphaInt * 0.28f)));
                canvas.drawCircle(s.x, s.y, coreRadius * 2.2f, paint);
                paint.setColor(withAlpha(s.color, alphaInt));
                canvas.drawCircle(s.x, s.y, coreRadius, paint);
            }
        }
    }

    private int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return Color.argb(clamped, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static class Rocket {
        float x, y, startY, targetY;
        int color;
        BurstType type;
    }

    private static class Spark {
        float x, y, prevX, prevY, vx, vy, size, alpha, gravityMul, fadeRate, ageMs, flickerPhase;
        int color;
        boolean trail;
        boolean crossette;
        boolean splitDone;
        boolean flicker;
    }
}
