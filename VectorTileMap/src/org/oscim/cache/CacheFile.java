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
import java.io.FileOutputStream;
import java.io.IOException;

import org.oscim.core.Tile;

import android.util.Log;

public class CacheFile {
	private final static String TAG = CacheFile.class.getName();

	private int written;
	private final FileOutputStream mCacheFile;
	private final File mFile;
	private final CacheManager mCacheManager;
	private final Tile mTile;

	CacheFile(Tile tile, CacheManager cm, File f, FileOutputStream out) {
		mCacheManager = cm;
		mCacheFile = out;
		mFile = f;
		mTile = tile;
	}

	public void write(byte[] readBuffer, int bufferSize, int len) {
		try {
			mCacheFile.write(readBuffer, bufferSize, len);
			written += len;
		} catch (IOException e) {
			e.printStackTrace();
			Log.d(TAG, mTile + " error writing: " + len);
			// FIXME close file, set failed state, 
		}
	}

	public void cacheFinish(boolean success) {
		if (mCacheFile == null)
			return;

		Log.d(TAG, mTile + " written: " + written);
		try {
			mCacheFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!success)
			mFile.delete();
		//else
		//	tell cacheManager that dir size increased by bytes written..
		// increase hit count etc
	}
}
