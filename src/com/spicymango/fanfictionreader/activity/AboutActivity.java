/**
 * 
 */
package com.spicymango.fanfictionreader.activity;

import com.spicymango.fanfictionreader.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * @author Michael Chen
 *
 */
public class AboutActivity extends Activity implements OnClickListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		findViewById(R.id.about_contact).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
				"mailto", "michaelchentejada+dev@gmail.com", null));
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "FanFiction Reader");
		startActivity(Intent.createChooser(emailIntent, "Send email..."));
	}

}
