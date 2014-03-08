package com.gmail.michaelchentejada.fanfictionreader;

import java.io.File;
import java.util.ArrayList;

import org.jsoup.nodes.Document;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.gmail.michaelchentejada.fanfictionreader.activity.StoryDisplayActivity;
import com.gmail.michaelchentejada.fanfictionreader.util.Story;
import com.gmail.michaelchentejada.fanfictionreader.util.StoryMenuAdapter;
import com.gmail.michaelchentejada.fanfictionreader.util.databaseHelper;

public class LibraryMenu extends Menu<Story> {

	@Override
	protected void addHeaderView(ListView listview) {
		// TODO Auto-generated method stub
		
	}
	
	int id;
	@Override
	protected void listLongClickListener(int id2) {
		id = id2;
		
		final CharSequence[] items = {"Details", "Remove from library"};

	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle("");
	    builder.setItems(items, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int item) {
	           switch (item) {
			case 0:
				Intent i = new Intent(context,DetailDisplay.class);
				i.putExtra(DetailDisplay.MAP,list.get(item));
				startActivity(i);
				break;

			case 1:
				int length = list.get(id).getChapterLenght();
				for (int j = 0; j < length; j++) {
					File file = new File(getFilesDir(), list.get(id).getId() + "_" + j + ".txt");
					file.delete();
				}
				databaseHelper db = new databaseHelper(context);
				db.deleteStory(list.get(id));
				list.remove(id);
				refreshList(list);
				break;
			}
	        }
	    }).show();
		
		
	}

	@Override
	protected void listListener(int id) {
//		Intent i = new Intent(context,LibraryDisplay.class);
//		i.putExtra(LibraryDisplay.EXTRA_STORY,list.get(id));
//		startActivity(i);
		
		Intent i = new Intent (context, StoryDisplayActivity.class);
		i.setData(Uri.fromFile(new File(getFilesDir(), list.get(id).getId() + "_" + 1 + ".txt")));
		startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null){
			databaseHelper db = new databaseHelper(context);
			list.addAll(db.getAllStories());
			refreshList(list);
		}
	}

	@Override
	protected String formatUrl(String baseUrl) {
		return null;
	}

	@Override
	protected BaseAdapter setAdapter() {
		return new StoryMenuAdapter(context,R.layout.story_menu_list_item,list);
	}


	@Override
	protected ArrayList<Story> parsePage(Document document) {
		return null;
	}
	

}
