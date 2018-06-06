package it.polito.mad.sharenbook.utils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class UpdatableFragmentDialog {

    private static String mTitle, mMessage;
    private static ProgressDialog progressDialog;
    private static DialogFragment fragment;
    private static boolean shouldBeClosed;

    public static void show(Activity activity, String title, String message) {

        if (!(activity instanceof FragmentActivity)) {
            Log.e("GenericFragmentDialog", "Passed activity is not instance of FragmentActivity or it's extension.");
            return;
        }

        mTitle = title;
        mMessage = message;
        shouldBeClosed = false;

        fragment = new ProgressDialogFragment();
        fragment.show(((FragmentActivity)activity).getSupportFragmentManager(), "progressDialogFragment");
    }

    public static void updateMessage(String message) {

        mMessage = message;

        if (progressDialog != null)
            progressDialog.setMessage(message);
    }

    public static void dismiss() {

        if (fragment.getFragmentManager() != null)
            fragment.dismiss();
        else
            shouldBeClosed = true;
    }

    public static class ProgressDialogFragment extends DialogFragment {

        public ProgressDialogFragment() {
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            if (shouldBeClosed) {
                this.dismiss();
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            this.setCancelable(false);

            progressDialog = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
            if (mTitle != null)
                progressDialog.setMessage(mTitle);
            if (mMessage != null)
                progressDialog.setMessage(mMessage);
            progressDialog.setCancelable(false);

            return progressDialog;
        }
    }
}
