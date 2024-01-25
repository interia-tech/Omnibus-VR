package pl.interia.omnibus.vr.quiz.object3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextPaint;

import androidx.appcompat.content.res.AppCompatResources;

import com.badlogic.gdx.assets.AssetManager;

import lombok.Data;
import pl.interia.omnibus.R;
import pl.interia.omnibus.model.api.pojo.AnswerSummary;
import pl.interia.omnibus.model.api.pojo.quiz.QuizSolutionSummary;
import pl.interia.omnibus.utils.DateUtils;
import pl.interia.omnibus.utils.StringUtils;
import pl.interia.omnibus.vr.object3d.ButtonModelInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.SummaryDialogModelInstance;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;

import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.EXIT;
import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.REPEAT_QUIZ;
import static pl.interia.omnibus.vr.utils.Align.CENTER;
import static pl.interia.omnibus.vr.utils.Align.END;
import static pl.interia.omnibus.vr.utils.Align.START;

public class QuizSummaryDialogModelInstance extends SummaryDialogModelInstance<QuizSummaryDialogModelInstance.Solution> {
    private static final int SCORE_RECT_HEIGHT = 300;
    private static final int SIDE_PADDING_LEFT_RIGHT = 90;
    private static final int SIDE_PADDING_TOP_BOTTOM = 50;
    private int blueColor;
    private Drawable clock;

    public QuizSummaryDialogModelInstance(AssetManager assets, Context ctx) {
        super(assets);
        button1 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        button2 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        blueColor = ctx.getResources().getColor(R.color.vrBlue);
        clock = AppCompatResources.getDrawable(ctx, R.drawable.ic_olymp_time_left_red);
        clock.setTint(Color.GRAY);
    }

    @Override
    protected void setupSideRect(Rect sideRect, int width, int height) {
        sideRect.set(SIDE_PADDING_LEFT_RIGHT, SIDE_PADDING_TOP_BOTTOM, width - SIDE_PADDING_LEFT_RIGHT, height - SIDE_PADDING_TOP_BOTTOM);
    }

    @Override
    public void draw(Canvas canvas, Solution data) {
        int correctCount = 0;
        int errorCount = 0;
        int all = data.getSummary().getAnswerSummaries().length;
        for(AnswerSummary a : data.getSummary().getAnswerSummaries()){
            if(a.isCorrect()){
                correctCount++;
            }else{
                errorCount++;
            }
        }
        canvas.drawColor(android.graphics.Color.WHITE);
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), blueColor, 50);
        Rect headerLabel = new Rect(sideRect.left, sideRect.top, sideRect.right, sideRect.top + 100);
        DrawUtils.drawText("Podsumowanie Testu", headerLabel, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, START);
        Rect scoresGroup = new Rect(sideRect.left, headerLabel.bottom, sideRect.right, headerLabel.bottom + SCORE_RECT_HEIGHT);
        Rect percents = new Rect(scoresGroup.left, scoresGroup.top, scoresGroup.right, scoresGroup.top + scoresGroup.height() / 2);
        Rect correct = new Rect(scoresGroup.left, percents.bottom, scoresGroup.left + scoresGroup.width() / 2,
                percents.bottom + scoresGroup.height() / 2);
        Rect incorrect = new Rect(correct.right, percents.bottom, correct.right + scoresGroup.width() / 2,
                percents.bottom + scoresGroup.height() / 2);
        Paint blue = DrawUtils.buildPaint(blueColor);
        canvas.drawRect(percents, blue);
        textPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), Color.WHITE, 80);
        DrawUtils.drawText(StringUtils.formatToPercentage(correctCount, all), percents, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
        Paint green = DrawUtils.buildPaint(Color.GREEN);
        Paint red = DrawUtils.buildPaint(Color.RED);
        drawScoreSection(canvas, correct, green, correctCount, "Poprawne");
        drawScoreSection(canvas, incorrect, red, errorCount, "Błędne");
        DrawUtils.drawDrawable(canvas, sideRect.width() / 2, incorrect.bottom + 30, 0.3f, clock);
        textPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), Color.GRAY, 30);
        Rect clockRect = clock.getBounds();
        Rect time = new Rect(clockRect.right + 15, clockRect.top + 5, clockRect.right + 100, clockRect.bottom + 5);
        DrawUtils.drawText(DateUtils.secondsToMinutesAndSeconds(data.duration), time, canvas, textPaint, Layout.Alignment.ALIGN_NORMAL, CENTER);
    }

    private void drawScoreSection(Canvas canvas, Rect externalRect, Paint background, int score, String label){
        Rect scoresLabelRect = new Rect(externalRect.left, externalRect.top, externalRect.right, externalRect.top + (int)(externalRect.height() * 0.4f));
        Rect scoresRect = new Rect(externalRect.left, scoresLabelRect.bottom, externalRect.right, scoresLabelRect.bottom + (int)(externalRect.height() * 0.6f));
        TextPaint scoreLabel = DrawUtils.buildTextPaint(Fonts.getArialBold(), Color.WHITE, 40);
        TextPaint scorePaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), Color.WHITE, 75);
        canvas.drawRect(externalRect, background);
        scoresLabelRect.offset(0, 10);
        DrawUtils.drawText(label, scoresLabelRect, canvas, scoreLabel, Layout.Alignment.ALIGN_CENTER, END);
        scoresRect.offset(0, -10);
        DrawUtils.drawText(""+score, scoresRect, canvas, scorePaint, Layout.Alignment.ALIGN_CENTER, START);
    }

    @Override
    public void prepareButtons(Solution data){
        button1.prepare(REPEAT_QUIZ);
        button2.prepare(EXIT);
    }

    @Override
    public void setButtonsPosition() {
        if(button2 == null) {
            button1.setToTranslation(0, 1.25f, -1.1f);
        } else if(button1 != null) {
            button1.setToTranslation(-0.3f, 1.25f, -1.1f);
            button2.setToTranslation(0.3f, 1.25f, -1.1f);
        }
    }

    @Data
    public static class Solution {
        final long duration;
        final QuizSolutionSummary summary;
    }

    public boolean isButtonRepeat(OmnibusModelInstance object) {
        return button1.equals(object);
    }

    public boolean isButtonExit(OmnibusModelInstance object) {
        return button2.equals(object);
    }
}
