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

import java.io.InputStream;

import org.oscim.core.Tile;

public interface CacheManager {
	//boolean cacheBegin(Tile tile, byte[] readBuffer, int bufferPos, int bufferSize);
	CacheFile writeCache(Tile tile);

	//void cacheFinish(Tile tile, boolean success);

	InputStream getCache(Tile tile);

	//void cacheReadFinish();

	//void cacheWrite(byte[] readBuffer, int offset, int len);

	void setCachingSize(long size);

	void setCachingPath(String path);
}
