package pl.interia.omnibus.vr.object3d;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextPaint;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

import lombok.Getter;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;
import pl.interia.omnibus.vr.utils.TextureUtils;

import static pl.interia.omnibus.vr.utils.Align.CENTER;

public class ButtonModelInstance extends OmnibusModelInstance {

    private static final String MODEL_FILENAME = "3d/objects/button.g3db";
    private static final float BUTTON_PROPORTION = 0.66f; //height/width
    private static final int BUTTON_TEXTURE_SIZE = 256;
    private Texture texture;
    private final Rect buttonRect = new Rect();
    private final Bitmap bitmap;
    private final Canvas canvas;
    private Material material;
    @Getter
    private Type currentType;

    public enum Type{
        OK("OK", Color.WHITE, Color.GREEN, 70),
        LEARNED("UMIEM", Color.WHITE, Color.GREEN, 60),
        NOT_LEARNED("NIE UMIEM", Color.WHITE, Color.RED, 60),
        RETRY("PONÓW", Color.WHITE, Color.GREEN, 60),
        EXIT("ZAMKNIJ", Color.WHITE, Color.RED, 50),
        STOP("PRZERWIJ", Color.WHITE, Color.RED, 42),
        NEXT_QUESTION("NASTĘPNE PYTANIE", Color.WHITE, Color.GREEN, 40),
        REPEAT_UNLEARNED("POWTÓRZ NIE NAUCZONE", Color.WHITE, Color.GREEN, 40),
        REPEAT_ALL("POWTÓRZ WSZYSTKIE", Color.WHITE, Color.GREEN, 40),
        REPEAT_QUIZ("POWTÓRZ TEST", Color.WHITE, Color.GREEN, 40),
        SUMMARY("PODSUMOWANIE", Color.WHITE, Color.GREEN, 25),
        START("ROZPOCZNIJ", Color.WHITE, Color.parseColor("#324a69"), 35),
        DISCONNECT("ROZŁĄCZ", Color.WHITE, Color.RED, 42),
        REPEAT("POWTÓRZ", Color.WHITE, Color.GREEN, 35);

        private final String text;
        private final int textColor;
        private final int backgroundColor;
        private final float textSize;

        Type(String text, int textColor, int backgroundColor, float textSize) {
            this.text = text;
            this.textColor = textColor;
            this.backgroundColor = backgroundColor;
            this.textSize = textSize;

        }
    }

    public ButtonModelInstance(AssetManager assets) {
        super(MODEL_FILENAME, assets);
        int buttonHeight = Math.round(BUTTON_TEXTURE_SIZE * BUTTON_PROPORTION);
        buttonRect.set(0, 0, BUTTON_TEXTURE_SIZE, buttonHeight);
        bitmap = Bitmap.createBitmap(BUTTON_TEXTURE_SIZE, BUTTON_TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        texture = new Texture(BUTTON_TEXTURE_SIZE, BUTTON_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        material = getMaterial("ButtonMaterial");
        material.set(TextureAttribute.createDiffuse(texture));
    }

    public void prepare(Type type){
        if(!type.equals(currentType)) {
            currentType = type;
            canvas.drawColor(type.backgroundColor);
            TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), type.textColor, type.textSize);
            DrawUtils.drawText(type.text, buttonRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
            TextureUtils.setAsTexture(texture, material, bitmap);
        }
    }

    public void hide(String animHideName){
        hide(animHideName, null);
    }

    public void hide(String animHideName, Runnable runAfterEnd){
        runAnimation(animHideName, runAfterEnd);
        isClickable = false;
    }

    public void show(String animShowName){
        runAnimation(animShowName, ()->{
            recalculateBoundingBox();
            isClickable = true;
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
    }
}
