package pl.interia.omnibus.vr.object3d.room;

import com.badlogic.gdx.assets.AssetManager;

import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;

public class RoomModelInstance extends OmnibusModelInstance {
    private static final String MODEL_FILENAME = "3d/objects/google_room.g3db";

    public RoomModelInstance(AssetManager assets) {
        super(MODEL_FILENAME, assets);
        useEnvironment = false;
    }
}
