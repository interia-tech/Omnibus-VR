package pl.interia.omnibus.vr.utils;

import android.graphics.Bitmap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

import pl.interia.omnibus.utils.UtilInitializeException;

public class TextureUtils {

    private TextureUtils() {
        throw new UtilInitializeException();
    }

    public static void setAsTexture(Texture texture, Material material, Bitmap bitmap) {
        Pixmap p = ImageUtils.bitmapToPixmap(bitmap);
        Gdx.app.postRunnable(() -> {
            texture.draw(p, 0, 0);
            material.set(TextureAttribute.createDiffuse(texture));
            p.dispose();
        });
    }
}
