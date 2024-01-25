package pl.interia.omnibus.vr.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import pl.interia.omnibus.utils.UtilInitializeException;

public class DrawUtils {

    private DrawUtils() {
        throw new UtilInitializeException();
    }

    public static void drawImage(Bitmap image, ImageUtils.RectToRect rtr, Canvas canvas, Paint paint) {
        canvas.drawBitmap(image, rtr.src, rtr.dst, paint);
    }

    public static void drawText(String text, Rect dst, Canvas canvas, TextPaint paint, Layout.Alignment textAlignment, Align vertical) {
        StaticLayout sl;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sl = StaticLayout.Builder.obtain(text, 0, text.length(), paint, dst.width())
                    .setAlignment(textAlignment)
                    .build();
        } else {
            sl = new StaticLayout(text, paint, dst.width(), textAlignment, 1, 0, true);
        }
        canvas.save();
        int newY = 0;
        if(vertical == Align.END){
            newY = Math.max(dst.height() - sl.getHeight(), 0);
        }
        if(vertical == Align.CENTER){
            newY = Math.max((dst.height() - sl.getHeight()) / 2, 0);
        }
        canvas.translate(dst.left, dst.top + newY);
        sl.draw(canvas);
        canvas.restore();
    }

    public static TextPaint buildTextPaint(Typeface typeface, int color, float size){
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setFilterBitmap(true);
        textPaint.setDither(true);
        textPaint.setTypeface(typeface);
        textPaint.setColor(color);
        textPaint.setTextSize(size);
        return textPaint;
    }

    public static Paint buildPaint(){
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        return paint;
    }

    public static Paint buildPaint(int color){
        Paint p = buildPaint();
        p.setColor(color);
        return p;
    }

    public static int argbTorgbaColor(int argb){
        int a = Color.alpha(argb);
        int r = Color.red(argb);
        int g = Color.green(argb);
        int b = Color.blue(argb);
        return com.badlogic.gdx.graphics.Color.rgba8888(a, r, g, b);
    }

    public static void drawDrawable(Canvas canva, int x, int y, float scale, Drawable d){
        int dWidth = d.getIntrinsicWidth();
        int dHeight = d.getIntrinsicHeight();
        d.setBounds(x, y, x + Math.round(dWidth * scale), y + Math.round(dHeight * scale));
        d.draw(canva);
    }
}
