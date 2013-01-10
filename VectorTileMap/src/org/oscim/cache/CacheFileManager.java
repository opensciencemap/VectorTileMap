/*
 * Copyright 2013 OpenScienceMap
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.oscim.core.Tile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

@SuppressLint("DefaultLocale")
public class CacheFileManager implements CacheManager {
	private final static String TAG = CacheFileManager.class.getName();
	private long MAX_SIZE = 4000000;//10485760L/2; // 10MB

	private static final String CACHE_DIRECTORY = "/Android/data/org.oscim.app/cache/";
	private static final String CACHE_FILE = "%d-%d-%d.tile";

	final TileHitDataSource mDatasource;
	private final File mCacheDir;

	private long mCacheSize = 0;

	private ArrayList<Tile> mCommitHitList;

	public CacheFileManager(Context context) {
		//		boolean mExternalStorageAvailable = false;
		//		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		String externalStorageDirectory = Environment.getExternalStorageDirectory()
				.getAbsolutePath();
		String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY;
		//			boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(
		//					android.os.Environment.MEDIA_MOUNTED);

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mCacheDir = createDirectory(cacheDirectoryPath);
			Log.d(TAG, "SDCARD");
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mCacheDir = context.getCacheDir();
			Log.d(TAG, "SDCARD not writing!");
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mCacheDir = context.getCacheDir();
			Log.d(TAG, "Memory");
		}
		//			if (isSDPresent) {
		//				mCacheDir = createDirectory(cacheDirectoryPath);
		//				Log.d(TAG, "SDCARD");
		//			} else {
		//				mCacheDir = context.getCacheDir();
		//				Log.d(TAG, "Memery");
		//			}
		mDatasource = new TileHitDataSource(context);
		//		if (mDatasource == null) {
		//			Log.d("TileHitDataSource", "mDatasource is null");
		//		} else {
		//			Log.d("TileHitDataSource", "mDatasource is not null");
		//		}
		mDatasource.open();

		// FIXME get size from Database or read once in asyncTask!
		// also use asyncTask for limiting cache:
		// for now check only once on initialization:

		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				mCacheSize = getCacheDirSize();

				if (mCacheSize > MAX_SIZE) {
					Log.d(TAG, "MAX_SIZE: " + MAX_SIZE);
					mDatasource.deleteTileFileUnderhits(2);
				}

				return null;
			}
		}.execute();

		// todo commit on app pause/destroy
		mCommitHitList = new ArrayList<Tile>(100);

	}

	private static File createDirectory(String pathName) {
		File file = new File(pathName);
		if (!file.exists() && !file.mkdirs()) {
			throw new IllegalArgumentException("could not create directory: " + file);
		} else if (!file.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + file);
		} else if (!file.canRead()) {
			throw new IllegalArgumentException("cannot read directory: " + file);
		} else if (!file.canWrite()) {
			throw new IllegalArgumentException("cannot write directory: " + file);
		}
		return file;
	}

	// synchronized to keep state consistent, later
	// ... dont read tile while writing them, etc..
	@Override
	public synchronized CacheFile writeCache(Tile tile) {
		File f = new File(mCacheDir, String.format(CACHE_FILE,
				Integer.valueOf(tile.zoomLevel),
				Integer.valueOf(tile.tileX),
				Integer.valueOf(tile.tileY)));

		addTileHit(tile);

		try {
			return new CacheFile(tile, this, f, new FileOutputStream(f));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	private void addTileHit(Tile t) {
		mCommitHitList.add(t);

		if (mCommitHitList.size() > 100) {
			Tile[] tiles = new Tile[mCommitHitList.size()];
			tiles = mCommitHitList.toArray(tiles);
			mCommitHitList.clear();
			new AsyncTask<Tile, Void, Boolean>() {
				@Override
				protected Boolean doInBackground(Tile... commits) {
					for (final Tile tile : commits) {
						System.out.println("commit " + tile);
						final String tileFile = String.format(CACHE_FILE,
								Integer.valueOf(tile.zoomLevel),
								Integer.valueOf(tile.tileX),
								Integer.valueOf(tile.tileY));
						mDatasource.setTileHit(tileFile);
					}
					return Boolean.TRUE;
				}
			}.execute(tiles);
		}
	}

	// input stream must be closed by calller!
	@Override
	public synchronized InputStream getCache(Tile tile) {
		InputStream is = null;

		File f = new File(mCacheDir, String.format(CACHE_FILE,
				Integer.valueOf(tile.zoomLevel),
				Integer.valueOf(tile.tileX),
				Integer.valueOf(tile.tileY)));
		if (f.exists() && f.length() > 0) {
			try {
				is = new FileInputStream(f);

				addTileHit(tile);

				Log.d(TAG, tile + " using cache");

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return is;
	}

	@Override
	public void cacheCheck() {

	}

	private long getCacheDirSize() {
		if (mCacheDir != null) {
			long size = 0;
			File[] files = mCacheDir.listFiles();

			for (File file : files) {
				if (file.isFile()) {
					size += file.length();
				}
			}
			return size;
		}
		return -1;
	}

	@Override
	public void setCachingSize(long size) {
		this.MAX_SIZE = size * 1024 * 1024;
		Log.d(TAG, "set MAX_SIZE to: " + MAX_SIZE);
	}

}
