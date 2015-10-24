/*
 * The MIT License
 * Copyright (c) 2011 Santiago Lezica (slezica89@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.slezica.tools.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;


public abstract class FilterableAdapter<ObjectType, ConstraintType> extends BaseAdapter implements Filterable {

    /* The rationale behind this class' implementation can be found at:
     * http://blogactivity.wordpress.com/2011/08/28/filterable-adapter/
     */
    
    private List<ObjectType> mObjects, mDisplayedObjects;

    private int mResourceId;
    private int mTextResourceId = 0;

    private Boolean mOutOfSync = false;
    private boolean mNotifyOnChange = false;

    private Filter mFilter;
    private CharSequence mLastFilter;

    private Context mContext;
    private LayoutInflater mInflater;
    
    private final Object mFilterLock = new Object();
    
    public FilterableAdapter(Context context) {
        this(context, 0, 0, null);
    }
    
    public FilterableAdapter(Context context, int resourceId) {
        this(context, resourceId, 0, null);
    }
    
    public FilterableAdapter(Context context, List<ObjectType> objects) {
        this(context, 0, 0, objects);
    }
    
    public FilterableAdapter(Context context, int resourceId, List<ObjectType> objects) {
        this(context, resourceId, 0, objects);
    }
    
    public FilterableAdapter(Context context, int resourceId, int textResourceId, List<ObjectType> objects) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        
        mResourceId = resourceId;
        mTextResourceId = textResourceId;
        
        mObjects = (objects != null) ? objects : new ArrayList<ObjectType>();
        mDisplayedObjects = new ArrayList<ObjectType>(mObjects);
    }
    
    public Context getContext() {
        return mContext;
    }
    
    protected List<ObjectType> getObjects() {
        return mObjects;
    }
    
    protected Object getFilterLock() {
        return mFilterLock;
    }
    
    protected boolean isOutOfSync() {
        return mOutOfSync;
    }
    
    protected void setOutOfSync(boolean outOfSync) {
        mOutOfSync = outOfSync;
    }
    
    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }
    
    public void add(ObjectType object) {
        synchronized (mObjects) {
            mObjects.add(object);
        }
        
        if (mNotifyOnChange) notifyDataSetChanged();
    }
    
    public void addAll(Collection<? extends ObjectType> collection) {
        synchronized (mObjects) {
            mObjects.addAll(collection);
        }
        
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void addAll(ObjectType ... objects) {
        synchronized (mObjects) {
            for (ObjectType object : objects)
                mObjects.add(object);
        }
        
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void insert(ObjectType object, int index) {
        synchronized (mObjects) {
            mObjects.add(index, object);
        }
        
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void remove(ObjectType object) {
        synchronized (mObjects) {
            mObjects.remove(object);
        }
        
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void clear() {
        synchronized (mObjects) {
            mObjects.clear();
        }
        
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void sort(Comparator<? super ObjectType> comparator) {
        synchronized (mObjects) {
            Collections.sort(mObjects, comparator);
        }
        
        if (mNotifyOnChange) notifyDataSetChanged();
    }
    
    @Override
    public void notifyDataSetChanged() {
        boolean reapplyFilter;
        
        synchronized (mFilterLock) {
            reapplyFilter = mOutOfSync = (mLastFilter != null);
        }

        if (reapplyFilter) {
            /* It would be amazing to only apply the filter to the
             * new elements, but since the collection could have
             * suffered unknown modifications, we can't.
             */
            
            getFilter().filter(mLastFilter);
            
        } else {
            synchronized (mObjects) {
                mDisplayedObjects = new ArrayList<ObjectType>(mObjects);
            }
        }
        
        doNotifyDataSetChanged();
    }
    
    protected void doNotifyDataSetChanged() {
        super.notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return mDisplayedObjects.size();
    }
    
    @Override
    public ObjectType getItem(int position) {
        return mDisplayedObjects.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        TextView textView;

        if (convertView == null) {
            if (mResourceId == 0)
                throw new IllegalStateException("No view specified for this Adapter. Construct with resource ID, or override getView()");
            
            view = mInflater.inflate(mResourceId, parent, false);
            
        } else {
            view = convertView;
        }

        try {
            if (mTextResourceId == 0) {
                textView = (TextView) view;
            } else {
                textView = (TextView) view.findViewById(mTextResourceId);
            }
            
        } catch (ClassCastException e) {
            throw new IllegalStateException("This Adapter needs a text view. Pass proper resource IDs on construction, or override getView()");
        }

        ObjectType item = getItem(position);
        textView.setText(item.toString());

        return view;
    }
    
    @Override
    public Filter getFilter() {
        if (mFilter == null)
            mFilter = new ExtensibleFilter();
        
        return mFilter;
    }
    
    protected abstract ConstraintType prepareFilter(CharSequence seq);
    protected abstract boolean passesFilter(ObjectType object, ConstraintType constraint);
    
    protected class ExtensibleFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraintSeq) {
            ArrayList<ObjectType> filteredObjects;

            synchronized (mFilterLock) {
                if (!mOutOfSync && mLastFilter != null && mLastFilter.equals(constraintSeq))
                    return null;
                
                mOutOfSync = false;
                mLastFilter = constraintSeq;
            }
            
            synchronized (mObjects) {
                /* We'll make a copy of the list, so we can release the lock
                 * as soon as possible.partial
                 */
                filteredObjects = new ArrayList<ObjectType>(mObjects);
            }
            
            if (constraintSeq == null) /* Part of the Filter contract */
                return resultsFromList(filteredObjects);
            
            ConstraintType constraint = prepareFilter(constraintSeq);
            
            ListIterator<ObjectType> it = filteredObjects.listIterator();
            
            while (it.hasNext()) {
                ObjectType item = it.next();
                
                if (!passesFilter(item, constraint))
                    it.remove();
            }
            
            return resultsFromList(filteredObjects);
        }

        @SuppressWarnings("unchecked")
		@Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results != null) {
                mDisplayedObjects = (List<ObjectType>) results.values;
            }
            
            doNotifyDataSetChanged();
        }
        
        protected FilterResults resultsFromList(List<ObjectType> list) {
            FilterResults fr = new FilterResults();
            
            fr.values = list;
            fr.count = list.size();
            
            return fr;
        }
        
    }
}
