package com.z7ui.edgepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

/**
 * 屏幕边缘那条手柄。只负责识别手势，动作交给 Actions。
 *
 * 手势判定：先看「往屏幕里面」滑了多少，再看方向（水平 / 斜上 / 斜下），
 * 最后按滑动距离分短滑和长滑。距离阈值取屏高的 16%。
 */
public class HandleView extends View {

    private final String side;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int baseAlpha;

    private float downX, downY;
    private boolean tracking;
    private boolean hot;

    public HandleView(Context c, String side) {
        super(c);
        this.side = side;
        baseAlpha = Math.round(Config.alphaPct(c) * 255f / 100f);
        bg.setColor(Color.argb(baseAlpha, 0x2E, 0x6B, 0xE6));
        grip.setColor(Color.argb(Math.min(255, baseAlpha + 90), 0xFF, 0xFF, 0xFF));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float r = Math.min(w, h) / 2f;
        rect.set(0, 0, w, h);
        bg.setAlpha(hot ? 230 : baseAlpha);
        canvas.drawRoundRect(rect, r, r, bg);

        // 中间两道短横线当握持提示
        float cx = w / 2f;
        float cy = h / 2f;
        float len = Math.max(w * 0.36f, 4f);
        grip.setStrokeWidth(Math.max(w * 0.08f, 2f));
        grip.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx - len / 2f, cy - w * 0.22f, cx + len / 2f, cy - w * 0.22f, grip);
        canvas.drawLine(cx - len / 2f, cy + w * 0.22f, cx + len / 2f, cy + w * 0.22f, grip);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getRawX();
                downY = e.getRawY();
                tracking = true;
                hot = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                return tracking;
            case MotionEvent.ACTION_UP:
                if (tracking) {
                    tracking = false;
                    hot = false;
                    invalidate();
                    String g = classify(e.getRawX() - downX, e.getRawY() - downY);
                    if (g != null) fire(g);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                hot = false;
                invalidate();
                return true;
            default:
                return false;
        }
    }

    /** 返回 short_h / long_h / short_up / long_up / short_down / long_down，认不出返回 null。 */
    private String classify(float dx, float dy) {
        float inward = Config.RIGHT.equals(side) ? -dx : dx;
        if (inward <= 0) return null;

        float adx = Math.abs(dx);
        float ady = Math.abs(dy);
        float dist = (float) Math.hypot(adx, ady);
        if (dist < dp(14)) return null;

        String dir;
        if (adx >= ady * 1.35f) {
            dir = "h";
        } else if (dy < 0) {
            dir = "up";
        } else {
            dir = "down";
        }

        float screenH = getResources().getDisplayMetrics().heightPixels;
        boolean longSwipe = dist > screenH * 0.16f;
        return (longSwipe ? "long_" : "short_") + dir;
    }

    private void fire(String gesture) {
        vibrate();
        String action = Config.action(getContext(), side, gesture);
        Actions.run(getContext(), action);
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(14, 60));
            }
        } catch (Throwable ignored) {
        }
    }

    private float dp(float v) {
        return getResources().getDisplayMetrics().density * v;
    }
}
