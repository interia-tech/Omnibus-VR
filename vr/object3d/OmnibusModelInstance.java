package pl.interia.omnibus.vr.object3d;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Disposable;

import lombok.Getter;
import lombok.Setter;

public abstract class OmnibusModelInstance implements Disposable {
    protected boolean isClickable = false;
    private BoundingBox boundingBox = new BoundingBox();
    protected ModelInstance model;
    private final String modelFilename;
    private AnimationController anim;
    @Getter
    protected boolean useEnvironment = true;
    @Setter
    @Getter
    private boolean shouldBeRendered = true;


    public OmnibusModelInstance(String modelFilename, AssetManager assets) {
        this.modelFilename = modelFilename;
        assets.load(modelFilename, Model.class);
    }

    public void notifyDoneLoading(AssetManager assets){
        model = new ModelInstance(assets.get(modelFilename, Model.class));
        fixEmissiveColor();
        recalculateBoundingBox();
    }

    /**
     * Object exported from blender to fbx and converter via fbx-conv to g3db have bad emissive value set to 0.8
     * we must change it to 0
     */
    private void fixEmissiveColor(){
        for(Material m : model.materials){
            for (Attribute a : m) {
                if (a.type == ColorAttribute.Emissive) {
                    ((ColorAttribute) a).color.set(0, 0, 0, 0);
                }
            }
        }
    }


    protected void recalculateBoundingBox() {
        model.calculateBoundingBox(boundingBox);
        boundingBox.mul(model.transform);
    }

    public void updateAnimations(float delta){
        if(anim != null){
            anim.update(delta);
        }
    }

    public void runAnimation(String animName, Runnable runAfterEnd){
        anim = new AnimationController(model);
        anim.setAnimation(animName, new AnimationController.AnimationListener() {
            @Override
            public void onEnd(AnimationController.AnimationDesc animation) {
                if(runAfterEnd != null){
                    runAfterEnd.run();
                }
            }

            @Override
            public void onLoop(AnimationController.AnimationDesc animation) {
                //nothing now
            }
        });
    }

    @Override
    public void dispose() {

    }

    public void render(ModelBatch modelBatch, Environment environment) {
        if(shouldBeRendered) {
            modelBatch.render(model, environment);
        }
    }

    public OmnibusModelInstance getFocused(Ray ray, Vector3 intersection){
        if (isClickable && Intersector.intersectRayBounds(ray, boundingBox, intersection)) {
            return this;
        }
        return null;
    }

    public void setToTranslation(float x, float y, float z){
        model.transform.setToTranslation(x, y, z);
        recalculateBoundingBox();
    }

    public Material getMaterial(String id){
        return model.getMaterial(id);
    }
}

