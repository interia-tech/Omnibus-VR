package pl.interia.omnibus.vr.utils;

import android.content.Context;
import android.graphics.Typeface;

public class Fonts {
    private static Typeface ARIAL_REGULAR;
    private static Typeface ARIAL_BOLD;

    private Fonts() {}

    public static void init (Context ctx) {
        ARIAL_REGULAR = Typeface.createFromAsset(ctx.getAssets(), "3d/fonts/Arial-Regular.ttf");
        ARIAL_BOLD = Typeface.createFromAsset(ctx.getAssets(), "3d/fonts/Arial-Bold.ttf");
    }

    public static Typeface getArialRegular() {
        return ARIAL_REGULAR;
    }

    public static Typeface getArialBold() {
        return ARIAL_BOLD;
    }
}
