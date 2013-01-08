/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.cache;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TileHitDataSource {
	private static final String TAG = "TileHitDataSource";
	private SQLiteDatabase database;
	private SQLiteHelper dbHelper;

	//	private String[] allColumns = { SQLiteHelper.COLUMN_ID,
	//			SQLiteHelper.COLUMN_COMMENT };

	public TileHitDataSource(Context context) {
		dbHelper = new SQLiteHelper(context);
	}

	public void open() throws SQLException {
		Log.d(TAG, "in dbHelper open");
		if (dbHelper == null) {
			Log.d(TAG, "dbHelper is null");
		} else {
			Log.d(TAG, "dbHelper is not null");
		}
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	//	private static TileHit cursorToTileHit(Cursor cursor) {
	//		TileHit tilehit = new TileHit();
	//		tilehit.setTileFile(cursor.getString(0));
	//		tilehit.setHit(cursor.getInt(1));
	//		return tilehit;
	//	}

	public void setTileHit(String TileName) {
		//ContentValues values = new ContentValues();
		String insert =
				"INSERT OR IGNORE INTO " + SQLiteHelper.TABLE_NAME + "(_name,hits)"
						+ " VALUES ('"
						+ TileName
						+ "', '0');";
		String update =
				"UPDATE " + SQLiteHelper.TABLE_NAME
						+ " SET hits = hits + 1 WHERE _name LIKE '"
						+ TileName + "'";
		database.execSQL(insert);
		database.execSQL(update);
		//values.put(SQLiteHelper.COLUMN_COMMENT, TileHit);
		//		long insertId = database.insert(SQLiteHelper.TABLE_NAME, null,
		//				values);
		//		Cursor cursor = database.query(SQLiteHelper.TABLE_NAME,
		//				allColumns, SQLiteHelper.COLUMN_ID + " = " + insertId, null,
		//				null, null, null);
		//		cursor.moveToFirst();
		//		TileHit th = cursorToTileHit(cursor);
		//		cursor.close();
		//		return th;
	}

	public int getHitsByTile(String Tilefile) {
		Cursor cursor = database.query(SQLiteHelper.TABLE_NAME, new String[] { "hits" }, "_name=?",
				new String[] { Tilefile }, null, null, null);
		cursor.moveToFirst();
		int hit = cursor.getInt(0);
		cursor.close();
		return hit;
	}

	public List<String> getAllTileFileUnderHits(int hit) {
		List<String> TileFiles = new ArrayList<String>();
		Cursor cursor = database.query(SQLiteHelper.TABLE_NAME, new String[] { "_name" },
				"hits<=?", new String[] { String.valueOf(hit) }, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String File = cursor.getString(0);
			TileFiles.add(File);
			cursor.moveToNext();
		}
		cursor.close();
		return TileFiles;
	}

	public void deleteTileFileUnderhits(int hit) {
		List<String> names = getAllTileFileUnderHits(hit);
		for (String name : names) {
			deleteTileFile(name);
		}
	}

	public void deleteTileFile(String name) {
		database.delete(SQLiteHelper.TABLE_NAME, SQLiteHelper.COLUMN_ID
				+ " = '" + name + "'", null);
	}
}
