package pl.interia.omnibus.vr.flashcard.object3d;

import com.badlogic.gdx.assets.AssetManager;

import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;

public class SwitcherModelInstance extends OmnibusModelInstance {
    private static final String MODEL_FILENAME = "3d/objects/switcher.g3db";

    public SwitcherModelInstance(AssetManager assets) {
        super(MODEL_FILENAME, assets);
    }

    public void hide(){
        runAnimation("Switcher|hide", null);
        isClickable = false;
    }

    public void show(){
        runAnimation("Switcher|show", () -> {
            recalculateBoundingBox();
            isClickable = true;
        });
    }
}
