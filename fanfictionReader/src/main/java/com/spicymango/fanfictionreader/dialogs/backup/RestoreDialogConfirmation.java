package com.spicymango.fanfictionreader.dialogs.backup;

import com.spicymango.fanfictionreader.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;


public class RestoreDialogConfirmation extends DialogFragment implements OnClickListener {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.diag_restoring);
		builder.setMessage(R.string.diag_restore_warning);
		builder.setPositiveButton(android.R.string.ok, this);
		builder.setNegativeButton(android.R.string.cancel, this);
		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			dismiss();
			DialogFragment diag = new RestoreDialog();
			diag.show(getFragmentManager(), diag.getClass().getName());
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			dismiss();
		default:
			break;
		}
		
	}

}
