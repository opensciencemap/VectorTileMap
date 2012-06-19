/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.map.writer.coastlinealgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.mapsforge.core.model.Tile;

/**
 * The CoastlineAlgorithm generates closed polygons from disjoint coastline segments. The algorithm is based on the
 * close-areas.pl script, written by Frederik Ramm for the Osmarender program.
 */
class CoastlineAlgorithm {
	/**
	 * A HelperPoint represents one of the four corners of the virtual tile.
	 */
	private static class HelperPoint {
		int x;
		int y;

		HelperPoint() {
			// do nothing
		}
	}

	private final List<HelperPoint> additionalCoastlinePoints;
	private int coastlineEndLength;
	private final Map<ImmutablePoint, float[]> coastlineEnds;
	private final List<float[]> coastlineSegments;
	private int coastlineStartLength;
	private final Map<ImmutablePoint, float[]> coastlineStarts;
	private final List<CoastlineWay> coastlineWays;
	private float[] coordinates;
	private final Set<EndPoints> handledCoastlineSegments;
	private final HelperPoint[] helperPoints;
	private float[] matchPath;
	private boolean needHelperPoint;
	private float[] newPath;
	private int relativeX1;
	private int relativeX2;
	private int relativeY1;
	private int relativeY2;
	private final int[] virtualTileBoundaries;
	private int virtualTileSize;
	private int zoomLevelDifference;

	/**
	 * Constructs a new CoastlineAlgorithm instance to generate closed polygons.
	 */
	CoastlineAlgorithm() {
		// create the four helper points at the tile corners
		this.helperPoints = new HelperPoint[4];
		this.helperPoints[0] = new HelperPoint();
		this.helperPoints[1] = new HelperPoint();
		this.helperPoints[2] = new HelperPoint();
		this.helperPoints[3] = new HelperPoint();

		this.additionalCoastlinePoints = new ArrayList<HelperPoint>(4);
		this.coastlineWays = new ArrayList<CoastlineWay>(4);

		// create the data structures for the coastline segments
		this.coastlineSegments = new ArrayList<float[]>(8);
		this.coastlineEnds = new TreeMap<ImmutablePoint, float[]>();
		this.coastlineStarts = new TreeMap<ImmutablePoint, float[]>();
		this.handledCoastlineSegments = new HashSet<EndPoints>(64);

		this.virtualTileBoundaries = new int[4];
	}

	/**
	 * Adds a coastline segment to the internal data structures. Coastline segments are automatically merged into longer
	 * parts when they share the same start or end point. Adding the same coastline segment more than once has no
	 * effect.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 */
	void addCoastlineSegment(float[] coastline) {
		if (CoastlineWay.isClosed(coastline) && coastline.length < 6) {
			// invalid polygon, skip it
			return;
		}

		// all coastline segments are accumulated and merged together if possible
		float[] nodesSequence = coastline;
		ImmutablePoint coastlineStartPoint = new ImmutablePoint(nodesSequence[0], nodesSequence[1]);
		ImmutablePoint coastlineEndPoint = new ImmutablePoint(nodesSequence[nodesSequence.length - 2],
				nodesSequence[nodesSequence.length - 1]);
		EndPoints endPoints = new EndPoints(coastlineStartPoint, coastlineEndPoint);

		// check for an already closed coastline segment
		if (coastlineStartPoint.equals(coastlineEndPoint)) {
			this.coastlineSegments.add(nodesSequence);
			return;
		}

		// check to avoid duplicate coastline segments
		if (!this.handledCoastlineSegments.contains(endPoints)) {
			// update the set of handled coastline segments
			this.handledCoastlineSegments.add(new EndPoints(coastlineStartPoint, coastlineEndPoint));

			// check if a data way starts with the last point of the current way
			if (this.coastlineStarts.containsKey(coastlineEndPoint)) {
				// merge both way segments
				this.matchPath = this.coastlineStarts.remove(coastlineEndPoint);
				this.newPath = new float[nodesSequence.length + this.matchPath.length - 2];
				System.arraycopy(nodesSequence, 0, this.newPath, 0, nodesSequence.length - 2);
				System.arraycopy(this.matchPath, 0, this.newPath, nodesSequence.length - 2, this.matchPath.length);
				nodesSequence = this.newPath;
				coastlineEndPoint = new ImmutablePoint(nodesSequence[nodesSequence.length - 2],
						nodesSequence[nodesSequence.length - 1]);
			}

			// check if a data way ends with the first point of the current way
			if (this.coastlineEnds.containsKey(coastlineStartPoint)) {
				this.matchPath = this.coastlineEnds.remove(coastlineStartPoint);
				// check if the merged way is already a circle
				if (!coastlineStartPoint.equals(coastlineEndPoint)) {
					// merge both way segments
					this.newPath = new float[nodesSequence.length + this.matchPath.length - 2];
					System.arraycopy(this.matchPath, 0, this.newPath, 0, this.matchPath.length - 2);
					System.arraycopy(nodesSequence, 0, this.newPath, this.matchPath.length - 2, nodesSequence.length);
					nodesSequence = this.newPath;
					coastlineStartPoint = new ImmutablePoint(nodesSequence[0], nodesSequence[1]);
				}
			}

			this.coastlineStarts.put(coastlineStartPoint, nodesSequence);
			this.coastlineEnds.put(coastlineEndPoint, nodesSequence);
		}
	}

