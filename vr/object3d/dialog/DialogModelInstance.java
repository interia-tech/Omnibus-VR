package pl.interia.omnibus.vr.object3d.dialog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextPaint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import pl.interia.omnibus.vr.object3d.ButtonModelInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelGroupInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.utils.Align;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;
import pl.interia.omnibus.vr.utils.TextureUtils;
import timber.log.Timber;

import static pl.interia.omnibus.vr.utils.Align.CENTER;
import static pl.interia.omnibus.vr.utils.Align.START;

public class DialogModelInstance extends OmnibusModelGroupInstance {

    public static final String MODEL_FILENAME = "3d/objects/dialog.g3db";
    private static final String TEXTURE_FILENAME = "3d/objects/dialog.png";
    public static final float PROPORTION = 0.571f; //height/width
    public static final int TEXTURE_SIZE = 1024;
    private static final int TEXT_PADDING = 30;
    private ButtonModelInstance button1;
    private ButtonModelInstance button2;
    private Texture texture;
    private final Rect textRect = new Rect();
    private final Bitmap bitmap;
    private final Canvas canvas;
    private Material material;
    @Getter
    private Type currentType;
    @Getter
    private boolean isHiding = false;
    @Getter
    private boolean isVisible = false;

    public enum Type{
        IO_EXCEPTION("Wystąpił błąd sieciowy", ButtonModelInstance.Type.RETRY, ButtonModelInstance.Type.EXIT, Color.BLACK, START,
                Color.WHITE, 60),
        DISCONNECTED("Zostałeś rozłączony, sprawdź połączenie z internetem", ButtonModelInstance.Type.EXIT, null, Color.BLACK, CENTER,
                Color.WHITE, 60),
        LWS_PARTNER_LEAVE("Partner opuścił wspólną naukę", ButtonModelInstance.Type.EXIT, null, Color.BLACK, CENTER,
                Color.WHITE, 60);

        private final String text;
        private final ButtonModelInstance.Type bType1;
        private final ButtonModelInstance.Type bType2;
        private final int textColor;
        private final int backgroundColor;
        private final float textSize;
        private final Align textAlign;

        Type(String text, ButtonModelInstance.Type bType1, ButtonModelInstance.Type bType2, int textColor, Align textAlign, int backgroundColor, float textSize) {
            this.text = text;
            this.bType1 = bType1;
            this.bType2 = bType2;
            this.textColor = textColor;
            this.backgroundColor = backgroundColor;
            this.textSize = textSize;
            this.textAlign = textAlign;
        }
    }

    public DialogModelInstance(AssetManager assets) {
        super(MODEL_FILENAME, assets);
        button1 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        button2 = (ButtonModelInstance)addChild(new ButtonModelInstance(assets));
        assets.load(TEXTURE_FILENAME, Pixmap.class);
        int width = TEXTURE_SIZE;
        int height = Math.round(TEXTURE_SIZE * PROPORTION);
        textRect.set(TEXT_PADDING, TEXT_PADDING, width - TEXT_PADDING, height - TEXT_PADDING);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        Pixmap p = assets.get(TEXTURE_FILENAME, Pixmap.class);
        texture = new Texture(p);
        assets.unload(TEXTURE_FILENAME);
        material = getMaterial("DialogMaterial");
        material.set(TextureAttribute.createDiffuse(texture));
    }

    private void prepare(Type type){
        prepareButtons(type);
        if(!type.equals(currentType)) {
            currentType = type;
            setButtonsPosition(type);
            canvas.drawColor(type.backgroundColor);
            TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialRegular(), type.textColor, type.textSize);
            DrawUtils.drawText(type.text, textRect, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, type.textAlign);
            TextureUtils.setAsTexture(texture, material, bitmap);
        }
    }

    private void setButtonsPosition(Type type){
        if(type.bType2 == null){
            button1.setToTranslation(0, 1.25f, -1.1f);
        }else if(type.bType1 != null){
            button1.setToTranslation(-0.3f, 1.25f, -1.1f);
            button2.setToTranslation(0.3f, 1.25f, -1.1f);
        }
    }

    private void prepareButtons(Type type){
        if(type.bType1 != null){
            button1.prepare(type.bType1);
        }
        if(type.bType2 != null){
            button2.prepare(type.bType2);
        }
    }


    public void hide(Runnable runAfterEnd){
        Timber.d("DIALOG HIDE");
        isHiding = true;
        isVisible = true;
        setButtonsPosition(currentType);
        runAnimation("Dialog|hide", () ->{
            isHiding = false;
            isVisible = false;
            if(runAfterEnd != null) {
                runAfterEnd.run();
            }
        });
        if(currentType.bType1!= null) {
            button1.hide("Button|dialog_hide");
        }
        if(currentType.bType2 != null){
            button2.hide("Button|dialog_hide");
        }
    }

    public void show(Type type){
        show(type, null);
    }

    public void show(Type type, Runnable runAfterEnd){
        Timber.d("DIALOG SHOW");
        isVisible = true;
        Schedulers.io().scheduleDirect(() -> {
            prepare(type);
            Gdx.app.postRunnable(() -> {
                runAnimation("Dialog|show", runAfterEnd);
                if(type.bType1!= null) {
                    button1.show("Button|dialog_show");
                }
                if(type.bType2 != null){
                    button2.show("Button|dialog_show");
                }
            });
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
    }

    public boolean isButton1(OmnibusModelInstance object) {
        return button1.equals(object);
    }

    public boolean isButton2(OmnibusModelInstance object) {
        return button2.equals(object);
    }
}
