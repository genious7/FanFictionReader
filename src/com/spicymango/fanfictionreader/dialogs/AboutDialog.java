/**
 * 
 */
package com.spicymango.fanfictionreader.dialogs;

import com.spicymango.fanfictionreader.R;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * @author Michael Chen
 *
 */

@SuppressLint("InflateParams")
public class AboutDialog extends DialogFragment implements OnClickListener{
	
	public AboutDialog() {
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View content = inflater.inflate(R.layout.activity_about, null);
		
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setView(content);
		builder.setTitle(R.string.menu_button_about);
		
		content.findViewById(R.id.about_contact).setOnClickListener(this);

		AlertDialog diag= builder.create();
		diag.setCanceledOnTouchOutside(true);
		return diag;
	}
	
	@Override
	public void onClick(View v) {
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
				"mailto", "michaelchentejada+dev@gmail.com", null));
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "FanFiction Reader");
		startActivity(Intent.createChooser(emailIntent, "Send email..."));
	}
	
}