package it.polito.mad.sharenbook.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowCaseActivity;


public class GenericAlertDialog extends DialogFragment {

    public static GenericAlertDialog newInstance(int title) {
        GenericAlertDialog frag = new GenericAlertDialog();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");

        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setTitle(title)
                .setMessage(R.string.borrow_book_msg)
                .setPositiveButton(R.string.confirm,
                        (dialog, whichButton) -> ((ShowCaseActivity)getActivity()).doPositiveClick()
                )
                .setNegativeButton(R.string.undo,
                        (dialog, whichButton) -> ((ShowCaseActivity)getActivity()).doNegativeClick()
                )
                .create();
    }

}
