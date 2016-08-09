package com.spicymango.fanfictionreader.menu.librarymenu;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

/**
 * An empty dummy cursor. Since Loader.deliverResults needs a different reference every time, this
 * cursor can be used as a cheap object to return.
 * <p/>
 * Created by Michael Chen on 07/17/2016.
 */
class DummyCursor implements Cursor {
	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public int getPosition() {
		return 0;
	}

	@Override
	public boolean move(int i) {
		return false;
	}

	@Override
	public boolean moveToPosition(int i) {
		return false;
	}

	@Override
	public boolean moveToFirst() {
		return false;
	}

	@Override
	public boolean moveToLast() {
		return false;
	}

	@Override
	public boolean moveToNext() {
		return false;
	}

	@Override
	public boolean moveToPrevious() {
		return false;
	}

	@Override
	public boolean isFirst() {
		return false;
	}

	@Override
	public boolean isLast() {
		return false;
	}

	@Override
	public boolean isBeforeFirst() {
		return false;
	}

	@Override
	public boolean isAfterLast() {
		return false;
	}

	@Override
	public int getColumnIndex(String s) {
		return 0;
	}

	@Override
	public int getColumnIndexOrThrow(String s) throws IllegalArgumentException {
		return 0;
	}

	@Override
	public String getColumnName(int i) {
		return null;
	}

	@Override
	public String[] getColumnNames() {
		return new String[0];
	}

	@Override
	public int getColumnCount() {
		return 0;
	}

	@Override
	public byte[] getBlob(int i) {
		return new byte[0];
	}

	@Override
	public String getString(int i) {
		return null;
	}

	@Override
	public void copyStringToBuffer(int i, CharArrayBuffer charArrayBuffer) {

	}

	@Override
	public short getShort(int i) {
		return 0;
	}

	@Override
	public int getInt(int i) {
		return 0;
	}

	@Override
	public long getLong(int i) {
		return 0;
	}

	@Override
	public float getFloat(int i) {
		return 0;
	}

	@Override
	public double getDouble(int i) {
		return 0;
	}

	@Override
	public int getType(int i) {
		return 0;
	}

	@Override
	public boolean isNull(int i) {
		return false;
	}

	@Override
	public void deactivate() {

	}

	@Override
	public boolean requery() {
		return false;
	}

	@Override
	public void close() {

	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void registerContentObserver(ContentObserver contentObserver) {

	}

	@Override
	public void unregisterContentObserver(ContentObserver contentObserver) {

	}

	@Override
	public void registerDataSetObserver(DataSetObserver dataSetObserver) {

	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

	}

	@Override
	public void setNotificationUri(ContentResolver contentResolver, Uri uri) {

	}

	@Override
	public Uri getNotificationUri() {
		return null;
	}

	@Override
	public boolean getWantsAllOnMoveCalls() {
		return false;
	}

	@Override
	public void setExtras(Bundle bundle) {

	}

	@Override
	public Bundle getExtras() {
		return null;
	}

	@Override
	public Bundle respond(Bundle bundle) {
		return null;
	}
}
