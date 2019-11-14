package de.eschoenawa.wifiar.utils;

import android.app.Activity;
import android.graphics.Color;

import de.eschoenawa.wifiar.R;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

/**
 * This class can create different Tap Target Prompts for helping new users understand the usage of
 * Wifi AR. This class is using Samuel Wall's 'Material Tap Target Prompt' library to achieve this.
 *
 * @author Emil Schoenawa
 *
 * @see <a href="https://github.com/sjwall/MaterialTapTargetPrompt">Material Tap Target Prompt library</a>
 */
public class OnboardingHelper {
    public static void newAreaPointTip(Activity activity) {
        if (!Preferences.getInstance().wasNewAreaPointTipShown() && !Preferences.getInstance().isAreaAutoDefinitionEnabled()) {
            new MaterialTapTargetPrompt.Builder(activity)
                    .setTarget(R.id.btnAddPointAndFinishArea)
                    .setPrimaryText(R.string.tip_title_new_area_point)
                    .setSecondaryText(R.string.tip_text_new_area_point)
                    .setFocalRadius(40f)
                    .setFocalColour(Color.TRANSPARENT)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
            Preferences.getInstance().setNewAreaPointTipShown(true);
        }
    }

    public static void areaDoneTip(Activity activity) {
        if (!Preferences.getInstance().wasAreaDoneTipShown() && !Preferences.getInstance().isAreaAutoDefinitionEnabled()) {
            new MaterialTapTargetPrompt.Builder(activity)
                    .setTarget(R.id.btnAddPointAndFinishArea)
                    .setPrimaryText(R.string.tip_title_area_done)
                    .setSecondaryText(R.string.tip_text_area_done)
                    .setFocalRadius(40f)
                    .setFocalColour(Color.TRANSPARENT)
                    .setCaptureTouchEventOnFocal(true)
                    .show();
            Preferences.getInstance().setAreaDoneTipShown(true);
        }
    }

    public static void measureTip(Activity activity) {
        if (!Preferences.getInstance().wasMeasureTipShown() && !Preferences.getInstance().isAreaAutoDefinitionEnabled()) {
            new MaterialTapTargetPrompt.Builder(activity)
                    .setTarget(R.id.btnMeasure)
                    .setPrimaryText(R.string.tip_title_measure)
                    .setSecondaryText(R.string.tip_text_measure)
                    .setFocalRadius(40f)
                    .setFocalColour(Color.TRANSPARENT)
                    .show();
            Preferences.getInstance().setMeasureTipShown(true);
        }
    }

    public static void generateTip(Activity activity) {
        if (!Preferences.getInstance().wasGenerateTipShown() && !Preferences.getInstance().isAreaAutoDefinitionEnabled()) {
            new MaterialTapTargetPrompt.Builder(activity)
                    .setTarget(R.id.btnCreateHeatmap)
                    .setPrimaryText(R.string.tip_title_generate)
                    .setSecondaryText(R.string.tip_text_generate)
                    .setFocalRadius(40f)
                    .setFocalColour(Color.TRANSPARENT)
                    .show();
            Preferences.getInstance().setGenerateTipShown(true);
        }
    }
}
