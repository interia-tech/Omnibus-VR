package pl.interia.omnibus.vr.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.IOException;
import java.io.InputStream;

import lombok.Data;
import pl.interia.omnibus.utils.UtilInitializeException;

import static pl.interia.omnibus.vr.utils.Align.END;
import static pl.interia.omnibus.vr.utils.Align.START;
import static pl.interia.omnibus.vr.utils.Scale.CROP;
import static pl.interia.omnibus.vr.utils.Scale.INSIDE;

public class ImageUtils {

    @Data
    public static class RectToRect {
        public final Rect src;
        public final Rect dst;
    }

    private ImageUtils() {
        throw new UtilInitializeException();
    }

    public static RectToRect rectToRect(@NonNull Bitmap source, Scale sType, Align aType, Rect out) {
        Rectangle src = new Rectangle(0, 0, source.getWidth(), source.getHeight());
        Rectangle target = rectToRectangle(out);
        Rectangle dst = rectToRectangle(out);
        float p = source.getHeight() / (float) source.getWidth();
        if (sType == CROP) {
            float newWidth = target.width;
            float newHeight = target.width * p;
            if (newHeight < target.height) {
                newHeight = target.height;
                p = newHeight / (float) source.getHeight();
                newWidth = Math.round(source.getWidth() * p);
            }
            float offsetX = Math.abs((target.width - newWidth) / 2f);
            float offsetY = Math.abs((target.height - newHeight) / 2f);
            src.x = source.getWidth() * (offsetX / newWidth);
            src.y = source.getHeight() * (offsetY / newHeight);
            src.width = source.getWidth() - src.x * 2;
            src.height = source.getHeight() - src.y * 2;
        } else if (sType == INSIDE) {
            dst.height = target.width * p;
            if (dst.height > target.height) {
                dst.height = target.height;
                p = dst.height / source.getHeight();
                dst.width = source.getWidth() * p;
            }
            dst.x = target.x + (target.width - dst.width) / 2;
            dst.y = target.y + (target.height - dst.height) / 2;
        }
        applyAlign(dst, target, aType);
        return new RectToRect(rectangleToRect(src), rectangleToRect(dst));
    }

    private static Rect rectangleToRect(Rectangle r) {
        return new Rect((int) r.x, (int) r.y, (int) (r.x + r.width), (int) (r.y + r.height));
    }

    private static Rectangle rectToRectangle(Rect r) {
        return new Rectangle(r.left, r.top, r.right - r.left, r.bottom - r.top);
    }

    private static void applyAlign(Rectangle dst, Rectangle target, Align aType) {
        float dstX, dstY;
        if (aType == START) {
            dstX = target.x + (target.width - dst.width) / 2;
            dst.setPosition(dstX, target.y);
        } else if (aType == END) {
            dstX = target.x + (target.width - dst.width) / 2;
            dstY = target.y + target.height - dst.height;
            dst.setPosition(dstX, dstY);
        }
    }

    public static Pixmap bitmapToPixmap(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert from ARGB to RGBA
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            pixels[i] = (pixel << 8) | ((pixel >> 24) & 0xFF);
        }
        Pixmap pixmap = new Pixmap(bitmap.getWidth(), bitmap.getHeight(), Pixmap.Format.RGBA8888);
        pixmap.getPixels().asIntBuffer().put(pixels);
        return pixmap;
    }

    public static Bitmap loadFromAssets(String filename) {
        FileHandle fh = Gdx.files.internal(filename);
        try (InputStream is = fh.read()) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            throw new GdxRuntimeException(e);
        }
    }
}
