package pl.interia.omnibus.vr.flashcard.object3d;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextPaint;
import android.util.Pair;

import com.badlogic.gdx.assets.AssetManager;

import java.util.Collection;
import java.util.LinkedHashMap;

import pl.interia.omnibus.model.api.pojo.flashcardsset.Flashcard;
import pl.interia.omnibus.vr.object3d.ButtonModelInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.SummaryDialogModelInstance;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;

import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.EXIT;
import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.REPEAT_ALL;
import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.REPEAT_UNLEARNED;
import static pl.interia.omnibus.vr.utils.Align.CENTER;
import static pl.interia.omnibus.vr.utils.Align.START;

public class FlashcardSummaryDialogModelInstance extends SummaryDialogModelInstance<LinkedHashMap<Long, Flashcard>> {
    private static final int SCORE_RECT_HEIGHT = 300;

    public FlashcardSummaryDialogModelInstance(AssetManager assets) {
        super(assets);
        button1 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        button2 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
    }

    @Override
    public void draw(Canvas canvas, LinkedHashMap<Long, Flashcard> data) {
        drawSummary(canvas, data, sideRect);
    }

    @Override
    public void prepareButtons(LinkedHashMap<Long, Flashcard> data) {
        Pair<Integer, Integer> p = calculateScores(data.values());
        button1.prepare(p.second == 0 || p.first == 0? REPEAT_ALL : REPEAT_UNLEARNED);
        button2.prepare(EXIT);
    }

    @Override
    public void setButtonsPosition(){
        if(button2 == null){
            button1.setToTranslation(0, 1.25f, -1.1f);
        }else if(button1 != null){
            button1.setToTranslation(-0.3f, 1.25f, -1.1f);
            button2.setToTranslation(0.3f, 1.25f, -1.1f);
        }
    }

    public boolean isButtonRepeat(OmnibusModelInstance object) {
        return button1.equals(object);
    }

    public boolean isButtonExit(OmnibusModelInstance object) {
        return button2.equals(object);
    }

    public boolean shouldRepeatUnlearned(){
        return button1.getCurrentType() == REPEAT_UNLEARNED;
    }

    public static void drawSummary(Canvas canvas, LinkedHashMap<Long, Flashcard> data, Rect sideRect){
        Pair<Integer, Integer> p = calculateScores(data.values());
        canvas.drawColor(android.graphics.Color.WHITE);
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.BLACK, 50);
        Rect headerLabel = new Rect(sideRect.left, sideRect.top, sideRect.right, sideRect.top + 120);
        DrawUtils.drawText("Podsumowanie Twojej nauki", headerLabel, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, START);
        Rect scores = new Rect(sideRect.left, headerLabel.bottom, sideRect.right, headerLabel.bottom + SCORE_RECT_HEIGHT);
        Rect learned = new Rect(scores.left, scores.top, scores.left + scores.width() / 2, scores.bottom);
        Rect notLearned = new Rect(learned.right, scores.top, scores.right, scores.bottom);
        Paint green = DrawUtils.buildPaint(Color.GREEN);
        Paint red = DrawUtils.buildPaint(Color.RED);
        drawScoreSection(canvas, learned, green, p.first, "JUŻ UMIESZ");
        drawScoreSection(canvas, notLearned, red, p.second, "NIE UMIESZ");
    }

    private static void drawScoreSection(Canvas canvas, Rect externalRect, Paint background, int score, String label){
        Rect scoresLabelRect = new Rect(externalRect.left, externalRect.top, externalRect.right, externalRect.top + (int)(SCORE_RECT_HEIGHT * 0.4f));
        Rect scoresRect = new Rect(externalRect.left, scoresLabelRect.bottom, externalRect.right, scoresLabelRect.bottom + (int)(SCORE_RECT_HEIGHT * 0.6f));
        TextPaint scoreLabel = DrawUtils.buildTextPaint(Fonts.getArialRegular(), Color.WHITE, 40);
        TextPaint scorePaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), Color.WHITE, 90);
        canvas.drawRect(externalRect, background);
        DrawUtils.drawText(label, scoresLabelRect, canvas, scoreLabel, Layout.Alignment.ALIGN_CENTER, CENTER);
        DrawUtils.drawText(""+score, scoresRect, canvas, scorePaint, Layout.Alignment.ALIGN_CENTER, START);
    }

    public static Pair<Integer, Integer> calculateScores(Collection<Flashcard> data){
        int correctCount = 0;
        int errorCount = 0;
        for(Flashcard f : data){
            if(f.isLearned){
                correctCount++;
            }else{
                errorCount++;
            }
        }
        return new Pair<>(correctCount, errorCount);
    }
}
