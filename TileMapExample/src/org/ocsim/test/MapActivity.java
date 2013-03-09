package org.ocsim.test;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.database.MapDatabases;
import org.oscim.database.MapOptions;
import org.oscim.view.MapView;

import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;

public class MapActivity extends org.oscim.view.MapActivity {

	private MapView mMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMap = (MapView) findViewById(R.id.mapView);

		// MapOptions options = new MapOptions(MapDatabases.PBMAP_READER);
		// options.put("url",
		// "http://city.informatik.uni-bremen.de:80/osmstache/test/");

		// MapOptions options = new MapOptions(MapDatabases.OSCIMAP_READER);
		// options.put("url",
		// "http://city.informatik.uni-bremen.de:80/osci/map-live/");

		MapOptions options = new MapOptions(MapDatabases.MAP_READER);
		// options.put("file",
		// Environment.getExternalStorageDirectory().getPath() +"/bremen.map");
		options.put("file", Environment.getExternalStorageDirectory().getPath()
				+ "/Download/berlin.map");

		mMap.setMapDatabase(options);

		// configure the MapView and activate the zoomLevel buttons
		mMap.setClickable(true);
		mMap.setFocusable(true);

		MapPosition mapCenter = mMap.getMapFileCenter();
		if (mapCenter == null)
			mMap.setMapCenter(new MapPosition(new GeoPoint(53.1f, 8.8f),
					(byte) 12, 1));
		else
			mMap.setMapCenter(mapCenter);

		// mMap.getOverlayManager().add(new GenericOverlay(mMap,new
		// GridOverlay(mMap)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}
