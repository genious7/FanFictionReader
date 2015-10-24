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

import java.util.List;

import android.content.Context;

public class SimpleFilterableAdapter<ObjectType> extends FilterableAdapter<ObjectType, String> {
    
    public SimpleFilterableAdapter(Context context, int resourceId,    int textResourceId, List<ObjectType> objects) {
        super(context, resourceId, textResourceId, objects);
    }

    public SimpleFilterableAdapter(Context context, int resourceId,
            List<ObjectType> objects) {
        super(context, resourceId, objects);
    }

    public SimpleFilterableAdapter(Context context, int resourceId) {
        super(context, resourceId);
    }

    public SimpleFilterableAdapter(Context context, List<ObjectType> objects) {
        super(context, objects);
    }

    public SimpleFilterableAdapter(Context context) {
        super(context);
    }

    @Override
    protected String prepareFilter(CharSequence seq) {
        return seq.toString().toLowerCase();
    }

    @Override
    protected boolean passesFilter(ObjectType object, String constraint) {
        String repr = object.toString().toLowerCase();
        
        if (repr.startsWith(constraint))
            return true;
        
        else {
            final String[] words = repr.split(" ");
            final int wordCount = words.length;
            
            for (int i = 0; i < wordCount; i++) {
                if (words[i].startsWith(constraint))
                    return true;
            }
        }
        
        return false;
    }
}
