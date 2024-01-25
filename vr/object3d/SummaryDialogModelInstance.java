package pl.interia.omnibus.vr.object3d;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import pl.interia.omnibus.vr.utils.TextureUtils;

import static pl.interia.omnibus.vr.object3d.dialog.DialogModelInstance.MODEL_FILENAME;
import static pl.interia.omnibus.vr.object3d.dialog.DialogModelInstance.PROPORTION;
import static pl.interia.omnibus.vr.object3d.dialog.DialogModelInstance.TEXTURE_SIZE;

public abstract class SummaryDialogModelInstance<T> extends OmnibusModelGroupInstance {
    private static final int SIDE_PADDING = 90;
    protected ButtonModelInstance button1;
    protected ButtonModelInstance button2;
    protected ButtonModelInstance button3;
    protected final Rect sideRect = new Rect();
    private final Bitmap bitmap;
    private final Canvas canvas;
    private Texture texture;
    private Material material;
    @Getter
    private boolean isHiding = false;
    @Getter
    private boolean isVisible = false;

    public SummaryDialogModelInstance(AssetManager assets) {
        super(MODEL_FILENAME, assets);
        int width = TEXTURE_SIZE;
        int height = Math.round(TEXTURE_SIZE * PROPORTION);
        setupSideRect(sideRect, width, height);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        Pixmap p = new Pixmap(TEXTURE_SIZE, TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        p.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        p.fill();
        texture = new Texture(p);
        material = getMaterial("DialogMaterial");
        material.set(TextureAttribute.createDiffuse(texture));
    }

    public void show(T data){
        show(data, null);
    }

    public void show(T data, Runnable runAfter){
        isVisible = true;
        Schedulers.io().scheduleDirect(() -> {
            prepare(data);
            Gdx.app.postRunnable(() -> {
                runAnimation("Dialog|show", runAfter);
                if(button1 != null){
                    button1.show("Button|dialog_show");
                }
                if(button2 != null){
                    button2.show("Button|dialog_show");
                }
                if(button3 != null){
                    button3.show("Button|dialog_show");
                }
            });
        });
    }

    public void hide(Runnable runAfterEnd){
        isHiding = true;
        isVisible = true;
        setButtonsPosition();
        runAnimation("Dialog|hide", () ->{
            isHiding = false;
            isVisible = false;
            if(runAfterEnd != null) {
                runAfterEnd.run();
            }
        });
        if(button1 != null) {
            button1.hide("Button|dialog_hide");
        }
        if(button2 != null){
            button2.hide("Button|dialog_hide");
        }
        if(button3 != null){
            button3.hide("Button|dialog_hide");
        }
    }

    private void prepare(T data){
        prepareButtons(data);
        setButtonsPosition();
        draw(canvas, data);
        TextureUtils.setAsTexture(texture, material, bitmap);

    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
    }

    protected void setupSideRect(Rect sideRect, int width, int height){
        sideRect.set(SIDE_PADDING, SIDE_PADDING, width - SIDE_PADDING, height - SIDE_PADDING);
    }

    public abstract void draw(Canvas canvas, T data);
    public abstract void setButtonsPosition();
    public abstract void prepareButtons(T data);

    public boolean isButton1(OmnibusModelInstance object) {
        return button1.equals(object);
    }

    public boolean isButton2(OmnibusModelInstance object) {
        return button2.equals(object);
    }

    public boolean isButton3(OmnibusModelInstance object) {
        return button3.equals(object);
    }
}
