package pl.interia.omnibus.vr.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Disposable;

public class Dot extends Widget implements Disposable {

    private final Texture text;
    private static final int PADDING = 2;
    private int width;
    private int height;

    public Dot(float radius){
        int diameter = MathUtils.floor(radius * 2);
        height = width = PADDING * 2 + diameter;
        int rad = MathUtils.floor(radius);
        Pixmap pixmap = new Pixmap(width, height , Pixmap.Format.RGBA8888);
        pixmap.setFilter(Pixmap.Filter.BiLinear);
        pixmap.setColor(Color.WHITE);
        pixmap.fillCircle(rad + PADDING, rad + PADDING, rad);
        text = new Texture(pixmap);
        text.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        setSize(width, height);
    }

    @Override
    public float getPrefHeight() {
        return height;
    }

    @Override
    public float getPrefWidth() {
        return width;
    }

    @Override
    public float getMaxHeight() {
        return height;
    }

    @Override
    public float getMinHeight() {
        return height;
    }

    @Override
    public float getMaxWidth() {
        return width;
    }

    @Override
    public float getMinWidth() {
        return width;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        batch.draw(text, getX(), getY());
    }

    @Override
    public void dispose() {
        text.dispose();
    }
}
