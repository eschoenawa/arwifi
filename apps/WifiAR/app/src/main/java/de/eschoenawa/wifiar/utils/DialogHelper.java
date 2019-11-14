package de.eschoenawa.wifiar.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import de.eschoenawa.wifiar.R;

/**
 * This utility-class provides methods to show dialogs for specific situations.
 *
 * @author Emil Schoenawa
 */
public class DialogHelper {
    public static void showResetWarning(Context context, DialogInterface.OnClickListener positiveClick) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_reset)
                .setMessage(R.string.dialog_message_reset)
                .setPositiveButton(R.string.yes, positiveClick)
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public static void showTooMuchMovementDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_too_much_movement)
                .setMessage(R.string.dialog_message_too_much_movement)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    public static void showLocationRationaleDialog(Context context, DialogInterface.OnClickListener positiveClick, DialogInterface.OnClickListener negativeClick) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_location_rationale)
                .setMessage(R.string.dialog_message_location_rationale)
                .setPositiveButton(R.string.ok, positiveClick)
                .setNegativeButton(R.string.cancel, negativeClick)
                .show();
    }

    public static void showLocationPermanentlyDeniedDialog(Context context, DialogInterface.OnClickListener openSettingsClick, DialogInterface.OnClickListener negativeClick) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_location_rationale)
                .setMessage(R.string.dialog_message_location_rationale)
                .setPositiveButton(R.string.settings, openSettingsClick)
                .setNegativeButton(R.string.cancel, negativeClick)
                .show();
    }
}
