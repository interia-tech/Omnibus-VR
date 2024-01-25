package pl.interia.omnibus.vr.flashcard.object3d;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextPaint;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;

import pl.interia.omnibus.vr.object3d.OmnibusModelGroupInstance;
import pl.interia.omnibus.vr.utils.DrawUtils;
import pl.interia.omnibus.vr.utils.Fonts;
import pl.interia.omnibus.vr.utils.TextureUtils;

import static pl.interia.omnibus.vr.utils.Align.CENTER;

public class LWSAvatarModelInstance extends OmnibusModelGroupInstance {
    private static final String MODEL_FILENAME = "3d/objects/lwsAvatar.g3db";
    private static final float NAME_SIDE_PROPORTION = 0.33f; //height/width
    private static final int NAME_SIDE_PADDING = 10;
    private static final int NAME_TEXTURE_SIZE = 256;
    private static final int AVATAR_TEXTURE_SIZE = 256;
    private Texture textureName;
    private Texture textureAvatar;
    private final Rect sideRec = new Rect();
    private Material materialName;
    private Material materialAvatar;

    public LWSAvatarModelInstance(AssetManager assets) {
        super(MODEL_FILENAME, assets);
        int nameHeight = Math.round(NAME_TEXTURE_SIZE * NAME_SIDE_PROPORTION);
        sideRec.set(0, 0, NAME_TEXTURE_SIZE - NAME_SIDE_PADDING * 2, nameHeight - NAME_SIDE_PADDING * 2);
        setShouldBeRendered(false);
        textureName = new Texture(NAME_TEXTURE_SIZE, NAME_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        textureAvatar = new Texture(AVATAR_TEXTURE_SIZE, AVATAR_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
    }

    @Override
    public void notifyDoneLoading(AssetManager assets) {
        super.notifyDoneLoading(assets);
        materialName = getMaterial("nameMaterial");
        materialAvatar = getMaterial("avatarMaterial");
    }

    private void drawAvatar(Bitmap avatar){
        Bitmap bitmap = Bitmap.createBitmap(AVATAR_TEXTURE_SIZE, AVATAR_TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = DrawUtils.buildPaint();
        Rect out = new Rect(0, 0, AVATAR_TEXTURE_SIZE, AVATAR_TEXTURE_SIZE);
        canvas.drawBitmap(avatar, null, out, paint);
        //draw texture back and edge of avatar
        canvas.drawRect(0,0, 10, 10, DrawUtils.buildPaint(Color.WHITE));
        TextureUtils.setAsTexture(textureAvatar, materialAvatar, bitmap);
    }

    private void drawName(String name){
        Bitmap bitmap = Bitmap.createBitmap(NAME_TEXTURE_SIZE, NAME_TEXTURE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        TextPaint textPaint = DrawUtils.buildTextPaint(Fonts.getArialBold(), Color.BLACK, 25);
        Rect s = new Rect(sideRec);
        s.offset(0, 10);
        DrawUtils.drawText(name, s, canvas, textPaint, Layout.Alignment.ALIGN_CENTER, CENTER);
        TextureUtils.setAsTexture(textureName, materialName, bitmap);
    }

    public void prepare(Bitmap avatar, String name) {
        drawAvatar(avatar);
        drawName(name);
    }

    public void show(){
        setToTranslation(-0.63f, 1.75f, -1f);
        setShouldBeRendered(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        textureName.dispose();
        textureAvatar.dispose();
    }
}
