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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.oscim.core.Tile;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class MultiCachingFileManager implements CachingManager {

	private long MAX_SIZE = 4000000;//10485760L/2; // 10MB
	private FileOutputStream mCacheFile;
	private InputStream mInputStream = null;
	//int mReadPos;
	//	private static final int BUFFER_SIZE = 65536;
	//	private final byte[] mReadBuffer = new byte[BUFFER_SIZE];
	//	// position in read buffer
	//	private int mBufferPos;
	//	// bytes available in read buffer
	//	private int mBufferSize;
	// overall bytes of content processed
	//private int mBytesProcessed;
	File f = null;
	private static File cacheDir;
	private static final String CACHE_DIRECTORY = "/Android/data/org.oscim.app/cache/";
	private static final String CACHE_FILE = "%d-%d-%d.tile";
	private TileHitDataSource datasource;

	public MultiCachingFileManager(Context context) {
		//		boolean mExternalStorageAvailable = false;
		//		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (cacheDir == null) {
			String externalStorageDirectory = Environment
					.getExternalStorageDirectory()
					.getAbsolutePath();
			String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY;
			//			boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(
			//					android.os.Environment.MEDIA_MOUNTED);

			if (Environment.MEDIA_MOUNTED.equals(state)) {
				// We can read and write the media
				cacheDir = createDirectory(cacheDirectoryPath);
				Log.d("Cache", "SDCARD");
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				// We can only read the media
				cacheDir = context.getCacheDir();
				Log.d("Cache", "SDCARD not writing!");
			} else {
				// Something else is wrong. It may be one of many other states, but all we need
				//  to know is we can neither read nor write
				cacheDir = context.getCacheDir();
				Log.d("Cache", "Memery");
			}
			//			if (isSDPresent) {
			//				cacheDir = createDirectory(cacheDirectoryPath);
			//				Log.d("Cache", "SDCARD");
			//			} else {
			//				cacheDir = context.getCacheDir();
			//				Log.d("Cache", "Memery");
			//			}
		}
		datasource = new TileHitDataSource(context);
		//		if (datasource == null) {
		//			Log.d("TileHitDataSource", "datasource is null");
		//		} else {
		//			Log.d("TileHitDataSource", "datasource is not null");
		//		}
		datasource.open();
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

	private int written;

	@Override
	public boolean cacheBegin(Tile tile, byte[] readBuffer, int bufferPos, int bufferSize) {
		// TODO Auto-generated method stub
		f = new File(cacheDir, String.format(CACHE_FILE,
				Integer.valueOf(tile.zoomLevel),
				Integer.valueOf(tile.tileX),
				Integer.valueOf(tile.tileY)));
		String tileFile = String.format(CACHE_FILE,
				Integer.valueOf(tile.zoomLevel),
				Integer.valueOf(tile.tileX),
				Integer.valueOf(tile.tileY));
		if (getCacheDirSize() > MAX_SIZE) {
			Log.d("Cache", "MAX_SIZE: " + MAX_SIZE);
			datasource.deleteTileFileUnderhits(2);
		}
		datasource.setTileHit(tileFile);
		try {
			mCacheFile = new FileOutputStream(f);
			written = 0;

			if (bufferSize - bufferPos > 0) {
				try {
					mCacheFile.write(readBuffer, bufferPos,
							bufferSize - bufferPos);
					written += bufferSize - bufferPos;

				} catch (IOException e) {
					e.printStackTrace();
					mCacheFile = null;
					return false;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mCacheFile = null;
			return false;
		}
		return true;
	}

	@Override
	public void cacheWrite(byte[] readBuffer, int bufferSize, int len) {
		// TODO Auto-generated method stub
		try {
			mCacheFile.write(readBuffer, bufferSize, len);
			written += len;
			Log.d("", "write " + len + " " + written);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void cacheFinish(Tile tile, boolean success) {
		// TODO Auto-generated method stub
		Log.d("", tile + "cache written " + written);
		if (success) {
			try {
				mCacheFile.flush();
				mCacheFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCacheFile = null;
		} else {
			f.delete();
		}
	}

	@Override
	public InputStream cacheReadBegin(Tile tile) {
		// TODO Auto-generated method stub
		f = new File(cacheDir, String.format(CACHE_FILE,
				Integer.valueOf(tile.zoomLevel),
				Integer.valueOf(tile.tileX),
				Integer.valueOf(tile.tileY)));
		if (f.exists() && f.length() > 0) {
			try {
				mInputStream = new FileInputStream(f);
				String tileFile = String.format(CACHE_FILE,
						Integer.valueOf(tile.zoomLevel),
						Integer.valueOf(tile.tileX),
						Integer.valueOf(tile.tileY));
				datasource.setTileHit(tileFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			return null;
		}
		return mInputStream;
	}

	@Override
	public void cacheReadFinish() {
		// TODO Auto-generated method stub
		if (mInputStream != null) {
			try {
				mInputStream.close();
				//f.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void cacheCheck() {

	}

	private static long getCacheDirSize() {
		if (cacheDir != null) {
			long size = 0;
			File[] files = cacheDir.listFiles();

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
		// TODO Auto-generated method stub
		this.MAX_SIZE = size * 1024 * 1024;
		Log.d("Cache", "set MAX_SIZE to: " + MAX_SIZE);
	}

}
