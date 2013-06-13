package org.anonymous.dobrochan.sqlite;

import java.util.LinkedList;
import java.util.List;

import org.anonymous.dobrochan.ApiWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class ThreadsInfoCacheOpenHelper extends SQLiteOpenHelper implements
		IThreadsInfoCache {
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "infos";
	private static final String THREAD_COLUMN = "board_and_display_id";
	private static final String DATA_COLUMN = "dump";
	private static final String[] GET_COLUMNS = { DATA_COLUMN };
	private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME
			+ " (" + THREAD_COLUMN + " TEXT UNIQUE, " + DATA_COLUMN + " TEXT);";
	private SQLiteDatabase db;

	public ThreadsInfoCacheOpenHelper(Context context) {
		super(context, (Build.VERSION.SDK_INT > 7 ? ApiWrapper
				.getExternalCacheDir(context).getAbsolutePath() + "/" : "")
				+ "threads_info_cache.sqlite", null, DATABASE_VERSION);
		db = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.anonymous.dobrochan.sqlite.IThreadsInfoCache#addThreadInfo(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public void addThreadInfo(String adress, String data) {
		ContentValues cv = new ContentValues();
		cv.put(THREAD_COLUMN, adress);
		cv.put(DATA_COLUMN, data);
		db.replace(TABLE_NAME, null, cv);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.anonymous.dobrochan.sqlite.IThreadsInfoCache#getThreadInfo(java.lang
	 * .String)
	 */
	@Override
	public String getThreadInfo(String adress) {
		Cursor c = db.query(TABLE_NAME, GET_COLUMNS, THREAD_COLUMN + " = '"
				+ adress + "'", null, null, null, null, "1");
		if (!c.moveToFirst()) {
			c.close();
			return null;
		}
		String data = c.getString(0);
		c.close();
		return data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.anonymous.dobrochan.sqlite.IThreadsInfoCache#deleteThreadInfo(java
	 * .lang.String)
	 */
	@Override
	public void deleteThreadInfo(String adress) {
		String whereArgs[] = { adress };
		db.delete(TABLE_NAME, THREAD_COLUMN + " = ?", whereArgs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.anonymous.dobrochan.sqlite.IThreadsInfoCache#clearCache()
	 */
	@Override
	public void clearCache() {
		// db.delete(TABLE_NAME, "1", null);
		onUpgrade(db, DATABASE_VERSION, DATABASE_VERSION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.anonymous.dobrochan.sqlite.IThreadsInfoCache#getAllThreads()
	 */
	@Override
	public List<String> getAllThreads() {
		List<String> addresses = new LinkedList<String>();
		Cursor c = db.query(TABLE_NAME, GET_COLUMNS, null, null, null, null,
				null);
		while (c.moveToNext())
			addresses.add(c.getString(0));
		c.close();
		return addresses;
	}
}
