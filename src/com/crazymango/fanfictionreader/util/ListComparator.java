package com.crazymango.fanfictionreader.util;

import java.util.Comparator;
import java.util.HashMap;

public class ListComparator implements Comparator<HashMap<String,String>> {

	boolean sortBy = true;
	public ListComparator(boolean SortBy) {
		sortBy = SortBy;
	}
	@Override
	public int compare(HashMap<String,String> arg0, HashMap<String,String> arg1) {
		if (sortBy){
			return arg0.get(Parser.TITLE).compareTo(arg1.get(Parser.TITLE));
		}else{
			return -((Integer)Integer.parseInt(arg0.get(Parser.VIEWS_INT))).compareTo(Integer.parseInt(arg1.get(Parser.VIEWS_INT)));
		}	
	}
}
