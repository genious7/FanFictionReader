package com.spicymango.fanfictionreader.menu.mainmenu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.ListFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.AccountActivity;
import com.spicymango.fanfictionreader.menu.librarymenu.LibraryMenuActivity;
import com.spicymango.fanfictionreader.activity.SearchAuthorActivity;
import com.spicymango.fanfictionreader.activity.SearchCommunityActivity;
import com.spicymango.fanfictionreader.activity.SearchStoryActivity;
import com.spicymango.fanfictionreader.activity.Site;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.AboutDialog;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuActivity;
import com.spicymango.fanfictionreader.menu.storymenu.StoryMenuActivity;
import com.spicymango.fanfictionreader.util.Sites;

public final class FanFictionMain extends ListFragment implements OnClickListener{

	private final static MainMenuItem menuItems[] = new MainMenuItem[] {
			new MainMenuItem(R.drawable.ic_menu_library, R.string.menu_button_my_library, 0),
			new MainMenuItem(R.drawable.ic_menu_resume, R.string.menu_button_resume, 7),
			new MainMenuItem(R.drawable.ic_menu_favorite, R.string.menu_button_favs_folls, 8),
			new MainMenuItem(R.drawable.ic_menu_browse, R.string.menu_button_browse_stories, 1),
			new MainMenuItem(R.drawable.ic_menu_just_in, R.string.menu_button_just_in, 2),
			new MainMenuItem(R.drawable.ic_menu_search, R.string.menu_button_search, 3),
			new MainMenuItem(R.drawable.ic_menu_community, R.string.menu_button_communities, 4),
			new MainMenuItem(R.drawable.ic_menu_settings, R.string.menu_button_settings, 5),
			new MainMenuItem(R.drawable.ic_menu_about, R.string.menu_button_about, 6) };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainMenuAdapter adapter = new MainMenuAdapter(getActivity(), menuItems);
		setListAdapter(adapter);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(R.string.site_fanfiction);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent i;
		switch ((int)id) {
		case 0:
			i = new Intent(getActivity(), LibraryMenuActivity.class);
			startActivity(i);
			break;
		case 1:// Case Browse Stories
			i = new Intent(getActivity(), BrowseMenuActivity.class);
			i.setData(Sites.FANFICTION.BASE_URI);
			startActivity(i);
			break;
		case 2: {// Case Just In
			Uri target = Uri.withAppendedPath(Sites.FANFICTION.BASE_URI, "j/");
			i = new Intent(getActivity(), StoryMenuActivity.class);
			i.setData(target);
			startActivity(i);
			break;
		}
		case 3:// Search
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			String[] label = getResources().getStringArray(R.array.menu_search_by);
			builder.setItems(label, this);
			builder.create();
			builder.show();
			break;
		case 4:// Communities
			Uri target = Uri.withAppendedPath(Sites.FANFICTION.BASE_URI, "communities/");
			i = new Intent(getActivity(), BrowseMenuActivity.class);
			i.setData(target);
			startActivity(i);
			break;
		case 5:
			i = new Intent(getActivity(), Settings.class);
			getActivity().startActivityForResult(i, MainActivity.INTENT_SETTINGS);
			break;
		case 6:
			DialogFragment diag = new AboutDialog();
			diag.show(getFragmentManager(), null);
			break;
		case 7:
			//SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
			//final String prefKey = Sites.FANFICTION.name() + " Resume ID";
			SharedPreferences preference = getActivity().getSharedPreferences(MainActivity.EXTRA_PREF,MainActivity.MODE_PRIVATE);
			long resumeId = preference.getLong(MainActivity.EXTRA_RESUME_ID, -1);
			if (resumeId == -1) {
				Toast toast = Toast.makeText(getActivity(), R.string.menu_toast_resume, Toast.LENGTH_SHORT);
				toast.show();
			}else{
				StoryDisplayActivity.openStory(getActivity(), resumeId, Site.FANFICTION, false);
			}
			break;
		case 8:
			i = new Intent(getActivity(), AccountActivity.class);
			startActivity(i);
		default:
			break;
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		Intent i;
		switch (which) {
		case 0:
			i = new Intent(getActivity(), SearchStoryActivity.class);
			break;
		case 1:
			i = new Intent(getActivity(), SearchAuthorActivity.class);
			break;
		case 2:
			i = new Intent(getActivity(), SearchCommunityActivity.class);
			break;
		default:
			return;
		}
		startActivity(i);			
	}	
}