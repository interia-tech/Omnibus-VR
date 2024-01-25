package pl.interia.omnibus.vr.olympiad.object3d;

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
import com.badlogic.gdx.utils.Timer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import lombok.Getter;
import pl.interia.omnibus.R;
import pl.interia.omnibus.model.api.pojo.Answer;
import pl.interia.omnibus.model.api.pojo.Question;
import pl.interia.omnibus.model.api.pojo.olympiad.OlympiadQuestion;
import pl.interia.omnibus.utils.StringUtils;
import pl.interia.omnibus.vr.object3d.ButtonModelInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelGroupInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.ProgressBarModelInstance;
import pl.interia.omnibus.vr.quiz.model.CachableQuestion;
import pl.interia.omnibus.vr.quiz.object3d.QuizButtonModelInstance;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;
import pl.interia.omnibus.vr.utils.ImageUtils;
import pl.interia.omnibus.vr.utils.TextureUtils;

import static pl.interia.omnibus.vr.utils.Align.CENTER;
import static pl.interia.omnibus.vr.utils.Align.START;
import static pl.interia.omnibus.vr.utils.Scale.INSIDE;

public class OlympiadQuestionModelInstance extends OmnibusModelGroupInstance {
    private static final String MODEL_FILENAME = "3d/objects/quizQuestion.g3db";
    private static final float QUIZ_SIDE_PROPORTION = 1.4f; //height/width
    private static final float IMAGE_MAX_HEIGHT = 0.6f;
    private static final int QUIZ_SIDE_PADDING = 30;
    private static final int QUIZ_TEXTURE_SIZE = 1024;
    private static final int PROGRESS_SECTION_SIZE = 46;
    private final int progressTextColor;
    @Getter
    private boolean isShowed = false;
    @Getter
    private boolean isHiding = false;
    @Getter
    private boolean isFlippedBack = false;
    private boolean isAnswersButtonsShowed = false;
    private boolean isStopAndGoNexButtonsShowed = false;
    private Texture texture;
    private final Rect sidePaddedRect = new Rect();
    private final Rect sideRect = new Rect();
    private final Rect imageRect = new Rect();
    private final Rect textRect = new Rect();
    private final Bitmap bitmap;
    private final Canvas canvas;
    private Material material;
    private QuizButtonModelInstance button1;
    private QuizButtonModelInstance button2;
    private QuizButtonModelInstance button3;
    private QuizButtonModelInstance button4;
    private ButtonModelInstance stop;
    private ButtonModelInstance nextQuestion;
    private ButtonModelInstance summary;
    private ButtonModelInstance exit;
    private ProgressBarModelInstance progressBar;
    private int quizSideHeight;
    private final Context ctx;
    private static final long ANSWERS_SHOW_DURATION = 250;
    private static final long ANSWERS_HIDE_DURATION = 250;
    private static final long QUESTION_SHOW_DURATION = 750;
    private static final long QUESTION_FLIP_DURATION = 250;

