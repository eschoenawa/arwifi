package de.eschoenawa.wifiar.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Display;

import com.google.ar.sceneform.math.Vector3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.eschoenawa.wifiar.common.Constants;

import static de.eschoenawa.wifiar.common.Constants.FUZZY_TARGET_DETECTION_DISTANCE;

public class Utils {
    private static final String TAG = "UTILS";

    public static float[] getScreenSize(Activity activity) {
        float[] result = new float[2];
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        result[0] = size.x;
        result[1] = size.y;
        return result;
    }

    public static boolean positionsCloseEnough(Vector3 a, Vector3 b) {
        if (a == null || b == null) {
            return false;
        }
        float distance = new Vector3(a.x - b.x, 0, a.z - b.z).length();
        return distance <= FUZZY_TARGET_DETECTION_DISTANCE;
    }

    public static Uri saveBitmap(Bitmap bitmap, String filename) {
        File file = new File(Environment.getExternalStorageDirectory() + Constants.BITMAP_PATH);
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "Cannot save bitmap, file can't be created!");
            return null;
        }
        file = new File(Environment.getExternalStorageDirectory() + Constants.BITMAP_PATH + filename);
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
        } catch (IOException e) {
            Log.e(TAG, "Cannot save bitmap!", e);
            return null;
        }
        return Uri.fromFile(file);
    }
}
