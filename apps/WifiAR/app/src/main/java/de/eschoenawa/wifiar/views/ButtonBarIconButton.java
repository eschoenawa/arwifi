package de.eschoenawa.wifiar.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import de.eschoenawa.wifiar.R;

/**
 * This compound view is a button with only an icon on it for use in Button Bars.
 *
 * @author Emil Schoenawa
 */
public class ButtonBarIconButton extends LinearLayout {
    private final ImageView imageView;

    public ButtonBarIconButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.button_bar_icon_button, this);
        TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.ButtonBarIconButton);
        Drawable image = styledAttributes.getDrawable(R.styleable.ButtonBarIconButton_image);
        boolean enabled = styledAttributes.getBoolean(R.styleable.ButtonBarIconButton_enabled, true);
        styledAttributes.recycle();
        imageView = findViewById(R.id.imgBtnBarBtn);
        imageView.setBackground(image);
        this.setBackground(null);
        this.setEnabled(enabled);
        // Make child not clickable allowing this LinearLayout to handle clicks
        getChildAt(0).setClickable(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        imageView.setEnabled(enabled);
    }

    public void setImage(Drawable drawable) {
        imageView.setBackground(drawable);
    }
}