	/**
	 * Clears the internal data structures. Must be called between tiles.
	 */
	void clearCoastlineSegments() {
		this.coastlineSegments.clear();
		this.coastlineStarts.clear();
		this.coastlineEnds.clear();
		this.handledCoastlineSegments.clear();
		this.coastlineWays.clear();
	}

	/**
	 * Generates closed water and land polygons from unconnected coastline segments. Closed segments are handled either
	 * as water or islands, depending on their orientation.
	 * 
	 * @param closedPolygonHandler
	 *            the implementation which will be called to handle the generated polygons.
	 */
	void generateClosedPolygons(ClosedPolygonHandler closedPolygonHandler) {
		this.coastlineSegments.addAll(this.coastlineStarts.values());

		// check if there are any coastline segments
		if (this.coastlineSegments.isEmpty()) {
			return;
		}

		boolean islandSituation = false;
		boolean waterBackground = true;
		boolean invalidCoastline = false;
		for (float[] coastline : this.coastlineSegments) {
			// is the current segment already closed?
			if (CoastlineWay.isClosed(coastline)) {
				// depending on the orientation we have either water or an island
				if (CoastlineWay.isClockWise(coastline)) {
					// water
					waterBackground = false;
					closedPolygonHandler.onWaterPolygon(coastline);
				} else {
					// island
					islandSituation = true;
					closedPolygonHandler.onIslandPolygon(coastline);
				}
			} else if (CoastlineWay.isValid(coastline, this.virtualTileBoundaries)) {
				coastline = SutherlandHodgmanClipping.clipPolyline(coastline, this.virtualTileBoundaries);
				if (coastline != null) {
					this.coastlineWays
							.add(new CoastlineWay(coastline, this.virtualTileBoundaries, this.virtualTileSize));
				}
			} else {
				invalidCoastline = true;
				closedPolygonHandler.onInvalidCoastlineSegment(coastline);
			}
		}

		if (invalidCoastline) {
			// do not create any closed polygons, just draw the coastline segments
			for (int i = 0, n = this.coastlineWays.size(); i < n; ++i) {
				closedPolygonHandler.onValidCoastlineSegment(this.coastlineWays.get(i).data);
			}
			return;
		}

		// check if there are no errors and the tile needs a water background
		if (islandSituation && waterBackground && this.coastlineWays.isEmpty()) {
			// add a water polygon for the whole tile
			closedPolygonHandler.onWaterTile();
			return;
		}

		// order all coastline segments ascending by their entering angle
		Collections.sort(this.coastlineWays, CoastlineWayComparator.INSTANCE);

		// join coastline segments to create closed water segments
		CoastlineWay coastlineStart;
		CoastlineWay coastlineEnd;
		int currentSide;
		while (!this.coastlineWays.isEmpty()) {
			coastlineStart = this.coastlineWays.get(0);
			coastlineEnd = null;
			// try to find a matching coastline segment
			for (int i = 0, n = this.coastlineWays.size(); i < n; ++i) {
				CoastlineWay coastline = this.coastlineWays.get(i);
				if (coastline.entryAngle > coastlineStart.exitAngle) {
					coastlineEnd = coastline;
					break;
				}
			}
			if (coastlineEnd == null) {
				// no coastline segment was found, take the first one
				coastlineEnd = this.coastlineWays.get(0);
			}
			this.coastlineWays.remove(0);

			// if the segment orientation is clockwise, we need at least one helper point
			if (coastlineEnd.entrySide == CoastlineWay.SIDE_RIGHT && coastlineStart.exitSide == CoastlineWay.SIDE_RIGHT) {
				this.needHelperPoint = (coastlineStart.exitAngle > coastlineEnd.entryAngle && (coastlineStart.exitAngle - coastlineEnd.entryAngle) < Math.PI)
						|| (coastlineStart.exitAngle < Math.PI && coastlineEnd.entryAngle > Math.PI);
			} else {
				this.needHelperPoint = coastlineStart.exitAngle > coastlineEnd.entryAngle;
			}

			this.additionalCoastlinePoints.clear();
			currentSide = coastlineStart.exitSide;

			// walk around the tile and add additional points to the list
			while (currentSide != coastlineEnd.entrySide || this.needHelperPoint) {
				this.needHelperPoint = false;
				this.additionalCoastlinePoints.add(this.helperPoints[currentSide]);
				currentSide = (currentSide + 1) % 4;
			}

			// check if the start segment is also the end segment
			if (coastlineStart.equals(coastlineEnd)) {
				// calculate the length of the new way
				this.coastlineStartLength = coastlineStart.data.length;
				this.coordinates = new float[this.coastlineStartLength + this.additionalCoastlinePoints.size() * 2 + 2];

				// copy the start segment
				System.arraycopy(coastlineStart.data, 0, this.coordinates, 0, this.coastlineStartLength);

				// copy the additional points
				for (int i = 0; i < this.additionalCoastlinePoints.size(); ++i) {
					this.coordinates[this.coastlineStartLength + 2 * i] = this.additionalCoastlinePoints.get(i).x;
					this.coordinates[this.coastlineStartLength + 2 * i + 1] = this.additionalCoastlinePoints.get(i).y;
				}

				// close the way
				this.coordinates[this.coordinates.length - 2] = this.coordinates[0];
				this.coordinates[this.coordinates.length - 1] = this.coordinates[1];

				// add the now closed way as a water polygon to the way list
				closedPolygonHandler.onWaterPolygon(this.coordinates);
			} else {
				// calculate the length of the new coastline segment
				this.coastlineStartLength = coastlineStart.data.length;
				this.coastlineEndLength = coastlineEnd.data.length;
				float[] newSegment = new float[this.coastlineStartLength + this.additionalCoastlinePoints.size() * 2
						+ this.coastlineEndLength];

				// copy the start segment
				System.arraycopy(coastlineStart.data, 0, newSegment, 0, this.coastlineStartLength);

				// copy the additional points
				for (int i = 0; i < this.additionalCoastlinePoints.size(); ++i) {
					newSegment[this.coastlineStartLength + 2 * i] = this.additionalCoastlinePoints.get(i).x;
					newSegment[this.coastlineStartLength + 2 * i + 1] = this.additionalCoastlinePoints.get(i).y;
				}

				// copy the end segment
				System.arraycopy(coastlineEnd.data, 0, newSegment, this.coastlineStartLength
						+ this.additionalCoastlinePoints.size() * 2, this.coastlineEndLength);

				// replace the end segment in the list with the new segment
				this.coastlineWays.remove(coastlineEnd);
				newSegment = SutherlandHodgmanClipping.clipPolyline(newSegment, this.virtualTileBoundaries);
				if (newSegment != null) {
					this.coastlineWays.add(new CoastlineWay(newSegment, this.virtualTileBoundaries,
							this.virtualTileSize));
					Collections.sort(this.coastlineWays, CoastlineWayComparator.INSTANCE);
				}
			}
		}
	}

