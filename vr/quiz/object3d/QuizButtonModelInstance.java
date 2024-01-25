package pl.interia.omnibus.vr.quiz.object3d;

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
import pl.interia.omnibus.model.api.pojo.Answer;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;
import pl.interia.omnibus.vr.utils.TextureUtils;

import static pl.interia.omnibus.vr.utils.Align.CENTER;

public class QuizButtonModelInstance extends OmnibusModelInstance {

    private static final String MODEL_FILENAME = "3d/objects/quizButton.g3db";
    private static final float BUTTON_PROPORTION = 0.66f; //height/width
    private static final int BUTTON_TEXTURE_SIZE = 256;
    private static final int BUTTON_SIDE_PADDING = 5;
    private Texture texture;
    private final Rect buttonRect = new Rect();
    private final Bitmap bitmap;
    private final Canvas canvas;
    private Material material;
    @Getter
    private Answer answer;

    public QuizButtonModelInstance(AssetManager assets) {
        super(MODEL_FILENAME, assets);
        int buttonHeight = Math.round(BUTTON_TEXTURE_SIZE * BUTTON_PROPORTION);
        buttonRect.set(BUTTON_SIDE_PADDING, BUTTON_SIDE_PADDING, BUTTON_TEXTURE_SIZE - 2 * BUTTON_SIDE_PADDING, buttonHeight - 2 * BUTTON_SIDE_PADDING);
        bitmap = Bitmap.createBitmap(BUTTON_TEXTURE_SIZE, BUTTON_TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        texture = new Texture(BUTTON_TEXTURE_SIZE, BUTTON_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        material = getMaterial("Material");
        material.set(TextureAttribute.createDiffuse(texture));
    }

    public void prepare(Answer answer){
        this.answer = answer;
        canvas.drawColor(Color.WHITE);
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), Color.BLACK, 25);
        DrawUtils.drawText(answer.getBody(), buttonRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
        TextureUtils.setAsTexture(texture, material, bitmap);
    }

    public void hide(Runnable runAfter){
        runAnimation("QuizButton|hide", runAfter);
        isClickable = false;
    }

    public void hide(){
        hide(null);
    }

    public void show(){
        show(null);
    }

    public void show(Runnable runAfter){
        runAnimation("QuizButton|show", ()->{
            recalculateBoundingBox();
            isClickable = true;
            if(runAfter != null){
                runAfter.run();
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
    }
}
