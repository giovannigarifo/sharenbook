package it.polito.mad.sharenbook.Utils;

import android.Manifest;
import android.app.Activity;
import android.view.View;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;

import java.util.List;

public class PermissionsHandler {

    private Activity mActivity;
    private View contentView;
    private MultiplePermissionsListener allPermissionsListener;

    private String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    public PermissionsHandler(Activity activity) {
        this.mActivity = activity;
        contentView = activity.findViewById(android.R.id.content);
        createPermissionListeners();
    }

    public void check() {
        Dexter.withActivity(mActivity)
                .withPermissions(permissions)
                .withListener(allPermissionsListener)
                .onSameThread()
                .check();
    }

    private void createPermissionListeners() {
        MultiplePermissionsListener dialogMultiplePermissionsListener =
                DialogOnAnyDeniedMultiplePermissionsListener.Builder
                        .withContext(mActivity)
                        .withTitle("Camera & audio permission")
                        .withMessage("Both camera and audio permission are needed to take pictures of your cat")
                        .withButtonText(android.R.string.ok)
                        .build();

        MultiplePermissionsListener snackbarMultiplePermissionsListener =
                SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
                        .with(contentView, "Camera and audio access is needed to take pictures of your dog")
                        .withOpenSettingsButton("Settings")
                        .build();

        MultiplePermissionsListener multiplePermissionsListener = new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted());
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

            }
        };

        allPermissionsListener = new CompositeMultiplePermissionsListener(
                dialogMultiplePermissionsListener,
                snackbarMultiplePermissionsListener
        );
    }

    /*
    private void showPermissionRationale(final PermissionToken token) {
        new AlertDialog.Builder(mActivity).setTitle(R.string.permission_rationale_title)
                .setMessage(R.string.permission_rationale_message)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        token.cancelPermissionRequest();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        token.continuePermissionRequest();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override public void onDismiss(DialogInterface dialog) {
                        token.cancelPermissionRequest();
                    }
                })
                .show();
    }
    */

}
