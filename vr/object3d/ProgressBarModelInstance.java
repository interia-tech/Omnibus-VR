package pl.interia.omnibus.vr.object3d;

import android.content.Context;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;

import pl.interia.omnibus.R;
import timber.log.Timber;

import static pl.interia.omnibus.container.olympiad.TimerView.TIME_BAR_WARNING_COLOR_TIME_PERCENTAGE;
import static pl.interia.omnibus.vr.utils.DrawUtils.argbTorgbaColor;

public class ProgressBarModelInstance extends OmnibusModelInstance{
    private static final String MODEL_FILENAME = "3d/objects/progressBar.g3db";
    private Texture texture;
    private Material material;
    private long endTimeMill = -1;
    private long durationMill = -1;
    private Matrix4 oryginalTransform;
    private Matrix4 tmp;
    private Pixmap blue = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
    private Pixmap red = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
    private Pixmap active;
    private Runnable runAfterTimeEnd;

    public ProgressBarModelInstance(AssetManager assets, Context ctx) {
        super(MODEL_FILENAME, assets);
        blue.setColor(argbTorgbaColor(ctx.getResources().getColor(R.color.colorAccent)));
        blue.fill();
        red.setColor(argbTorgbaColor(ctx.getResources().getColor(R.color.colorLowTime)));
        red.fill();
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        material = getMaterial("Material");
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
        blue.dispose();
        red.dispose();
    }

    @Override
    public void updateAnimations(float delta) {
        super.updateAnimations(delta);
        if(endTimeMill != -1) {
            float scaleX;
            long now = System.currentTimeMillis();
            if (endTimeMill <= now) {
                scaleX = 0;
                endTimeMill = -1;
                runAfterTimeEnd.run();
                runAfterTimeEnd = null;
            } else {
                scaleX = (endTimeMill - now) / (float)durationMill;
                if(scaleX < TIME_BAR_WARNING_COLOR_TIME_PERCENTAGE && active != red){
                    setTexture(red);
                }
            }
            scale(scaleX, 1, 1);
        }
    }

    @Override
    public void setToTranslation(float x, float y, float z) {
        super.setToTranslation(x, y, z);
        oryginalTransform = new Matrix4(model.transform);
    }

    public void show(long durationMill, Runnable runAfterTimeEnd) {
        setTexture(blue);
        tmp = new Matrix4(oryginalTransform);
        scale(1, 1,1);
        runAnimation("progressBar|show", () -> {
            this.durationMill = durationMill;
            endTimeMill = System.currentTimeMillis() + durationMill;
            Timber.d("Progress end at %s", System.currentTimeMillis());
            this.runAfterTimeEnd = runAfterTimeEnd;
        });
    }

    public void hide() {
        endTimeMill = -1;
        tmp = new Matrix4(model.transform);
        scale(0, 0,0);
    }

    private void scale(float scaleX, float scaleY, float scaleZ){
        model.transform.set(tmp);
        model.transform.scale(scaleX, scaleY, scaleZ);
        model.calculateTransforms();
    }

    private void setTexture(Pixmap p){
        texture = new Texture(p);
        material.set(TextureAttribute.createDiffuse(texture));
        active = p;
    }
}
