package de.eschoenawa.wifiar.utils;

import android.graphics.Bitmap;

public class ImageHolder {

    private Bitmap image;

    /**
     * Helper-class to make ImageHolder a Bill Pugh Singleton.
     */
    private static class InstanceHolder {
        private static final ImageHolder INSTANCE = new ImageHolder();
    }

    public static ImageHolder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ImageHolder() {

    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public void clear() {
        this.image = null;
    }
}
