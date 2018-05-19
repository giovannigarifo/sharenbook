package it.polito.mad.sharenbook.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import it.polito.mad.sharenbook.R;

public class PermissionsHandler {

    private static final int LENGTH_VERY_LONG = 5000;

    private static String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static void check(Activity activity, GrantedPermissionListener grantedPermissionListener) {
        View contentView = activity.findViewById(android.R.id.content);

        MultiplePermissionsListener multiplePermissionsListener = new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.isAnyPermissionPermanentlyDenied()) {
                    Snackbar.make(contentView, R.string.permissions_snackbar, LENGTH_VERY_LONG)
                            .setAction(R.string.settings, v -> {
                                Context context = v.getContext();
                                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + context.getPackageName()));
                                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(myAppSettings);
                            })
                            .show();
                } else if (report.areAllPermissionsGranted()) {
                    if (grantedPermissionListener != null)
                        grantedPermissionListener.onAllGranted();
                } else {
                    showPermissionsDeniedDialog(activity);
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                showPermissionsRationale(activity, token);
            }
        };

        Dexter.withActivity(activity)
                .withPermissions(permissions)
                .withListener(multiplePermissionsListener)
                .check();
    }

    public static void check(Activity activity) {
        check(activity, null);
    }

    private static void showPermissionsRationale(Activity activity, final PermissionToken token) {
        new AlertDialog.Builder(activity).setTitle(R.string.permissions_rationale_title)
                .setMessage(R.string.permissions_rationale_message)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    token.cancelPermissionRequest();
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    token.continuePermissionRequest();
                })
                .setOnDismissListener(dialog -> token.cancelPermissionRequest())
                .show();
    }

    private static void showPermissionsDeniedDialog(Activity activity) {
        new AlertDialog.Builder(activity).setTitle(R.string.permissions_denied_title)
                .setMessage(R.string.permissions_denied_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public interface GrantedPermissionListener {
        /**
         * Callback method that is passed from caller
         */
        void onAllGranted();
    }
}
