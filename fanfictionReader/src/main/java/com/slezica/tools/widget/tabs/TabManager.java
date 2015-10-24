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

package com.slezica.tools.widget.tabs;

import java.util.HashMap;
import java.util.Map;


public abstract class TabManager<TabType, ContainerType> {
    
    private int mCurrentTab;
    private Map<Integer, TabType> mTabs = new HashMap<Integer, TabType>();
    
    private ContainerType mContainer;
    
    public TabManager(ContainerType container) {
        mContainer = container;
    }
    
    public void setContainer(ContainerType container) {
        if (mContainer != null && mContainer != container)
            onContainerChanged(mContainer, container);
        
        mContainer = container;
    }
    
    public ContainerType getContainer() {
        return mContainer;
    }
    
    public void addTab(int id, TabType tabObject) {
        mTabs.put(id, tabObject);
    }

    public void switchTo(int id) {
        if (!mTabs.containsKey(id))
            throw new IllegalArgumentException("No tab with id " + id + " found.");
        
        performSwitch(id);
        mCurrentTab = id;
    }
    
    protected abstract void performSwitch(int id);
    protected abstract void onContainerChanged(ContainerType prevContainer, ContainerType newContainer);
    
    public int getCurrentTabId() {
        return mCurrentTab;
    }
    
    public TabType getCurrentTab() {
        return getTabObject(getCurrentTabId());
    }
    
    public TabType getTabObject(int id) {
        return mTabs.get(id);
    }
    
    @SuppressWarnings("unchecked")
    public TabType[] getTabs() {
        return (TabType[]) mTabs.values().toArray();
    }
}
