/**
 * 
 */
package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.gmail.michaelchentejada.fanfictionreader.util.currentState;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Provides a basic template to use as a menu throughout the application
 * @author Michael Chen
 */
public abstract class Menu<T> extends Activity{
	
	/**
	 * Saved Instance State Strings
	 */
	private final static String ON_SAVE_LIST = "List";
	private final static String ON_SAVE_CUR_PAGE = "Current Page";
	private final static String ON_SAVE_TOT_PAGE = "Total Pages";
	
	/**
	 * Key for the value of the activity
	 */
	public final static String EXTRA_ACTIVITY_STATE = "com.gmail.michaelchentejada.fanfictionreader.activityState";
	
	/** Current application pathway*/
	protected currentState activityState;
	
	/** Links the list to the listView.	**/
	private BaseAdapter adapter;
	
	/** Represents the current context **/
	protected Context context;
	
	/** Contains the items to be listed*/
	protected ArrayList<T> list = new ArrayList<T>();
	
	/** Listens for click events on the ListView **/
	private final OnItemClickListener listListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			listListener((int) id);
		}
	};
	
	/** Listens for long click events on the ListView*/	
	private final OnItemLongClickListener listLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int position, long id) {
			listLongClickListener((int) id);
			return false;
		}
	};

	/** Adds the next page to the ListView*/
	private final OnClickListener addPageListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			addToList();
		}
	};
	
	/** Fetches the data to be displayed */
	private Parse asyncTask = new Parse();
	
	/**Contains the page currently loaded*/
	protected int currentPage = 1;
	
	/** Contains the total number of pages*/
	private int totalPages = 0;
	
	/** The button used for loading additional pages.*/
	private Button loadPageButton;
	
	/**The URL of the web page*/
	private String url;
	
	/**
	 * Adds the header view to the ListView. Called during onCreate. To implement:
	 * <pre><code> View header = (View)getLayoutInflater().inflate( <b>id</b>, null);
	 * listview.addHeaderView(header);	</code></pre>
	 * @param listview The ListView to add the header to. Supplied automatically.
	 */
	protected abstract void addHeaderView (ListView listview);
	
	/** Listens for click events on the ListView **/
	protected abstract void listListener(int id);
	
	/** Listens for long click events on the ListView **/
	protected  void listLongClickListener(int id){
	};
	
	/**
	 * Parses the web page. Fetching filters must also be done here.
	 * @param document The web page to be parsed
	 * @return the items in a list format.
	 */
	protected abstract ArrayList<T> parsePage (Document document);
	
	/** Find the number of pages that the page has. Override to manually set the number of pages.
	 * @param document The parsed web page
	 * @return The total number of pages*/
	protected  int setNumberOfPages (Document document){
		if (activityState == currentState.JUSTIN)
			return 1;
		return UtilParse.getpageNumber(document);
	}
	
	/**
	 * Performs any sorting that needs to be done to the data.
	 * @param list The list to be sorted
	 */
	protected void refreshList (ArrayList<T> list){
		adapter.notifyDataSetChanged();							//Update the listView
	};
	
	/**
	 * Adds the filter and page count to the url
	 * @param baseUrl The url the activity received as an intent
	 * @return The url to be parsed
	 */
	protected abstract String formatUrl (String baseUrl); 
	
	/**
	 * Creates a new adapter
	 * @return the adapter
	 */
	protected abstract BaseAdapter setAdapter();
	
	/**
	 * Adds a new page to the existing list
	 */
	protected final void addToList() {
		currentPage++;
		asyncTask.cancel(true);
		asyncTask = new Parse();
		asyncTask.execute();
	}
	
	/**
	 * Clears and re-populates the list
	 */
	protected final void setList(){
		list.clear();
		totalPages = 0;
		currentPage = 1;
		asyncTask.cancel(true);
		asyncTask = new Parse();
		asyncTask.execute();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);					//Set the layout to a simple listView
		context = this;													//Set the context to the activity context.
		
		ListView listview = (ListView)findViewById(R.id.list);	//Find the menu's ListView
		View footer = (View)getLayoutInflater().inflate(R.layout.story_menu_footer, null);
		listview.addFooterView(footer);									//Adds the footer
		addHeaderView(listview);										//Adds the header
		listview.setOnItemClickListener(listListener);	
		listview.setOnItemLongClickListener(listLongClickListener);
		adapter = setAdapter();
		listview.setAdapter(adapter);
		
		loadPageButton = (Button)findViewById(R.id.story_load_pages);	//Initialize loadPageButton
		loadPageButton.setOnClickListener(addPageListener);				//Set onClickListener
		
		url = getIntent().getDataString();
		activityState = (currentState) getIntent().getSerializableExtra(EXTRA_ACTIVITY_STATE);
		//TODO: parse the URL to find the current state
		
		if (savedInstanceState == null) {
			
		}else {
			list.clear();
			list.addAll((Collection<? extends T>) savedInstanceState.getSerializable(ON_SAVE_LIST));
			refreshList(list);
			
			currentPage = savedInstanceState.getInt(ON_SAVE_CUR_PAGE);
			totalPages = savedInstanceState.getInt(ON_SAVE_TOT_PAGE);
			
			loadPageButton.setText(String.format(getResources().
					getString(R.string.story_add_page_button)
					, currentPage, totalPages));					//Add the "Page i of n" text
			
			loadPageButton.setVisibility(
					(currentPage >= totalPages) ? 
							View.GONE : View.VISIBLE);				//Set the Button visibility based on validity.
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(ON_SAVE_LIST, list);
		outState.putInt(ON_SAVE_CUR_PAGE, currentPage);
		outState.putInt(ON_SAVE_TOT_PAGE, totalPages);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy() {
		asyncTask.cancel(true);
		super.onDestroy();
	}
	
	/**
	 * The AsyncTask that loads the information to be displayed.
	 * @author Michael Chen
	 */
	private class Parse extends AsyncTask<Void, Void, ArrayList<T>>{
		/** The progress dialog displayed while loading*/
		private ProgressDialog progressDialog; 							//Progress dialogue for the async task.
		
		@Override
		protected void onPreExecute() {			
			progressDialog = new ProgressDialog(context);
			progressDialog.setTitle("");
			progressDialog.setMessage(getResources().getString(R.string.dialog_loading));
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					Parse.this.cancel(true);						//Cancel the task, not interested
					finish();											//Close the progress dialog
				}
			});
			progressDialog.show();
			super.onPreExecute();
		}

		@Override
		protected ArrayList<T> doInBackground(
				Void... params) {
			try {
				Document document = Jsoup.connect(formatUrl(url)).get();
				
				if (totalPages == 0)
					totalPages = setNumberOfPages(document);
				
				return parsePage(document);
			} catch (IOException e) {
				return null;
			}	
		}
	
		@Override
		protected void onPostExecute(ArrayList<T> result) {
			progressDialog.dismiss();
			
			if (result != null) {	
				list.addAll(result);									//Add the results to the list
				refreshList(list);											//Sorts the list and updates the ListView
				
				loadPageButton.setText(String.format(getResources().
						getString(R.string.story_add_page_button)
						, currentPage, totalPages));					//Add the "Page i of n" text
				
				loadPageButton.setVisibility(
						(currentPage >= totalPages) ? 
								View.GONE : View.VISIBLE);				//Set the Button visibility based on validity.

			}else{
				Toast toast = Toast.makeText(context,
						getString(R.string.dialog_internet), 
						Toast.LENGTH_SHORT);							//Internet access error toast
				
				toast.show();
				if (currentPage == 1) {
					finish();											//Close activity if no information has been displayed at all
				}else{
					currentPage--;										//Decrement currentPage so that next loadPage works.
				}
			}
			super.onPostExecute(result);
		}
	}
	
	/**
	 * Various miscellaneous parsing.
	 * @author Michael Chen
	 *
	 */
	static private class UtilParse{
		
		private static final  Pattern pattern = Pattern.compile(
				"(?:&p=)(\\d{1,4}+)"//Normal
				+ "|(?:communit[^/]*+/(?:[^/]*+/){4})(\\d{1,4}+)"//Communities
				+ "|(?:&ppage=)(\\d{1,4}+)");//Search
		
		/**
		 * Gets the number of pages in the document
		 * @param document The parsed document
		 * @return The number of pages in the document
		 */
		public final static int getpageNumber(Document document){
			Elements elements = document.select("div#content a:matchesOwn(\\A(?i)last\\Z)");
			if (elements.isEmpty()){
				if (document.select("div#content a:matchesOwn(\\A(?i)next\\Z)").isEmpty())
					return 1;
				return 2;
			}
			return getpageNumber(elements.first().attr("href"));
			
		}
	
		/**
		 * Gets the page number in the url
		 * @param url The url to parse
		 * @return The current page
		 */
		private final static int getpageNumber(String url){
			Matcher matcher = pattern.matcher(url);
			matcher.find();
			for (int i = 1; i < matcher.groupCount(); i++) {
				if (matcher.group(i) != null)
					return Integer.valueOf(matcher.group(i));
			}
			return 1;
		}
	}
}
