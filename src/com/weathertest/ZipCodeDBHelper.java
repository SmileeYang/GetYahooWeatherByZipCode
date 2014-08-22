package com.weathertest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ZipCodeDBHelper extends SQLiteOpenHelper {

	private static final String TAG = "ZipCodeDBHelper";
	private final static int DBVersion = 1;
	public final static String DBName = "zipCode.db";
	SQLiteDatabase zipCodeDB = null;
	private String DB_PATH, myPath;

	public class DbTable {
		public static final String zipCode_new_tb = "zipCode_new_tb";
	}

	public class TableField {
		public static final String zip = "zip";
		public static final String state = "state";
		public static final String city = "city";
		public static final String timezone = "timezone";
		public static final String dst = "dst";
	}

	public ZipCodeDBHelper(Context context) {
		super(context, DBName, null, DBVersion);
		DB_PATH = "/data/data/" + context.getApplicationContext().getPackageName() + "/databases/";
		myPath = DB_PATH + DBName;
		Log.d(TAG, "DB_PATH="+myPath);
		checkDB(context);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	
	private void checkDB(Context context) {
		File dbFile = context.getDatabasePath(DBName);
		
		if (!dbFile.exists()) {
            try {
            	copyDatabase(context, dbFile);
                Log.d(TAG, "copyDataBase()");
            } catch (IOException e) {
                throw new RuntimeException("Error creating source database", e);
            }
        }
	}
	
	private void copyDatabase(Context context, File dbFile) throws IOException {
		Log.d(TAG, "copyDatabase()");
		File databaseDir = new File(dbFile.getParent());
		if(!databaseDir.exists()) {
			databaseDir.mkdir();
		}
		
        InputStream is = context.getAssets().open(DBName);
        OutputStream os = new FileOutputStream(dbFile);
 
        byte[] buffer = new byte[1024];
        while (is.read(buffer) > 0) {
            os.write(buffer);
        }
 
        os.flush();
        os.close();
        is.close();
    }
	
	public ArrayList<ZipCodeTimeZone> getZipData(Context context, String zipCode) {
		Log.d(TAG, "Receiving zipCode="+zipCode);
		zipCodeDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		
		if (zipCode == null) {
			return null;
		}
		
		ArrayList<ZipCodeTimeZone> resultList = new ArrayList<ZipCodeTimeZone>();
		Cursor cursor = null;
		try {
			String selection = String.format("%s=?", TableField.zip);
            String[] selectionArgs = new String[] { zipCode };
            cursor = zipCodeDB.query(DbTable.zipCode_new_tb, null, selection, selectionArgs, null, null, null);
            if (null != cursor) {
            	while (cursor.moveToNext()) {
            		ZipCodeTimeZone zctz = new ZipCodeTimeZone();
            		zctz.setZipCode(cursor.getString(cursor.getColumnIndex(TableField.zip)));
            		zctz.setCity(cursor.getString(cursor.getColumnIndex(TableField.city)));
            		zctz.setState(cursor.getString(cursor.getColumnIndex(TableField.state)));
            		zctz.setTimeZone(cursor.getString(cursor.getColumnIndex(TableField.timezone)));
            		zctz.setDst(cursor.getString(cursor.getColumnIndex(TableField.dst)));
                    Log.d(TAG, "zip=" + cursor.getString(cursor.getColumnIndex(TableField.zip))
                    		+ " city=" + cursor.getString(cursor.getColumnIndex(TableField.city))
                            + " timezone=" + cursor.getString(cursor.getColumnIndex(TableField.timezone))
                            + " dst=" + cursor.getString(cursor.getColumnIndex(TableField.dst)));
                    resultList.add(zctz);
                }
            } else {
            	Log.d(TAG, "--------No such zipCode--------");
            }
		} catch (Exception ex) {
			ex.printStackTrace();
			resultList = null;
		} finally {
			if (null != cursor) {
				cursor.close();
				cursor = null;
			}
		}

		return resultList;
	}

	public void closeDB() {
		zipCodeDB.close();
	}
}
