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

package com.slezica.tools.async;

import java.util.LinkedList;
import java.util.List;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

public class TaskManagerFragment extends Fragment implements TaskManager {

    public static final String DEFAULT_TAG = "TaskManagerFragment";

    protected final Object mLock = new Object();

    protected Boolean mReady = false;
    protected List<Runnable> mPendingCallbacks = new LinkedList<Runnable>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        synchronized (mLock) {
            mReady = false;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        synchronized (mLock) {
            mReady = true;

            int pendingCallbacks = mPendingCallbacks.size();

            while (pendingCallbacks-- > 0)
                runNow(mPendingCallbacks.remove(0));
        }
    }

    public boolean isReady() {
        synchronized (mLock) {
            return mReady;
        }
    }

    protected void setReady(boolean ready) {
        synchronized (mLock) {
            mReady = ready;
        }
    }

    protected void addPending(Runnable runnable) {
        synchronized (mLock) {
            mPendingCallbacks.add(runnable);
        }
    }

    public void runWhenReady(Runnable runnable) {
        if (isReady())
            runNow(runnable);

        else
            addPending(runnable);
    }

    protected void runNow(Runnable runnable) {
        getActivity().runOnUiThread(runnable);
    }
}
