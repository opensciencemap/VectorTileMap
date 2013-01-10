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

import org.oscim.core.Tile;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

public class TileHitDataSource {
	private static final String TAG = "TileHitDataSource";
	private SQLiteDatabase database;
	private SQLiteHelper dbHelper;
	public static String Lock = "dblock";

	//	private String[] allColumns = { SQLiteHelper.COLUMN_ID,
	//			SQLiteHelper.COLUMN_COMMENT };

	public TileHitDataSource(Context context) {
		dbHelper = new SQLiteHelper(context);
	}

	public void open() throws SQLException {
		//Log.d(TAG, "in dbHelper open");
		if (dbHelper == null) {
			Log.d(TAG, "dbHelper is null!");
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
		synchronized (Lock) {
			open();
			String insert =
					"INSERT OR IGNORE INTO " + SQLiteHelper.TABLE_NAME + "(_name,hits)"
							+ " VALUES ('"
							+ TileName
							+ "', '0');";
			String update =
					"UPDATE " + SQLiteHelper.TABLE_NAME
							+ " SET hits = hits + 1 WHERE _name = '"
							+ TileName + "'";
			database.execSQL(insert);
			database.execSQL(update);
			//Log.d("Cache", "setTileHit once");
			database.close();
		}
		//Log.d("Cache", "hits from " + TileName + ": " + getHitsByTile(TileName));
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

	private static final String CACHE_FILE = "%d-%d-%d.tile";

	public void setTileHit(Tile[] tiles) {

		new AsyncTask<Tile, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Tile... commits) {
				synchronized (Lock) {
					open();
					for (final Tile tile : commits) {
						System.out.println("commit " + tile);

						final String tileName = String.format(CACHE_FILE,
								Integer.valueOf(tile.zoomLevel),
								Integer.valueOf(tile.tileX),
								Integer.valueOf(tile.tileY));
						String insert =
								"INSERT OR IGNORE INTO " + SQLiteHelper.TABLE_NAME + "(_name,hits)"
										+ " VALUES ('"
										+ tileName
										+ "', '0');";
						String update =
								"UPDATE " + SQLiteHelper.TABLE_NAME
										+ " SET hits = hits + 1 WHERE _name = '"
										+ tileName + "'";
						database.execSQL(insert);
						database.execSQL(update);
					}

					database.close();
				}
				return Boolean.TRUE;
			}
		}.execute(tiles);
	}

	public int getHitsByTile(String Tilefile) {
		synchronized (Lock) {
			open();
			Cursor cursor = database.query(SQLiteHelper.TABLE_NAME, new String[] { "hits" },
					"_name=?",
					new String[] { Tilefile }, null, null, null);
			cursor.moveToFirst();
			int hit = cursor.getInt(0);
			cursor.close();
			database.close();
			return hit;
		}
	}

	public List<String> getAllTileFileUnderHits(int hit) {
		synchronized (Lock) {
			open();
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
			database.close();
			return TileFiles;
		}
	}

	public List<String> getAllTileFileAboveHits(int hit) {
		synchronized (Lock) {
			open();
			List<String> TileFiles = new ArrayList<String>();
			Cursor cursor = database.query(SQLiteHelper.TABLE_NAME, new String[] { "_name" },
					"hits>?", new String[] { String.valueOf(hit) }, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String File = cursor.getString(0);
				TileFiles.add(File);
				cursor.moveToNext();
			}
			cursor.close();
			database.close();
			return TileFiles;
		}
	}

	public int getMiddleHits() {
		synchronized (Lock) {
			open();
			Cursor c = database.rawQuery(
					"select max(hits) from " + SQLiteHelper.TABLE_NAME,
					null);
			c.moveToFirst();
			int middle = c.getInt(0) / 2;
			c.close();
			database.close();
			return middle;
		}
	}

	public void deleteTileFileUnderhits(int hit) {
		List<String> names = getAllTileFileUnderHits(hit);
		for (String name : names) {
			deleteTileFile(name);
		}
	}

	public void deleteTileFile(String name) {
		synchronized (Lock) {
			open();
			database.delete(SQLiteHelper.TABLE_NAME, SQLiteHelper.COLUMN_ID
					+ " = '" + name + "'", null);
			close();
		}
	}
}
