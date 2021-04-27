package com.spicymango.fanfictionreader.menu.mainmenu;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Represents a single menu item
 * @author Michael Chen
 */
class MainMenuItem {
	public final int id, icon, title;
		
	/**
	 * Initializes a new menu item.
	 * @param icon The Id of the image to be used as the icon
	 * @param title The text to be used as the label
	 * @author Michael Chen
	 */
	public MainMenuItem(@DrawableRes int icon, @StringRes int title, int id){
		this.icon = icon;
		this.title = title;
		this.id = id;
	}
}