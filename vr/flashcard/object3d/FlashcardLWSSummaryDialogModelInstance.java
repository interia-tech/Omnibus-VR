package pl.interia.omnibus.vr.flashcard.object3d;

import android.graphics.Canvas;
import android.util.Pair;

import com.badlogic.gdx.assets.AssetManager;

import java.util.LinkedHashMap;

import pl.interia.omnibus.model.api.pojo.flashcardsset.Flashcard;
import pl.interia.omnibus.vr.object3d.ButtonModelInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.SummaryDialogModelInstance;

import static pl.interia.omnibus.vr.flashcard.object3d.FlashcardSummaryDialogModelInstance.calculateScores;
import static pl.interia.omnibus.vr.flashcard.object3d.FlashcardSummaryDialogModelInstance.drawSummary;
import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.DISCONNECT;
import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.REPEAT;
import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.REPEAT_ALL;
import static pl.interia.omnibus.vr.object3d.ButtonModelInstance.Type.REPEAT_UNLEARNED;

public class FlashcardLWSSummaryDialogModelInstance extends SummaryDialogModelInstance<LinkedHashMap<Long, Flashcard>> {

    private final boolean isStudent;

    public FlashcardLWSSummaryDialogModelInstance(AssetManager assets, boolean isStudent) {
        super(assets);
        button1 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        button2 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        button3 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        this.isStudent = isStudent;
    }

    @Override
    public void draw(Canvas canvas, LinkedHashMap<Long, Flashcard> data) {
        drawSummary(canvas, data, sideRect);
    }

    @Override
    public void prepareButtons(LinkedHashMap<Long, Flashcard> data) {
        Pair<Integer, Integer> p = calculateScores(data.values());
        button1.prepare(DISCONNECT);
        button2.prepare(p.second == 0 || p.first == 0 ? (isStudent ? REPEAT_ALL : REPEAT) : REPEAT_UNLEARNED);
        if(button2.getCurrentType() != REPEAT_ALL) {
            button3.setShouldBeRendered(true);
            button3.prepare(REPEAT_ALL);
        } else {
            button3.setShouldBeRendered(false);
        }
    }

    @Override
    public void setButtonsPosition(){
        if(button3.isShouldBeRendered()){
            button1.setToTranslation(-0.45f, 1.25f, -1.1f);
            button2.setToTranslation(0.0f, 1.25f, -1.1f);
            button3.setToTranslation(0.45f, 1.25f, -1.1f);
        }else{
            button1.setToTranslation(-0.3f, 1.25f, -1.1f);
            button2.setToTranslation(0.3f, 1.25f, -1.1f);
        }
    }

    public boolean isButtonRepeat(OmnibusModelInstance object) {
        return button2.equals(object) || button3.equals(object);
    }

    public boolean isButtonDisconnect(OmnibusModelInstance object) {
        return button1.equals(object);
    }

    public boolean shouldRepeatUnlearned(OmnibusModelInstance object){
        return button2.getCurrentType() == REPEAT_UNLEARNED && !button3.equals(object);
    }
}
