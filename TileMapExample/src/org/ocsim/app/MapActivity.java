package org.ocsim.app;

import org.oscim.database.MapDatabases;
import org.oscim.database.MapOptions;
import org.oscim.view.MapView;

import android.os.Bundle;
import android.view.Menu;

public class MapActivity extends org.oscim.view.MapActivity {

	private MapView mMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMap = (MapView) findViewById(R.id.mapView);

		//MapOptions options = new MapOptions(MapDatabases.PBMAP_READER);
		//options.put("url", "http://city.informatik.uni-bremen.de:80/osmstache/test/");
		MapOptions options = new MapOptions(MapDatabases.OSCIMAP_READER);
		options.put("url", "http://city.informatik.uni-bremen.de:80/osci/map-live/");

		mMap.setMapDatabase(options);

		// configure the MapView and activate the zoomLevel buttons
		mMap.setClickable(true);
		mMap.setFocusable(true);
		//mMap.getOverlayManager().add(new EventsOverlay());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}
