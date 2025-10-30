package com.example.loyaltyprogram;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ViewfinderOverlay extends View {
    private final Paint dimPaint = new Paint();
    private final Paint strokePaint = new Paint();
    private RectF frame;

    public ViewfinderOverlay(Context c, AttributeSet a) {
        super(c, a);
        dimPaint.setColor(Color.parseColor("#88000000"));
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setAntiAlias(true);

        strokePaint.setColor(Color.WHITE);
        strokePaint.setStrokeWidth(6f);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float size = Math.min(w, h) * 0.65f;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        frame = new RectF(left, top, left + size, top + size);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (frame == null) return;

        Path p = new Path();
        p.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        p.addRoundRect(frame, 24f, 24f, Path.Direction.CCW);
        p.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(p, dimPaint);

        float c = 48f;
        // TL
        canvas.drawLine(frame.left, frame.top, frame.left + c, frame.top, strokePaint);
        canvas.drawLine(frame.left, frame.top, frame.left, frame.top + c, strokePaint);
        // TR
        canvas.drawLine(frame.right - c, frame.top, frame.right, frame.top, strokePaint);
        canvas.drawLine(frame.right, frame.top, frame.right, frame.top + c, strokePaint);
        // BL
        canvas.drawLine(frame.left, frame.bottom - c, frame.left, frame.bottom, strokePaint);
        canvas.drawLine(frame.left, frame.bottom, frame.left + c, frame.bottom, strokePaint);
        // BR
        canvas.drawLine(frame.right - c, frame.bottom, frame.right, frame.bottom, strokePaint);
        canvas.drawLine(frame.right, frame.bottom - c, frame.right, frame.bottom, strokePaint);
    }
}
