package pl.interia.omnibus.vr.flashcard.object3d;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextPaint;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

import javax.annotation.Nullable;

import lombok.Getter;
import pl.interia.omnibus.vr.object3d.ButtonModelInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelGroupInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;
import pl.interia.omnibus.vr.utils.ImageUtils;
import pl.interia.omnibus.vr.utils.TextureUtils;

import static pl.interia.omnibus.vr.utils.Align.CENTER;
import static pl.interia.omnibus.vr.utils.Align.START;
import static pl.interia.omnibus.vr.utils.Scale.INSIDE;

public class FlashcardModelInstance extends OmnibusModelGroupInstance {
    private static final String MODEL_FILENAME = "3d/objects/flashcard.g3db";
    private static final String TEXTURE_FILENAME = "3d/objects/flashcard.png";
    private static final float FLASHCARD_SIDE_PROPORTION = 1.75f; //height/width
    private static final float IMAGE_MAX_HEIGHT = 0.6f;
    private static final int FLASHCARD_SIDE_PADDING = 30;
    private static final int FLASHCARD_TEXTURE_SIZE = 1024;
    @Getter
    private boolean isQuestionSide = true;
    private Texture texture;
    private final Rect sideRec = new Rect();
    private final Rect imageRect = new Rect();
    private final Rect textRect = new Rect();
    private final Bitmap bitmap;
    private final Canvas canvas;
    private Material material;
    private ButtonModelInstance learned;
    private ButtonModelInstance notLearned;

    public FlashcardModelInstance(AssetManager assets, boolean disableDecisionButtons) {
        super(MODEL_FILENAME, assets);
        if(!disableDecisionButtons) {
            learned = (ButtonModelInstance) addChild(new ButtonModelInstance(assets));
            notLearned = (ButtonModelInstance) addChild(new ButtonModelInstance(assets));
        }
        assets.load(TEXTURE_FILENAME, Pixmap.class);
        int flashcardSideWidth = FLASHCARD_TEXTURE_SIZE / 2;
        int flashcardSideHeight = Math.round(flashcardSideWidth * FLASHCARD_SIDE_PROPORTION);
        sideRec.set(0, 0, flashcardSideWidth - FLASHCARD_SIDE_PADDING * 2, flashcardSideHeight - FLASHCARD_SIDE_PADDING * 2);
        imageRect.set(0, 0, sideRec.right, Math.round(sideRec.height() * IMAGE_MAX_HEIGHT));
        textRect.set(0, imageRect.bottom, sideRec.right, sideRec.bottom);
        bitmap = Bitmap.createBitmap(FLASHCARD_TEXTURE_SIZE, flashcardSideHeight, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

    }

    public void flipToAnswer(){
        flipToAnswer(null);
    }

    public void flipToAnswer(Runnable runAfterEnd){
        runAnimation("Flashcard|flipToAnswer", runAfterEnd);
        isQuestionSide = false;
        if(learned != null) {
            notLearned.setToTranslation(0, 1.17f, -1.1f);
            learned.setToTranslation(0, 1.17f, -1.1f);
            notLearned.show("Button|flashcard_show_left");
            learned.show("Button|flashcard_show_right");
        }
    }

    public void hideLeft(Runnable runAfterEnd){
        runAnimation("Flashcard|hide_left", runAfterEnd);
        if(learned != null) {
            notLearned.hide("Button|flashcard_hide_left");
            learned.hide("Button|flashcard_hide_right");
        }
    }

    public void hideRight(Runnable runAfterEnd){
        runAnimation("Flashcard|hide_right", runAfterEnd);
        if(learned != null) {
            notLearned.hide("Button|flashcard_hide_left");
            learned.hide("Button|flashcard_hide_right");
        }
    }


    public void show(){
        show(null);
    }

    public void show(Runnable runAfterEnd){
        runAnimation("Flashcard|show", runAfterEnd);
        isQuestionSide = true;
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        Pixmap p = assets.get(TEXTURE_FILENAME, Pixmap.class);
        texture = new Texture(p);
        assets.unload(TEXTURE_FILENAME);
        material = getMaterial("FlashcardMaterial");
        material.set(TextureAttribute.createDiffuse(texture));
    }

    private void drawSide(int offsetX, @Nullable Bitmap image, String text, Paint paint, TextPaint textPaint){
        offsetAllRectTo(offsetX);
        Rect imgDest = new Rect(imageRect.left, imageRect.top, imageRect.right, imageRect.top);
        if(image != null) {
            ImageUtils.RectToRect r = ImageUtils.rectToRect(image, INSIDE, START, imageRect);
            DrawUtils.drawImage(image, r, canvas, paint);
            imgDest = r.getDst();
        }
        textRect.set(sideRec.left, imgDest.bottom, sideRec.right, sideRec.bottom);
        DrawUtils.drawText(text, textRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
    }

    public void prepare(@Nullable Bitmap qImage, String qText, @Nullable Bitmap aImage, String aText) {
        if(learned != null) {
            learned.prepare(ButtonModelInstance.Type.LEARNED);
            notLearned.prepare(ButtonModelInstance.Type.NOT_LEARNED);
        }
        Paint paint = DrawUtils.buildPaint();
        canvas.drawColor(Color.WHITE);
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.BLACK, 35);
        drawSide(0, qImage, qText, paint, textPaint);
        drawSide(texture.getWidth() / 2, aImage, aText, paint, textPaint);
        TextureUtils.setAsTexture(texture, material, bitmap);


    }

    private void offsetAllRectTo(int x){
        sideRec.offsetTo(FLASHCARD_SIDE_PADDING + x, FLASHCARD_SIDE_PADDING);
        imageRect.offsetTo(sideRec.left, sideRec.top);
        textRect.offsetTo(sideRec.left, imageRect.bottom);
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
    }

    public boolean isLearned(OmnibusModelInstance object) {
        return learned != null && learned.equals(object);
    }

    public boolean isNotLearned(OmnibusModelInstance object) {
        return notLearned != null && notLearned.equals(object);
    }
}
