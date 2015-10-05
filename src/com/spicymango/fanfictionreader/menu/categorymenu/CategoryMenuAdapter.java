package com.spicymango.fanfictionreader.menu.categorymenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.spicymango.fanfictionreader.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class CategoryMenuAdapter extends ArrayAdapter<CategoryMenuItem> implements SectionIndexer{
	private static final CategoryMenuComparator COMPARATOR = new CategoryMenuComparator();
	
	private HashMap<Character, Integer> mIndexer;
	private Character[] mSections;
	
	public CategoryMenuAdapter(Context context, List<CategoryMenuItem> dataset) {
		super(context, R.layout.category_menu_list_item, dataset);
		mIndexer = new HashMap<>();
		mSections = new Character[]{};
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		CategoryMenuItem menuRow = getItem(position);
		CategoryItemHolder holder = null;
		
		if(convertView == null)
        {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            convertView = inflater.inflate(R.layout.category_menu_list_item, parent, false);
           
            holder = new CategoryItemHolder();
            holder.txtTitle = (TextView)convertView.findViewById(android.R.id.text1);
            holder.txtViews = (TextView)convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        }
        else
        {
            holder = (CategoryItemHolder)convertView.getTag();
        }
       
        
        holder.txtTitle.setText(menuRow.mTitle);
        holder.txtViews.setText(menuRow.mViews);
       
        return convertView;
	}
	
	private void invalidateIndex(){
		final int count = getCount();
		mIndexer.clear();
		for (int i = 0; i < count; i++) {
			CategoryMenuItem item = getItem(i);
			Character c;
			
			if (item.mSortInt == Integer.MAX_VALUE) c = ' ';
			else{
				c = Character.toUpperCase(item.mTitle.charAt(0));
				
				//Group numbers and periods together
				if (!Character.isLetter(c)) c = '#';
				// Ignore entries with accented characters; used since normalize
				// requires API 9. The collator used in sorting handles accented
				// characters correctly, though. This will also prevent a
				// section indexer with hundreds of Japanese characters.
				else if (c < 'A' || c > 'Z') continue;
			}
			
			if (!mIndexer.containsKey(c)) {
				mIndexer.put(c, i);
			}			
		}
		ArrayList<Character> sectionList = new ArrayList<Character>(mIndexer.keySet());
		Collections.sort(sectionList);
		mSections = sectionList.toArray(new Character[0]);		
	}
	
	/**
	 * Sorts the entries that are being displayed 
	 * @param order False for alphabetical sorting, true for sorting By views
	 */
	public void sort(boolean order){
		if (order == CategoryMenuComparator.SORT_ALPHABETICAL) {
			COMPARATOR.setSortType(CategoryMenuComparator.SORT_ALPHABETICAL);
			sort(COMPARATOR);
			invalidateIndex();
		}else{
			COMPARATOR.setSortType(CategoryMenuComparator.SORT_VIEWS);
			sort(COMPARATOR);
		}		
	}
	
	/*
	 * A cache of the TextViews. Provides a speed improvement.
	 * @author Michael Chen
	 */
	static class CategoryItemHolder
    {
        TextView txtTitle;
        TextView txtViews;
    }

	@Override
	public Object[] getSections() {
		return mSections;
	}

	@Override
	public int getPositionForSection(int sectionIndex) {
		int position = mIndexer.get(mSections[sectionIndex]);
		return position < getCount() ? position : getCount() - 1;
	}

	@Override
	public int getSectionForPosition(int position) {
		int i;
		for (i = mSections.length - 1; i >= 0; i--){
			if(position >= mIndexer.get(mSections[i]))
				break;
		}
		return i < 0 ? 0 : i ;
	}

}
