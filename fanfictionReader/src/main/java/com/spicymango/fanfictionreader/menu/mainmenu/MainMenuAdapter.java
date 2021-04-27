package com.spicymango.fanfictionreader.menu.mainmenu;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.spicymango.fanfictionreader.R;

/**
 * The custom menu adapter for the main menu. Contains only an image and a single text.
 * @author Michael Chen
 */
class MainMenuAdapter extends ArrayAdapter<MainMenuItem> {
	private final static int mLayoutResourceId = R.layout.main_menu_list_item;
	
	/**
	 * Initializes the adapter
	 * @param context The current context
	 * @param layoutResourceId The resource ID for a layout file
	 * @param data The objects to represent in the list view
	 */
	public MainMenuAdapter(Context context, MainMenuItem[] data) {
		super(context, mLayoutResourceId, data);
	}
	
	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}
	
	@Override 
	public View getView(int position, View convertView, ViewGroup parent){
		View row = convertView;
		MenuItemHolder holder = null;
		if(row == null)
        {
            LayoutInflater inflater = ((AppCompatActivity)getContext()).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);
           
            holder = new MenuItemHolder();
            holder.txtTitle = (TextView)row.findViewById(android.R.id.text1);
            holder.imgIcon = (ImageView)row.findViewById(android.R.id.icon);

            row.setTag(holder);
        }
        else
        {
            holder = (MainMenuAdapter.MenuItemHolder)row.getTag();
        }
       
        MainMenuItem menuRow = getItem(position);
        holder.txtTitle.setText(menuRow.title);
        holder.imgIcon.setImageResource(menuRow.icon);
        
        return row;
    }

	/**
	 * A cache of the ImageView and the TextView. Provides a speed improvement.
	 * @author Michael Chen
	 */
	private static class MenuItemHolder
    {
        private ImageView imgIcon;
        private TextView txtTitle;
    }
}