package com.spicymango.fanfictionreader.menu.mainmenu;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuActivity;
import com.spicymango.fanfictionreader.util.Sites;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

public class FictionPressMain extends ListFragment {
	private final static MainMenuItem menuItems[] = new MainMenuItem[] {
			new MainMenuItem(R.drawable.ic_menu_browse, R.string.menu_button_browse_stories, 0),
			new MainMenuItem(R.drawable.ic_menu_community, R.string.menu_button_communities, 4),};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainMenuAdapter adapter = new MainMenuAdapter(getActivity(), menuItems);
		setListAdapter(adapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(R.string.site_fictionpress);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		switch ((int) id) {
		case 0:// Browse Stories
			Intent i = new Intent(getActivity(), BrowseMenuActivity.class);
			i.setData(Sites.FICTIONPRESS.BASE_URI);
			startActivity(i);
			break;
		case 4:// Communities
			Uri target = Uri.withAppendedPath(Sites.FICTIONPRESS.BASE_URI, "communities/");
			i = new Intent(getActivity(), BrowseMenuActivity.class);
			i.setData(target);
			startActivity(i);
			break;
		default:
			break;
		}
	}
}
