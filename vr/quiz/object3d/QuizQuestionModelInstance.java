package pl.interia.omnibus.vr.quiz.object3d;

import android.content.Context;
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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import lombok.Getter;
import pl.interia.omnibus.R;
import pl.interia.omnibus.model.api.pojo.Answer;
import pl.interia.omnibus.model.api.pojo.Question;
import pl.interia.omnibus.vr.object3d.OmnibusModelGroupInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;
import pl.interia.omnibus.vr.utils.ImageUtils;
import pl.interia.omnibus.vr.utils.TextureUtils;

import static pl.interia.omnibus.vr.utils.Align.CENTER;
import static pl.interia.omnibus.vr.utils.Align.START;
import static pl.interia.omnibus.vr.utils.Scale.INSIDE;

public class QuizQuestionModelInstance extends OmnibusModelGroupInstance {
    private static final String MODEL_FILENAME = "3d/objects/quizQuestion.g3db";
    private static final float QUIZ_SIDE_PROPORTION = 1.4f; //height/width
    private static final float IMAGE_MAX_HEIGHT = 0.6f;
    private static final int QUIZ_SIDE_PADDING = 30;
    private static final int QUIZ_TEXTURE_SIZE = 1024;
    private static final int PROGRESS_SIZE = 23;
    private final int progressTextColor;
    @Getter
    private boolean isShowed = false;
    @Getter
    private boolean isHiding = false;
    @Getter
    private boolean isFlippedBack = false;
    private boolean isButtonsShowed = false;
    private Texture texture;
    private final Rect sideRec = new Rect();
    private final Rect imageRect = new Rect();
    private final Rect textRect = new Rect();
    private final Rect progressRectangle = new Rect();
    private final Bitmap bitmap;
    private final Canvas canvas;
    private Material material;
    private QuizButtonModelInstance button1;
    private QuizButtonModelInstance button2;
    private QuizButtonModelInstance button3;
    private QuizButtonModelInstance button4;

    public QuizQuestionModelInstance(AssetManager assets, Context ctx) {
        super(MODEL_FILENAME, assets);
        this.progressTextColor = ctx.getResources().getColor(R.color.textHint);
        button1 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        button2 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        button3 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        button4 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        int quizSideHeight = QUIZ_TEXTURE_SIZE / 2;
        int quizSideWidth = Math.round(quizSideHeight * QUIZ_SIDE_PROPORTION);
        sideRec.set(QUIZ_SIDE_PADDING, QUIZ_SIDE_PADDING, quizSideWidth - QUIZ_SIDE_PADDING, quizSideHeight - QUIZ_SIDE_PADDING);
        imageRect.set(sideRec.left, sideRec.top, sideRec.right, Math.round(sideRec.height() * IMAGE_MAX_HEIGHT));
        textRect.set(sideRec.left, imageRect.bottom, sideRec.right, sideRec.bottom - PROGRESS_SIZE);
        progressRectangle.set(sideRec.left, textRect.bottom, sideRec.right, sideRec.bottom);
        bitmap = Bitmap.createBitmap(QUIZ_TEXTURE_SIZE, QUIZ_TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        texture = new Texture(QUIZ_TEXTURE_SIZE, QUIZ_TEXTURE_SIZE, Pixmap.Format.RGBA8888);

    }

    public void flipToBack(Runnable runAfterEnd){
        runAnimation("QuizQuestion|rotate_back", () -> {
            isFlippedBack = true;
            runAfterEnd.run();
        });
        hideButtons();
    }

    public void flipToFront(){
        runAnimation("QuizQuestion|rotate_front", () -> {
            isFlippedBack = false;
            showButtons();
        });
    }

    public boolean isAnswerClick(OmnibusModelInstance object) {
        return button1.equals(object) || button2.equals(object) || button3.equals(object) || button4.equals(object);
    }

    public void hide(Runnable runAfterEnd){
        if(isShowed) {
            runAnimation(isFlippedBack ? "QuizQuestion|hide_back" : "QuizQuestion|hide_front", () -> {
                isHiding = false;
                runAfterEnd.run();
            });
            isHiding = true;
            isShowed = false;
            if(isButtonsShowed) {
                hideButtons();
            }
        }else{
            isHiding = false;
            runAfterEnd.run();
        }
    }

    public void show(){
        runAnimation("QuizQuestion|show_front", this::showButtons);
        isShowed = true;
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        material = getMaterial("Material");
        material.set(TextureAttribute.createDiffuse(texture));
        button1.setToTranslation(-0.35f, 0.7f, -1.0f);
        button2.setToTranslation(0.35f, 0.7f, -1.0f);
        button3.setToTranslation(-0.35f, 0.2f, -1.0f);
        button4.setToTranslation(0.35f, 0.2f, -1.0f);
    }

    private void drawSide(@Nullable Bitmap image, String text, int currentQuestion, int questionsCount){
        Paint paint = DrawUtils.buildPaint();
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.BLACK, 35);
        TextPaint progressPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), progressTextColor, PROGRESS_SIZE - 2);
        Rect imgDest = new Rect(imageRect.left, imageRect.top, imageRect.right, imageRect.top);
        if(image != null) {
            ImageUtils.RectToRect r = ImageUtils.rectToRect(image, INSIDE, START, imageRect);
            DrawUtils.drawImage(image, r, canvas, paint);
            imgDest = r.getDst();
        }
        textRect.set(sideRec.left, imgDest.bottom, sideRec.right, sideRec.bottom - PROGRESS_SIZE);
        DrawUtils.drawText(text, textRect, canvas, textPaint, Layout.Alignment.ALIGN_NORMAL, START);
        DrawUtils.drawText((currentQuestion + 1) + " z " + questionsCount, progressRectangle, canvas,
                progressPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
    }

    public void prepare(@Nullable Bitmap qImage, Question question, int currentQuestion, int questionsCount) {
        prepareButtons(question.getAnswers());
        canvas.drawColor(Color.WHITE);
        drawSide(qImage, question.getBody(), currentQuestion, questionsCount);
        TextureUtils.setAsTexture(texture, material, bitmap);
    }

    private void prepareButtons(List<Answer> answers) {
        Collections.shuffle(answers);
        button1.prepare(answers.get(0));
        button2.prepare(answers.get(1));
        button3.prepare(answers.get(2));
        button4.prepare(answers.get(3));
    }

    private void hideButtons(){
        button1.hide();
        button2.hide();
        button3.hide();
        button4.hide();
        isButtonsShowed = false;
    }

    private void showButtons(){
        isButtonsShowed = true;
        button1.show();
        button2.show();
        button3.show();
        button4.show();
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
    }
}
