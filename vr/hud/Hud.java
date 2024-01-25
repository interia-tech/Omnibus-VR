package pl.interia.omnibus.vr.hud;

import androidx.annotation.Nullable;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.android.CardboardCamera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;

public class Hud implements Disposable {
    private static final String LOADER_TEXTURE_FILENAME = "3d/objects/loader.png";
    private static final float LOADER_SIZE = 50;
    private static final float DOT_RADIUS = 2;
    private Stage stage;
    private Viewport viewport;
    private Dot dot;
    private CircleLoader loader;
    private Array<OmnibusModelInstance> allObjects = new Array<>();
    private OmnibusModelInstance currentFocused;
    private OmnibusModelInstance currentClicked;
    private final OnClickListener listener;

    public Hud (float width, float height, AssetManager assets, OnClickListener listener){
        assets.load(LOADER_TEXTURE_FILENAME, Texture.class);
        this.listener = listener;
        viewport = new FitViewport(width, height, new OrthographicCamera());
        stage = new Stage(viewport, new SpriteBatch());

        loader = new CircleLoader();
        loader.setSize(LOADER_SIZE, LOADER_SIZE);

        dot = new Dot(DOT_RADIUS);
        stage.addActor(dot);

        resize((int)width, (int)height);

    }

    public void setDrawPoint(Vector3 eyeCenter){
        dot.setPosition(eyeCenter.x - dot.getWidth() / 2, eyeCenter.y - dot.getHeight() / 2);
        loader.setPosition(eyeCenter.x - loader.getWidth() / 2, eyeCenter.y - loader.getHeight() / 2);
    }

    public void updateFocusedObject(CardboardCamera cam, Vector3 rayCenter){
        loader.update();
        if(loader.isLoaded()){
            currentClicked = currentFocused;
            listener.onClick(currentClicked);
        }
        OmnibusModelInstance focused = findFocusedObject(cam, rayCenter);
        if(focused != null && (currentFocused == null || !currentFocused.equals(currentClicked))){
            loader.activeLoading();
        }else{
            if(focused == null){
                currentClicked = null;
            }
            loader.deactivateLoading();
        }
        currentFocused = focused;
    }

    @Nullable
    private OmnibusModelInstance findFocusedObject(CardboardCamera cam, Vector3 rayCenter) {
        Ray ray = cam.getPickRay(rayCenter.x, rayCenter.y);
        Vector3 intersection = new Vector3();
        for(OmnibusModelInstance i : allObjects) {
            OmnibusModelInstance focused = i.getFocused(ray, intersection);
            if(focused != null){
                return focused;
            }
        }
        return null;
    }

    public void resize(int width, int height) {
        viewport.setWorldSize(width, height);
        viewport.update(width, height, false);
    }

    public void draw() {
        stage.draw();
    }

    public void notifyDoneLoading(AssetManager assets) {
        Texture text = assets.get(LOADER_TEXTURE_FILENAME, Texture.class);
        this.loader.setLoaderTexture(text);
        stage.addActor(loader);
    }


    @Override
    public void dispose() {
        dot.dispose();
        loader.dispose();
        stage.dispose();
    }

    public void setModels(Array<OmnibusModelInstance> allObjects) {
        this.allObjects = allObjects;
    }

    public interface OnClickListener{
         void onClick(OmnibusModelInstance object);
    }
}
