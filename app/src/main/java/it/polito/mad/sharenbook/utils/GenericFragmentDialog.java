package it.polito.mad.sharenbook.utils;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import it.polito.mad.sharenbook.R;

public class GenericFragmentDialog {

    private static String mTitle, mMessage;
    private static ClickListener mCallback;

    public static void show(FragmentActivity activity, String title, String message, ClickListener callback) {
        mTitle = title;
        mMessage = message;
        mCallback = callback;

        DialogFragment fragment = new AlertFragment();
        fragment.show(activity.getSupportFragmentManager(), "alertDialog");
    }

    public static class AlertFragment extends DialogFragment {

        public AlertFragment() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(R.drawable.ic_warning_black_24dp)
                    .setTitle(mTitle)
                    .setMessage(mMessage)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> mCallback.onPositiveClick())
                    .setNegativeButton(R.string.undo, (dialog, which) -> mCallback.onNegativeClick());

            return builder.create();
        }
    }

    public interface ClickListener {

        void onPositiveClick();
        void onNegativeClick();
    }
}
