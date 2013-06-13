package org.anonymous.dobrochan.sqlite;

import org.anonymous.dobrochan.ApiWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class ThreadsCacheOpenHelper extends SQLiteOpenHelper implements
		IThreadsCache {
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "threads";
	private static final String THREAD_COLUMN = "board_and_display_id";
	private static final String DATA_COLUMN = "dump";
	private static final String[] GET_COLUMNS = { DATA_COLUMN };
	private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME
			+ " (" + THREAD_COLUMN + " TEXT UNIQUE, " + DATA_COLUMN + " TEXT);";
	private SQLiteDatabase db;

	public ThreadsCacheOpenHelper(Context context) {
		super(context, (Build.VERSION.SDK_INT > 7 ? ApiWrapper
				.getExternalCacheDir(context).getAbsolutePath() + "/" : "")
				+ "threads_cache.sqlite", null, DATABASE_VERSION);
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
	 * org.anonymous.dobrochan.sqlite.IThreadsCache#addThread(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void addThread(String adress, String data) {
		ContentValues cv = new ContentValues();
		cv.put(THREAD_COLUMN, adress);
		cv.put(DATA_COLUMN, data);
		db.replace(TABLE_NAME, null, cv);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.anonymous.dobrochan.sqlite.IThreadsCache#getThreadData(java.lang.
	 * String)
	 */
	@Override
	public String getThreadData(String adress) {
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
	 * org.anonymous.dobrochan.sqlite.IThreadsCache#deleteThread(java.lang.String
	 * )
	 */
	@Override
	public void deleteThread(String adress) {
		String whereArgs[] = { adress };
		db.delete(TABLE_NAME, THREAD_COLUMN + " = ?", whereArgs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.anonymous.dobrochan.sqlite.IThreadsCache#clearCache()
	 */
	@Override
	public void clearCache() {
		// db.delete(TABLE_NAME, "1", null);
		onUpgrade(db, DATABASE_VERSION, DATABASE_VERSION);
	}

	@Override
	public boolean isFake() {
		return false;
	}
}
