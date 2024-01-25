package pl.interia.omnibus.vr.object3d;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

public abstract class OmnibusModelGroupInstance extends OmnibusModelInstance {
    private final Array<OmnibusModelInstance> childs = new Array<>();

    public OmnibusModelGroupInstance(String modelFilename, AssetManager assets) {
        super(modelFilename, assets);
    }

    protected OmnibusModelInstance addChild(OmnibusModelInstance child){
        childs.add(child);
        return child;
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        for(OmnibusModelInstance child : childs){
            child.notifyDoneLoading(assets);
        }
    }

    @Override
    public void render(ModelBatch modelBatch, Environment environment) {
        super.render(modelBatch, environment);
        for(OmnibusModelInstance child : childs){
            child.render(modelBatch, environment);
        }
    }

    @Override
    public void setShouldBeRendered(boolean shouldBeRendered) {
        super.setShouldBeRendered(shouldBeRendered);
        for(OmnibusModelInstance child : childs){
            child.setShouldBeRendered(shouldBeRendered);
        }
    }

    @Override
    public void updateAnimations(float delta) {
        super.updateAnimations(delta);
        for(OmnibusModelInstance child : childs){
            child.updateAnimations(delta);
        }
    }

    @Override
    public OmnibusModelInstance getFocused(Ray ray, Vector3 intersection) {
        for(OmnibusModelInstance child : childs){
            OmnibusModelInstance focused = child.getFocused(ray, intersection);
            if(focused != null){
                return focused;
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
        childs.clear();
    }
}

