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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.oscim.core.Tile;
import org.oscim.generator.JobTile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

@SuppressLint("DefaultLocale")
public class CacheFileManager implements CacheManager {
	private final static String TAG = CacheFileManager.class.getName();
	private long MAX_SIZE;//= 4000000;//10485760L/2; // 10MB

	private String CACHE_DIRECTORY;
	private static final String CACHE_FILE = "%d-%d-%d.tile";

	final TileHitDataSource mDatasource;
	private File mCacheDir;

	private volatile long mCacheSize = 0;
	private Context mContext;

	private ArrayList<Tile> mCommitHitList;

	public CacheFileManager(Context context) {
		//		boolean mExternalStorageAvailable = false;
		//		boolean mExternalStorageWriteable = false;

		//			if (isSDPresent) {
		//				mCacheDir = createDirectory(cacheDirectoryPath);
		//				Log.d(TAG, "SDCARD");
		//			} else {
		//				mCacheDir = context.getCacheDir();
		//				Log.d(TAG, "Memery");
		//			}
		mContext = context;
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

		// FIXME ASYNC!!!!! but safe with other operation...
		cacheCheck((JobTile) tile);

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

			mDatasource.setTileHit(tiles);
		}
	}

	// input stream must be closed by calller!
	@Override
	public synchronized File getCache(Tile tile) {

		File f = new File(mCacheDir, String.format(CACHE_FILE,
				Integer.valueOf(tile.zoomLevel),
				Integer.valueOf(tile.tileX),
				Integer.valueOf(tile.tileY)));
		if (f.exists() && f.length() > 0) {
			return f;
		}

		return null;
	}

	private void cacheCheck(JobTile tile) {
		if (mCacheSize > MAX_SIZE) {
			if (mCleanupJob == null) {
				// TODO dump mCommitHitList before cleanup!

				long limit = MAX_SIZE - Math.min(MAX_SIZE / 4, 4 * 1024 * 1024);
				mCleanupJob = new CleanupTask(mCacheSize, limit, mCacheDir);
				mCleanupJob.execute(tile);
			}
		}
	}

	private long getCacheDirSize() {
		Log.d(TAG, ">>>>>> " + mCacheSize + " " + mCacheDir);
		if (mCacheDir != null) {
			long size = 0;
			File[] files = mCacheDir.listFiles();

			for (File file : files) {
				if (file.isFile()) {
					size += file.length();
				}
			}
			mCacheSize = size;
			Log.d(TAG, "cache size is now " + mCacheSize);

			return size;
		}
		return -1;
	}

	@Override
	public void setCachingSize(long size) {
		//this.MAX_SIZE = 500 * 1024; //size * 1024 * 1024;
		this.MAX_SIZE = size * 1024 * 1024;
		Log.d(TAG, "set MAX_SIZE to: " + MAX_SIZE);
	}

	@Override
	public void setCachingPath(String path) {
		this.CACHE_DIRECTORY = path;
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
			mCacheDir = mContext.getCacheDir();
			Log.d(TAG, "SDCARD not writing!");
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mCacheDir = mContext.getCacheDir();
			Log.d(TAG, "Memory");
		}
		Log.d(TAG, "cache dir: " + mCacheDir);

		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				getCacheDirSize();
				return null;
			}
		}.execute();
	}

	class CleanupTask extends AsyncTask<Tile, Void, Boolean> {
		File dir;
		long limit;
		long currentSize;
		long beginSize;

		public CleanupTask(long size, long limit, File cacheDir) {
			dir = cacheDir;
			this.limit = limit;
			beginSize = currentSize = size;
		}

		@Override
		protected Boolean doInBackground(Tile... params) {
			reduceCachingSize(params[0]);
			return null;
		}

		private void reduceCachingSize(Tile tile) {
			/* 1.distance
			 * 2.haeufigkeit
			 * 3.time */
			ArrayList<String> safeTile = new ArrayList<String>();
			int z = tile.zoomLevel;
			int x = tile.tileX;
			int y = tile.tileY;

			/* the tiles surrouding the current tile should not be deleted. */
			for (int zz = z; zz > 4; zz--) {
				for (int xx = x - 3; xx < x + 4; xx++) {
					for (int yy = y - 3; yy < y + 4; yy++) {
						if (xx < 0) {
							xx = (int) (Math.pow(2, zz) - 1 + xx);
						}
						if (yy > 0) {
							String safeTileFile = String.format(CACHE_FILE, Integer.valueOf(zz),
									Integer.valueOf(xx), Integer.valueOf(yy));
							safeTile.add(safeTileFile);
						}
					}
				}
				x = x / 2;
				x = y / 2;
			}
			/* get the middle haeufigkeit */
			//Log.d("Cache", "middle is: " + datasource.getMiddleHits());
			ArrayList<String> always = (ArrayList<String>) mDatasource
					.getAllTileFileAboveHits(mDatasource.getMiddleHits());

			//long limit = MAX_SIZE - 1024 * 1024;

			safeTile.addAll(always);
			/* time */
			if (dir != null) {
				File[] files = dir.listFiles();
				for (int i = 0; i < files.length && currentSize > limit; i++) {
					File file = files[i];
					if (!file.isFile()) {
						files[i] = null;
						continue;
					}
					Date now = new Date();
					long NOW = now.getTime();
					Date befor = new Date(file.lastModified());
					long BEFOR = befor.getTime();
					long old = NOW - BEFOR;
					//Log.d("Cache", file.getName());
					if (!safeTile.contains(file.getName()) && old > 2000000) {
						Log.d(TAG, "delete 1 " + file.getName());
						currentSize -= file.length();
						file.delete();
						files[i] = null;
					}
				}
				for (int i = 0; i < files.length && currentSize > limit; i++) {
					File file = files[i];
					if (file == null)
						continue;

					if (!safeTile.contains(file.getName())) {
						Log.d(TAG, "delete 2 " + file.getName());
						currentSize -= file.length();
						file.delete();
						files[i] = null;
					}
				}
				for (int i = 0; i < files.length && currentSize > limit; i++) {
					File file = files[i];
					if (file == null)
						continue;
					Log.d(TAG, "delete 3 " + file.getName());

					currentSize -= file.length();
					file.delete();
				}
			}
			mCacheSize -= (beginSize - currentSize);
			mCleanupJob = null;
			Log.d(TAG, "finished cleanup" + (beginSize - currentSize) + " now: " + mCacheSize);
		}

	}

	private CleanupTask mCleanupJob;

	@Override
	public void tileAdded(CacheFile cacheFile, int size) {

		mCacheSize += size;
		Log.d(TAG, cacheFile.mTile + " written: " + size + " sum:" + mCacheSize);
	}

}
