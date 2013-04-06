package org.anonymous.dobrochan.sqlite;

import org.anonymous.dobrochan.ApiWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class HiddenPostsOpenHelper extends SQLiteOpenHelper implements IHiddenPosts {
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "hidden";
    private static final String KEY_COLUMN = "board_and_display_id";
    private static final String DATA_COLUMN = "comment";
    private static final String[] GET_COLUMNS = { DATA_COLUMN };
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                KEY_COLUMN + " TEXT UNIQUE, " +
                DATA_COLUMN + " TEXT);";

    public HiddenPostsOpenHelper(Context context) {
        super(context,
        		(Build.VERSION.SDK_INT>7?ApiWrapper.getExternalCacheDir(context).getAbsolutePath()+
        		"/":"")+"hidden_posts.sqlite",
        		null, 
        		DATABASE_VERSION);
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
	
	/* (non-Javadoc)
	 * @see org.anonymous.dobrochan.sqlite.IHiddenPosts#hidePost(java.lang.String)
	 */
	@Override
	public void hidePost(String adress) {
		hidePost(adress, "");
	}
	
	/* (non-Javadoc)
	 * @see org.anonymous.dobrochan.sqlite.IHiddenPosts#hidePost(java.lang.String, java.lang.String)
	 */
	@Override
	public void hidePost(String adress, 
			String data) {
		SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_COLUMN, adress);
        cv.put(DATA_COLUMN, data);
        db.replace(TABLE_NAME, null, cv);
	}
	
	
	/* (non-Javadoc)
	 * @see org.anonymous.dobrochan.sqlite.IHiddenPosts#getComment(java.lang.String)
	 */
	@Override
	public String getComment(String adress) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(TABLE_NAME,
				GET_COLUMNS,
				KEY_COLUMN+" = '"+adress+"'",
				null,
				null,
				null,
				null,
				"1");
		if(!c.moveToFirst()) {
			c.close();
			return null;
		}
		String data = c.getString(0);
		c.close();
		return data;
	}
	
	/* (non-Javadoc)
	 * @see org.anonymous.dobrochan.sqlite.IHiddenPosts#unhidePost(java.lang.String)
	 */
	@Override
	public void unhidePost(String adress) {
		String whereArgs[] = {adress};
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_NAME, KEY_COLUMN + " = ?", whereArgs);
	}
	
	/* (non-Javadoc)
	 * @see org.anonymous.dobrochan.sqlite.IHiddenPosts#clearAll()
	 */
	@Override
	public void clearAll() {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_NAME, "1", null);
	}
}