    public OlympiadQuestionModelInstance(AssetManager assets, Context ctx) {
        super(MODEL_FILENAME, assets);
        this.progressTextColor = ctx.getResources().getColor(R.color.textHint);
        this.ctx = ctx;
        button1 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        button2 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        button3 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        button4 = (QuizButtonModelInstance)addChild(new QuizButtonModelInstance(assets));
        stop = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        nextQuestion = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        summary = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        exit = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        progressBar = (ProgressBarModelInstance)addChild(new ProgressBarModelInstance(assets, ctx));
        quizSideHeight = QUIZ_TEXTURE_SIZE / 2;
        int quizSideWidth = Math.round(quizSideHeight * QUIZ_SIDE_PROPORTION);
        sideRect.set(0, 0, quizSideWidth, quizSideHeight);
        sidePaddedRect.set(QUIZ_SIDE_PADDING, QUIZ_SIDE_PADDING, quizSideWidth - QUIZ_SIDE_PADDING, quizSideHeight - QUIZ_SIDE_PADDING);
        imageRect.set(sidePaddedRect.left, sidePaddedRect.top, sidePaddedRect.right, Math.round(sidePaddedRect.height() * IMAGE_MAX_HEIGHT));
        textRect.set(sidePaddedRect.left, imageRect.bottom, sidePaddedRect.right, sidePaddedRect.bottom - PROGRESS_SECTION_SIZE);
        bitmap = Bitmap.createBitmap(QUIZ_TEXTURE_SIZE, QUIZ_TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        texture = new Texture(QUIZ_TEXTURE_SIZE, QUIZ_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        canvas.drawColor(Color.WHITE);

    }

    public void flipToBack(Runnable runAfterEnd){
        runAnimation("QuizQuestion|rotate_back", () -> {
            isFlippedBack = true;
            if(runAfterEnd != null) {
                runAfterEnd.run();
            }
        });
    }

    public void flipToFront(Runnable runAfter){
        runAnimation("QuizQuestion|rotate_front", () -> {
            isFlippedBack = false;
            runAfter.run();
        });
    }

    public boolean isAnswerClick(OmnibusModelInstance object) {
        return button1.equals(object) || button2.equals(object) || button3.equals(object) || button4.equals(object);
    }

    public boolean isStopClick(OmnibusModelInstance object) {
        return stop.equals(object);
    }

    public boolean isExitClick(OmnibusModelInstance object) {
        return exit.equals(object);
    }

    public boolean isNextQuestionClick(OmnibusModelInstance object) {
        return nextQuestion.equals(object);
    }

    public boolean isSummaryClick(OmnibusModelInstance object) {
        return summary.equals(object);
    }

    public void hide(Runnable runAfterEnd){
        if(isShowed) {
            runAnimation(isFlippedBack ? "QuizQuestion|hide_back" : "QuizQuestion|hide_front", () -> {
                isHiding = false;
                runAfterEnd.run();
            });
            isHiding = true;
            isShowed = false;
            if(isAnswersButtonsShowed) {
                hideAnswersButtons(null);
            }
            hideStopAndGoNextButtons(null);
        }else{
            isHiding = false;
            runAfterEnd.run();
        }
    }

    public void showQuestion(CachableQuestion question, Runnable runAfterQuestionTimeout){
        OlympiadQuestion oq = (OlympiadQuestion)question.getData();
        long cachingDuration = System.currentTimeMillis() - question.getCreationTime();
        long timeToExpire = oq.getTimestampToExpire() - oq.getServerTimestamp();
        long timeToReadLeft = timeToExpire - TimeUnit.MILLISECONDS.convert(oq.getTimeToAnswer(), TimeUnit.SECONDS);
        long timeToAnswerLeft = timeToExpire - TimeUnit.MILLISECONDS.convert(oq.getTimeToRead(), TimeUnit.SECONDS);
        if(!isShowed()) {
            long realTimeToReadLeft = calculateRealTimeToReadLeft(timeToReadLeft, cachingDuration, QUESTION_SHOW_DURATION);
            long realTimeToAnswerLeft = calculateRealTimeToAnswerLeft(realTimeToReadLeft, timeToAnswerLeft, cachingDuration, QUESTION_SHOW_DURATION);
            show(realTimeToReadLeft, realTimeToAnswerLeft, runAfterQuestionTimeout);
        } else {
            long realTimeToReadLeft = calculateRealTimeToReadLeft(timeToReadLeft, cachingDuration, QUESTION_FLIP_DURATION);
            long realTimeToAnswerLeft = calculateRealTimeToAnswerLeft(realTimeToReadLeft, timeToAnswerLeft, cachingDuration, QUESTION_FLIP_DURATION);
            flipToFront(() -> Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    showAnswersButtons(realTimeToAnswerLeft, runAfterQuestionTimeout);
                }
            }, realTimeToReadLeft / 1000f));
        }
    }

    private long calculateRealTimeToReadLeft(long timeToReadLeft, long cachingTime, long qestionAnimationDuration){
       return Math.max(0, timeToReadLeft - cachingTime - ANSWERS_SHOW_DURATION - qestionAnimationDuration);
    }

    private long calculateRealTimeToAnswerLeft(long realTimeToReadLeft, long timeToAnswerLeft, long cachingTime, long qestionAnimationDuration){
        long ret = timeToAnswerLeft - ANSWERS_HIDE_DURATION;
        if(realTimeToReadLeft == 0){
            ret = ret - cachingTime - ANSWERS_SHOW_DURATION - qestionAnimationDuration;
        }
        return Math.max(0, ret);
    }

    private void show(long timeToReadLeft, long timeToAnswerLeft, Runnable runAfterTimeEnd){
        show(() -> Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                showAnswersButtons(timeToAnswerLeft, runAfterTimeEnd);
            }
        }, timeToReadLeft / 1000f), false);
    }

    public void show(Runnable runAfterEnd, boolean showFlipped){
        runAnimation(showFlipped  ? "QuizQuestion|show_back" : "QuizQuestion|show_front", runAfterEnd);
        isShowed = true;
        isFlippedBack = showFlipped;
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        material = getMaterial("Material");
        material.set(TextureAttribute.createDiffuse(texture));
        button1.setToTranslation        (-0.35f, 0.7f, -1.0f);
        button2.setToTranslation        (0.35f, 0.7f, -1.0f);
        button3.setToTranslation        (-0.35f, 0.2f, -1.0f);
        button4.setToTranslation        (0.35f, 0.2f, -1.0f);
        stop.setToTranslation           (-0.35f, 1.4f, -1.1f);
        nextQuestion.setToTranslation   (0.35f, 1.4f, -1.1f);
        summary.setToTranslation        (0f, 1.4f, -1.1f);
        exit.setToTranslation           (0f, 1.4f, -1.1f);
        progressBar.setToTranslation    (0f, 0.95f, -1.1f);
        stop.prepare(ButtonModelInstance.Type.STOP);
        nextQuestion.prepare(ButtonModelInstance.Type.NEXT_QUESTION);
        summary.prepare(ButtonModelInstance.Type.SUMMARY);
        exit.prepare(ButtonModelInstance.Type.EXIT);
    }

    private void drawQuestionSide(@Nullable Bitmap image, String text, int currentQuestion, int questionsCount, int points){
        offsetAllRectTo(0, 0);
        canvas.drawRect(sideRect, DrawUtils.buildPaint(Color.WHITE));
        Paint paint = DrawUtils.buildPaint();
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.BLACK, 30);
        TextPaint progressPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), progressTextColor, 30);
        Rect imgDest = new Rect(imageRect.left, imageRect.top, imageRect.right, imageRect.top);
        if(image != null) {
            ImageUtils.RectToRect r = ImageUtils.rectToRect(image, INSIDE, START, imageRect);
            DrawUtils.drawImage(image, r, canvas, paint);
            imgDest = r.getDst();
        }
        textRect.set(sidePaddedRect.left, imgDest.bottom, sidePaddedRect.right, sidePaddedRect.bottom - PROGRESS_SECTION_SIZE);
        DrawUtils.drawText(text, textRect, canvas, textPaint, Layout.Alignment.ALIGN_NORMAL, START);
        //progress section
            Rect psr = new Rect(sidePaddedRect.left, textRect.bottom, sidePaddedRect.right, sidePaddedRect.bottom);
            Rect index = new Rect(psr.left, psr.top, psr.left + 300, psr.bottom);
            Rect score = new Rect(psr.right - 300, psr.top, psr.right, psr.bottom);
            //progress
            DrawUtils.drawText((currentQuestion + 1) + " z " + (questionsCount + 1), index, canvas,
                progressPaint, Layout.Alignment.ALIGN_NORMAL, CENTER);
            DrawUtils.drawText(points + " PKT", score, canvas,
                    progressPaint, Layout.Alignment.ALIGN_OPPOSITE, CENTER);
    }

    private void drawScoreSide(int score){
        offsetAllRectTo(0, quizSideHeight);
        canvas.drawRect(sideRect, DrawUtils.buildPaint(score > 0 ? Color.GREEN : Color.RED));
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.WHITE, 90);
        String txt = score == -1 ? "Czas odpowiedzi na pytanie minął." : score + " " + StringUtils.polishPlural(ctx, R.array.numberOfPoints, score);
        DrawUtils.drawText(txt, sidePaddedRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
    }

    private void drawSummarySide(int score){
        offsetAllRectTo(0, 0);
        canvas.drawRect(sideRect, DrawUtils.buildPaint(Color.WHITE));
        int headerHeight = 120;
        int rectHeaderHeight = 70;
        int rectScoreHeight = 160;
        int rectHeight = rectHeaderHeight + rectScoreHeight;
        int rectWidth = 340;
        //header
        Rect headerRect = new Rect(sidePaddedRect.left, sidePaddedRect.top, sidePaddedRect.right, sidePaddedRect.top + headerHeight);
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.BLACK, 30);
        DrawUtils.drawText("To już koniec", headerRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
        //score
        int x = (sideRect.width() - rectWidth) / 2;
        Rect scoreRect = new Rect(x, headerRect.bottom, x + rectWidth, headerRect.bottom + rectHeight);
        canvas.drawRect(scoreRect, DrawUtils.buildPaint(Color.GREEN));
            // score header
            Rect rectHeaderRect = new Rect(scoreRect.left, scoreRect.top, scoreRect.right, scoreRect.top + rectHeaderHeight);
            textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.WHITE, 25);
            DrawUtils.drawText("ZDOBYTE PUNKTY", rectHeaderRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
            // score score label
            Rect rectScoreRect = new Rect(scoreRect.left, rectHeaderRect.bottom, scoreRect.right, rectHeaderRect.bottom + rectScoreHeight);
            textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.WHITE, 90);
            DrawUtils.drawText(""+score, rectScoreRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, START);
        // bottom label
        Rect labelRect = new Rect(sidePaddedRect.left, scoreRect.bottom, sidePaddedRect.right, sidePaddedRect.bottom);
        textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.BLACK, 20);
        DrawUtils.drawText("Poinformujemy Cię o wynikach, gdy olimpiada się zakończy", labelRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
    }

    public void setQuestion(@Nullable Bitmap qImage, Question question, int currentQuestion, int questionsCount, int points) {
        prepareButtons(question.getAnswers());
        drawQuestionSide(qImage, question.getBody(), currentQuestion, questionsCount, points);
        TextureUtils.setAsTexture(texture, material, bitmap);
    }

    public void setScore(int score) {
        drawScoreSide(score);
        TextureUtils.setAsTexture(texture, material, bitmap);
    }

    public void setSummary(int score) {
        drawSummarySide(score);
        TextureUtils.setAsTexture(texture, material, bitmap);
    }

    private void offsetAllRectTo(int x, int y){
        sideRect.offsetTo(x, y);
        sidePaddedRect.offsetTo(QUIZ_SIDE_PADDING + x, QUIZ_SIDE_PADDING + y);
        imageRect.offsetTo(sidePaddedRect.left + x, sidePaddedRect.top + y);
    }


    private void prepareButtons(List<Answer> answers) {
        Collections.shuffle(answers);
        button1.prepare(answers.get(0));
        button2.prepare(answers.get(1));
        button3.prepare(answers.get(2));
        button4.prepare(answers.get(3));
    }

    public void hideAnswersButtons(Runnable runAfter){
        button1.hide();
        button2.hide();
        button3.hide();
        button4.hide(runAfter);
        progressBar.hide();
        isAnswersButtonsShowed = false;
    }

    public void showStopAndGoNextButtons(){
        stop.show("Button|dialog_show_immediately");
        nextQuestion.show("Button|dialog_show_immediately");
        isStopAndGoNexButtonsShowed = true;
    }

    public void hideStopAndGoNextButtons(Runnable runAfterEnd){
        if(isStopAndGoNexButtonsShowed) {
            stop.hide("Button|dialog_hide");
            nextQuestion.hide("Button|dialog_hide", runAfterEnd);
            isStopAndGoNexButtonsShowed = false;
        }
    }

    public void showSummaryButton(){
        summary.show("Button|dialog_show_immediately");
    }

    public void showExityButton(){
        exit.show("Button|dialog_show_immediately");
    }

    public void hideSummaryButtons(Runnable runAfterEnd){
        summary.hide("Button|dialog_hide", runAfterEnd);
    }

    public void showAnswersButtons(long durationMill, Runnable runAfterTimeEnd){
        button1.show();
        button2.show();
        button3.show();
        button4.show();
        progressBar.show(durationMill, runAfterTimeEnd);
        isAnswersButtonsShowed = true;
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
    }
}
