package com.spicymango.fanfictionreader.menu.mainmenu;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.graphics.PorterDuff;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
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
	private final int mTintColor;
	
	/**
	 * Initializes the adapter
	 * @param context The current context
	 * @param layoutResourceId The resource ID for a layout file
	 * @param data The objects to represent in the list view
	 */
	public MainMenuAdapter(Context context, MainMenuItem[] data) {
		super(context, mLayoutResourceId, data);

		//Obtain the primary text color for the menu
		TypedValue typedValue = new TypedValue();
		Theme theme = context.getTheme();
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		mTintColor = typedValue.data;
		
	}
	
	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}
	
	@Override 
	public View getView(int position, View convertView, ViewGroup parent){
		View row = convertView;
		MainMenuAdapter.MenuItemHolder holder = null;
		if(row == null)
        {
            LayoutInflater inflater = ((ActionBarActivity )getContext()).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);
           
            holder = new MenuItemHolder();
            holder.txtTitle = (TextView)row.findViewById(R.id.list_item_title);
            holder.imgIcon = (ImageView)row.findViewById(R.id.list_item_icon);

            //Tint icon according to theme
            holder.imgIcon.setColorFilter(mTintColor, PorterDuff.Mode.SRC_IN);
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