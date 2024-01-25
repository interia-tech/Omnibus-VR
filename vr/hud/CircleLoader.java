package pl.interia.omnibus.vr.hud;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Disposable;

import java.util.concurrent.TimeUnit;

import pl.interia.omnibus.vr.RadialSprite;

public class CircleLoader extends Image implements Disposable {

    private RadialSprite sprite;
    private Texture text;
    private static final long DURATION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
    private float currentAngle = -1;
    private boolean isLoading = false;
    private long startTimestamp = 0;
    private boolean isLoaded = false;


    @Override
    public void dispose() {
        if(text != null) {
            text.dispose();
        }
    }

    public void setLoaderTexture(Texture loader){
        text = loader;
        sprite = new RadialSprite(new TextureRegion(text));
        setDrawable(sprite);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        sprite.setAngle(currentAngle);
        super.draw(batch, parentAlpha);
    }

    public void update(){
        if(isLoading){
            float percent = (System.currentTimeMillis() - startTimestamp) / (float)DURATION;
            currentAngle =  360 - 360 * percent;
            if(currentAngle <= 0){
                currentAngle = 0;
                isLoading = false;
                isLoaded = true;
            }
        }
    }

    public void activeLoading(){
        if(!isLoading) {
            isLoading = true;
            isLoaded = false;
            startTimestamp = System.currentTimeMillis();
        }
    }

    public void deactivateLoading(){
        isLoading = false;
        isLoaded = false;
        currentAngle = -0.001f;
    }

    public boolean isLoaded() {
        return isLoaded;
    }
}
