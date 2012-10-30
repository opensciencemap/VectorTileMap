package org.osmdroid.location;

import java.util.List;

import org.oscim.core.BoundingBox;

public interface POIProvider {
	public List<POI> getPOIInside(BoundingBox boundingBox, String query, int maxResults);
}
