package dev.steamy.bitcointicker;

import android.graphics.drawable.GradientDrawable;

final class ShapeFactory {
    private ShapeFactory() {}

    static GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusDp * android.content.res.Resources.getSystem()
                .getDisplayMetrics().density);
        return shape;
    }

    static GradientDrawable roundedBorder(int color, int borderColor,
                                          int radiusDp, int borderDp) {
        GradientDrawable shape = rounded(color, radiusDp);
        float density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
        shape.setStroke(Math.max(1, Math.round(borderDp * density)), borderColor);
        return shape;
    }

    static GradientDrawable roundedGradient(int startColor, int endColor, int radiusDp) {
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{startColor, endColor});
        shape.setCornerRadius(radiusDp * android.content.res.Resources.getSystem()
                .getDisplayMetrics().density);
        return shape;
    }
}
