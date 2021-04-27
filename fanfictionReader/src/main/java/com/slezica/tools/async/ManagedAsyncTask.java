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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.AsyncTask;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public abstract class ManagedAsyncTask<Params, Progress, Result> {

    private TaskManagerFragment mManager;

    private InternalAsyncTask mTask;

    public ManagedAsyncTask(FragmentActivity activity) {
        this(activity, TaskManagerFragment.DEFAULT_TAG);
    }

    public ManagedAsyncTask(FragmentActivity activity, String fragmentTag) {

        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        mManager = (TaskManagerFragment) fragmentManager
                .findFragmentByTag(fragmentTag);

        if (mManager == null) {
            mManager = new TaskManagerFragment();

            fragmentManager.beginTransaction().add(mManager, fragmentTag)
                    .commit();
        }

        mTask = new InternalAsyncTask();
    }

    protected void onPreExecute() {}

    protected abstract Result doInBackground(Params... params);

    protected void onProgressUpdate(Progress... values) {}

    protected void onPostExecute(Result result) {}

    /**
     * <p>Applications should preferably override {@link #onCancelled(Object)}.
     * This method is invoked by the default implementation of
     * {@link #onCancelled(Object)}.</p>
     * 
     * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.</p>
     *
     * @see #onCancelled(Object) 
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    protected void onCancelled() {}

    public ManagedAsyncTask<Params, Progress, Result> execute(Params... params) {
        mTask.execute(params);

        return this;
    }

    public FragmentActivity getActivity() {
        return mManager.getActivity();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return mTask.cancel(mayInterruptIfRunning);
    }
    
    protected void publishProgress(Progress... values){
    	mTask.updateProgress(values);
    }

    public boolean isCancelled() {
        return mTask.isCancelled();
    }

    public Result get() throws InterruptedException, ExecutionException {
        return mTask.get();
    }

    public Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mTask.get(timeout, unit);
    }

    public AsyncTask.Status getStatus() {
        return mTask.getStatus();
    }

    protected class InternalAsyncTask extends
            AsyncTask<Params, Progress, Result> {

        @Override
        protected void onPreExecute() {
            ManagedAsyncTask.this.onPreExecute();
        }

        @Override
        protected Result doInBackground(Params... params) {
            return ManagedAsyncTask.this.doInBackground(params);
        }

        protected void onProgressUpdate(final Progress... values) {
            mManager.runWhenReady(new Runnable() {
                public void run() {
                    ManagedAsyncTask.this.onProgressUpdate(values);
                }
            });
            
            return;
        };

        protected void onPostExecute(final Result result) {
            mManager.runWhenReady(new Runnable() {
                public void run() {
                    ManagedAsyncTask.this.onPostExecute(result);
                }
            });

            return;
        }

        @Override
        protected void onCancelled() {
            mManager.runWhenReady(new Runnable() {
                public void run() {
                    ManagedAsyncTask.this.onCancelled();
                }
            });
        }
        
        protected void updateProgress(Progress... progress){
        	publishProgress(progress);
        }
    }
}
