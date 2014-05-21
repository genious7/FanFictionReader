/**
 * 
 */
package com.crazymango.fanfictionreader.util;

/**
 * Represents a single menu item
 * @author Michael Chen
 */
public class MenuItem {
	public int icon;
	public String title;
	
	public MenuItem() {
		super();
	}
	
	/**
	 * Initializes a new menu item.
	 * @param Icon The Id of the image to be used as the icon
	 * @param Title The text to be used as the label
	 * @author Michael Chen
	 */
	public MenuItem(int Icon, String Title){
		super();
		icon = Icon;
		title = Title;
	}
}
