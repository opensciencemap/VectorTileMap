/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.renderer.overlays;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.TileManager;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.layer.TextItem;
import org.oscim.renderer.layer.TextLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.view.MapView;

import android.graphics.Color;
import android.graphics.Paint.Cap;

public class OverlayGrid extends RenderOverlay {

	private TileSet tiles;
	private Text mText;
	final static String TAG = "OverlayGrid";

	public OverlayGrid(MapView mapView) {
		super(mapView);
		mText = Text.createText(22, 2, 0xFFFFFFFF, 0xCC000000, false);
	}

	private void addLabels(int x, int y, int z) {
		int size = Tile.TILE_SIZE;

		TextLayer tl = new TextLayer();
		tiles = TileManager.getActiveTiles(tiles);

		for (int i = -2; i < 2; i++) {
			for (int j = -2; j < 2; j++) {
				for (MapTile t : tiles.tiles) {
					if (t != null && t.tileX == x + j && t.tileY == y + i && t.isEmpty) {

						//  check parents
						if ((t.proxies & 1 << 4) != 0)
							continue;

						// check children
						if ((t.proxies & 1 << 0) != 0
								&& (t.proxies & 1 << 1) != 0
								&& (t.proxies & 1 << 2) != 0
								&& (t.proxies & 1 << 3) != 0)
							continue;
						TextItem ti = TextItem.get().set(size * j + size / 2, size * i + size / 2,
								"no data", mText);
						TextItem ti2 = TextItem.get().set(size * j + size / 2,
								size * i + size / 2 + mText.fontDescent + mText.fontHeight,
								"check the connection", mText);
						ti.x1 = 0;
						ti.y1 = 1;
						ti.x2 = 1;
						ti.y2 = 1;

						ti2.x1 = 0;
						ti2.y1 = 1;
						ti2.x2 = 1;
						ti2.y2 = 1;
						tl.addText(ti);
						tl.addText(ti2);
					}

				}
			}
		}

		tl.prepare();

		layers.textureLayers = tl;
	}

	private int mCurX = -1;
	private int mCurY = -1;
	private byte mCurZ = -1;

	private boolean finished;

	void timerFinished() {
		//Log.d("...", "timer finish!");
		finished = true;
		mMapView.redrawMap();
	}

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		updateMapPosition();

		// fix map position to tile coordinates
		float size = Tile.TILE_SIZE;
		int x = (int) (mMapPosition.x / size);
		int y = (int) (mMapPosition.y / size);
		mMapPosition.x = x * size;
		mMapPosition.y = y * size;

		if (!finished)
			mMapPosition.scale = 1;

		// update layers when map moved by at least one tile
		if (x != mCurX || y != mCurY || mMapPosition.zoomLevel != mCurZ) {
			mCurX = x;
			mCurY = y;
			mCurZ = mMapPosition.zoomLevel;

			layers.clear();

			LineLayer ll = (LineLayer) layers.getLayer(1, Layer.LINE);
			ll.line = new Line(Color.BLUE, 1.0f, Cap.BUTT);
			ll.width = 1.5f;
			//ll.addLine(mPoints, mIndex, false);

			//Log.d("...", "update labels");

			addLabels(x, y, mCurZ);

			newData = true;
			finished = false;
		}
	}
}
