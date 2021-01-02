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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


public class FragmentTabManager extends TabManager<Fragment, Integer> {

    /* The container type we'll use is the target ViewGroup's id */
    
    private FragmentManager mManager;
    
    public FragmentTabManager(FragmentManager manager, int containerId) {
        super(containerId);
        mManager = manager;
    }
    
    @Override
    public void onContainerChanged(Integer prevContainerId, Integer newContainerId) {
        /* If we already had a visible fragment in a previous container,
         * we have to move it over to the new one.
         */
        Fragment fragment = getCurrentTab();
        
        mManager.beginTransaction()
            .remove(fragment)
            .add(newContainerId, fragment)
        .commit();
    }
    
    @Override
    public void performSwitch(int id) {
        if (id == getCurrentTabId())
            return;
            
        Fragment fragment = getTabObject(id);
        
        mManager.beginTransaction()
            .replace(getContainer(), fragment)
        .commit();
    }
}
