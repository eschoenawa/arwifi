package de.eschoenawa.wifiar.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.eschoenawa.wifiar.R;
import de.eschoenawa.wifiar.utils.ImageHolder;
import de.eschoenawa.wifiar.utils.Utils;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class HeatmapImageActivity extends AppCompatActivity {

    private ImageView imgView;
    private Bitmap image;
    private Bitmap rotatedImage;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heatmap_image);
        imgView = findViewById(R.id.heatmap);

        ImageHolder imageHolder = ImageHolder.getInstance();
        rotatedImage = image = imageHolder.getImage();
        imageHolder.clear();

        imgView.setImageBitmap(image);
        imgView.setOnTouchListener((v, event) -> {
            float xOffset = imgView.getWidth() / 2;
            float yOffset = imgView.getHeight() / 2;

            float x = event.getX();
            float y = event.getY();

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float angle = (float) Math.toDegrees(Math.atan2(x - xOffset, yOffset - y));
                rotate(angle);
            }

            return true;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        HeatmapImageActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public void onClickSave(View view) {
        HeatmapImageActivityPermissionsDispatcher.saveImageWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void saveImage() {
        long timestamp = System.currentTimeMillis();
        Utils.saveBitmap(rotatedImage, "heatmap_" + timestamp + ".png");
        Toast.makeText(this, "Heatmap gespeichert!", Toast.LENGTH_SHORT).show();
        NavUtils.navigateUpFromSameTask(this);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void savePermissionDenied() {
        Toast.makeText(this, "Um das Bild zu speichern benötigt WifiAR Zugriff auf Ihren Speicher!", Toast.LENGTH_LONG).show();
    }

    private void rotate(float toDegrees) {
        if (image == null) {
            return;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(toDegrees);
        rotatedImage = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        imgView.setImageBitmap(rotatedImage);
    }
}