	/**
	 * Sets the tiles on which the coastline algorithm should work.
	 * 
	 * @param readCoastlineTile
	 *            the tile whose coastline segments have been read.
	 * @param currentTile
	 *            the tile for which the coastline coordinates are relative to.
	 */
	void setTiles(Tile readCoastlineTile, Tile currentTile) {
		if (readCoastlineTile.zoomLevel < currentTile.zoomLevel) {
			// calculate the virtual tile dimensions
			this.zoomLevelDifference = currentTile.zoomLevel - readCoastlineTile.zoomLevel;
			this.virtualTileSize = Tile.TILE_SIZE << this.zoomLevelDifference;
			this.relativeX1 = (int) ((readCoastlineTile.getPixelX() << this.zoomLevelDifference) - currentTile
					.getPixelX());
			this.relativeY1 = (int) ((readCoastlineTile.getPixelY() << this.zoomLevelDifference) - currentTile
					.getPixelY());
			this.relativeX2 = this.relativeX1 + this.virtualTileSize;
			this.relativeY2 = this.relativeY1 + this.virtualTileSize;

			this.virtualTileBoundaries[0] = this.relativeX1;
			this.virtualTileBoundaries[1] = this.relativeY1;
			this.virtualTileBoundaries[2] = this.relativeX2;
			this.virtualTileBoundaries[3] = this.relativeY2;
		} else {
			// use the standard tile dimensions
			this.virtualTileSize = Tile.TILE_SIZE;

			this.virtualTileBoundaries[0] = 0;
			this.virtualTileBoundaries[1] = 0;
			this.virtualTileBoundaries[2] = Tile.TILE_SIZE;
			this.virtualTileBoundaries[3] = Tile.TILE_SIZE;
		}

		// bottom-right
		this.helperPoints[0].x = this.virtualTileBoundaries[2];
		this.helperPoints[0].y = this.virtualTileBoundaries[3];

		// bottom-left
		this.helperPoints[1].x = this.virtualTileBoundaries[0];
		this.helperPoints[1].y = this.virtualTileBoundaries[3];

		// top-left
		this.helperPoints[2].x = this.virtualTileBoundaries[0];
		this.helperPoints[2].y = this.virtualTileBoundaries[1];

		// top-right
		this.helperPoints[3].x = this.virtualTileBoundaries[2];
		this.helperPoints[3].y = this.virtualTileBoundaries[1];
	}
}