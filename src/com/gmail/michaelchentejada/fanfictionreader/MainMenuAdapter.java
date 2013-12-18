/**
 * Based on http://www.ezzylearning.com/tutorial.aspx?tid=1763429&q=customizing-android-listview-items-with-custom-arrayadapter
 * @author Michael Chen
 */
package com.gmail.michaelchentejada.fanfictionreader;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The custom menu adapter for the main menu. Contains only an image and a single text.
 * @author Michael Chen
 */
public class MainMenuAdapter extends ArrayAdapter<MenuItem> {
	Context context;
	int layoutResourceId;
	MenuItem data[]=null;
	
	/**
	 * Initializes the adapter
	 * @param context The current context
	 * @param layoutResourceId The resource ID for a layout file
	 * @param data The objects to represent in the list view
	 */
	public MainMenuAdapter(Context context, int layoutResourceId, MenuItem[] data) {
		super(context, layoutResourceId, data);
		
		this.context=context;
		this.layoutResourceId=layoutResourceId;
		this.data=data;
	}
	
	@Override 
	public View getView(int position, View convertView, ViewGroup parent){
		View row = convertView;
		MenuItemHolder holder = null;
		if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
           
            holder = new MenuItemHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.list_item_icon);
            holder.txtTitle = (TextView)row.findViewById(R.id.list_item_title);
           
            row.setTag(holder);
        }
        else
        {
            holder = (MenuItemHolder)row.getTag();
        }
       
        MenuItem menuRow = data[position];
        holder.txtTitle.setText(menuRow.title);
        holder.imgIcon.setImageResource(menuRow.icon);
       
        return row;
    }
   
	/**
	 * A cache of the ImageView and the TextView. Provides a speed improvement.
	 * @author Michael Chen
	 */
	static class MenuItemHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
    }
}
