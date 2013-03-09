/*
 * Copyright 2012 osmdroid: M.Kergall
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
package org.oscim.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.oscim.core.GeoPoint;
import org.oscim.overlay.Overlay;
import org.oscim.overlay.OverlayItem;
import org.oscim.overlay.PathOverlay;
import org.oscim.view.MapView;
import org.osmdroid.location.GeocoderNominatim;
import org.osmdroid.overlays.DefaultInfoWindow;
import org.osmdroid.overlays.ExtendedOverlayItem;
import org.osmdroid.overlays.ItemizedOverlayWithBubble;
import org.osmdroid.routing.Road;
import org.osmdroid.routing.RoadManager;
import org.osmdroid.routing.RoadNode;
import org.osmdroid.routing.provider.MapQuestRoadManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class RouteSearch {
	protected Road mRoad;
	protected PathOverlay mRoadOverlay;
	protected ItemizedOverlayWithBubble<ExtendedOverlayItem> mRoadNodeMarkers;
	protected ItemizedOverlayWithBubble<ExtendedOverlayItem> itineraryMarkers;

	protected GeoPoint startPoint, destinationPoint;
	protected ArrayList<GeoPoint> viaPoints;
	protected static int START_INDEX = -2, DEST_INDEX = -1;
	protected ExtendedOverlayItem markerStart, markerDestination;

	private final TileMap tileMap;

	RouteSearch(TileMap tileMap) {

		this.tileMap = tileMap;
		startPoint = null;
		destinationPoint = null;
		viaPoints = new ArrayList<GeoPoint>();

		// Itinerary markers:
		final ArrayList<ExtendedOverlayItem> waypointsItems = new ArrayList<ExtendedOverlayItem>();
		itineraryMarkers = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(tileMap.map, tileMap,
				waypointsItems, new ViaPointInfoWindow(R.layout.itinerary_bubble, tileMap.map));
		tileMap.map.getOverlays().add(itineraryMarkers);
		updateUIWithItineraryMarkers();

		//Route and Directions
		final ArrayList<ExtendedOverlayItem> roadItems = new ArrayList<ExtendedOverlayItem>();
		mRoadNodeMarkers = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(tileMap, roadItems,
				tileMap.map);
		tileMap.map.getOverlays().add(mRoadNodeMarkers);
	}

	//------------- Geocoding and Reverse Geocoding

	/**
	 * Reverse Geocoding
	 * @param p
	 *            ...
	 * @return ...
	 */
	public String getAddress(GeoPoint p) {
		GeocoderNominatim geocoder = new GeocoderNominatim(tileMap);
		String theAddress;
		try {
			double dLatitude = p.getLatitude();
			double dLongitude = p.getLongitude();
			List<Address> addresses = geocoder.getFromLocation(dLatitude, dLongitude, 1);
			StringBuilder sb = new StringBuilder();
			if (addresses.size() > 0) {
				Address address = addresses.get(0);
				int n = address.getMaxAddressLineIndex();
				for (int i = 0; i <= n; i++) {
					if (i != 0)
						sb.append(", ");
					sb.append(address.getAddressLine(i));
				}
				theAddress = new String(sb.toString());
			} else {
				theAddress = null;
			}
		} catch (IOException e) {
			theAddress = null;
		}
		if (theAddress != null) {
			return theAddress;
		}
		return "";
	}

	// Async task to reverse-geocode the marker position in a separate thread:
	class GeocodingTask extends AsyncTask<Object, Void, String> {
		ExtendedOverlayItem marker;

		@Override
		protected String doInBackground(Object... params) {
			marker = (ExtendedOverlayItem) params[0];
			return getAddress(marker.getPoint());
		}

		@Override
		protected void onPostExecute(String result) {
			marker.setDescription(result);
			//itineraryMarkers.showBubbleOnItem(???, map); //open bubble on the item
		}
	}

	//------------ Itinerary markers

	/* add (or replace) an item in markerOverlays. p position. */
	public ExtendedOverlayItem putMarkerItem(ExtendedOverlayItem item, GeoPoint p, int index,
			int titleResId, int markerResId, int iconResId) {

		if (item != null) {
			itineraryMarkers.removeItem(item);
		}

		Drawable marker = App.res.getDrawable(markerResId);
		String title = App.res.getString(titleResId);

		ExtendedOverlayItem overlayItem = new ExtendedOverlayItem(title, "", p);
		overlayItem.setMarkerHotspot(OverlayItem.HotspotPlace.BOTTOM_CENTER);
		overlayItem.setMarker(marker);

		if (iconResId != -1)
			overlayItem.setImage(App.res.getDrawable(iconResId));
		overlayItem.setRelatedObject(Integer.valueOf(index));

		itineraryMarkers.addItem(overlayItem);

		tileMap.map.redrawMap(true);

		//Start geocoding task to update the description of the marker with its address:
		new GeocodingTask().execute(overlayItem);
		return overlayItem;
	}

	public void addViaPoint(GeoPoint p) {
		viaPoints.add(p);
		putMarkerItem(null, p, viaPoints.size() - 1,
				R.string.viapoint, R.drawable.marker_via, -1);
	}

	public void removePoint(int index) {
		if (index == START_INDEX)
			startPoint = null;
		else if (index == DEST_INDEX)
			destinationPoint = null;
		else
			viaPoints.remove(index);

		getRoadAsync();
		updateUIWithItineraryMarkers();
	}

	public void updateUIWithItineraryMarkers() {
		itineraryMarkers.removeAllItems();
		//Start marker:
		if (startPoint != null) {
			markerStart = putMarkerItem(null, startPoint, START_INDEX,
					R.string.departure, R.drawable.marker_departure, -1);
		}
		//Via-points markers if any:
		for (int index = 0; index < viaPoints.size(); index++) {
			putMarkerItem(null, viaPoints.get(index), index,
					R.string.viapoint, R.drawable.marker_via, -1);
		}
		//Destination marker if any:
		if (destinationPoint != null) {
			markerDestination = putMarkerItem(null, destinationPoint, DEST_INDEX,
					R.string.destination,
					R.drawable.marker_destination, -1);
		}
	}

	//------------ Route and Directions

	private void putRoadNodes(Road road) {
		mRoadNodeMarkers.removeAllItems();
		Drawable marker = App.res.getDrawable(R.drawable.marker_node);
		int n = road.nodes.size();
		//		TypedArray iconIds = App.res.obtainTypedArray(R.array.direction_icons);
		for (int i = 0; i < n; i++) {
			RoadNode node = road.nodes.get(i);
			String instructions = (node.instructions == null ? "" : node.instructions);
			ExtendedOverlayItem nodeMarker = new ExtendedOverlayItem(
					"Step " + (i + 1), instructions, node.location);

			nodeMarker.setSubDescription(road.getLengthDurationText(node.length, node.duration));
			nodeMarker.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
			nodeMarker.setMarker(marker);
			//			int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
			//			if (iconId != R.drawable.ic_empty) {
			//				Drawable icon = App.res.getDrawable(iconId);
			//				nodeMarker.setImage(icon);
			//			}
			mRoadNodeMarkers.addItem(nodeMarker);
		}
	}

	void updateUIWithRoad(Road road) {
		mRoadNodeMarkers.removeAllItems();
		List<Overlay> mapOverlays = tileMap.map.getOverlays();
		if (mRoadOverlay != null) {
			mapOverlays.remove(mRoadOverlay);
		}
		if (road == null)
			return;
		if (road.status == Road.STATUS_DEFAULT)
			Toast.makeText(tileMap, "We have a problem to get the route",
					Toast.LENGTH_SHORT).show();
		mRoadOverlay = RoadManager.buildRoadOverlay(tileMap.map, road, tileMap);
		Overlay removedOverlay = mapOverlays.set(1, mRoadOverlay);
		//we set the road overlay at the "bottom", just above the MapEventsOverlay,
		//to avoid covering the other overlays. 
		mapOverlays.add(removedOverlay);
		putRoadNodes(road);

		tileMap.map.redrawMap(true);

		//Set route info in the text view:

		//	((TextView) findViewById(R.id.routeInfo)).setText(road.getLengthDurationText(-1));
	}

	void removeRoadPath() {
		List<Overlay> mapOverlays = tileMap.map.getOverlays();
		if (mRoadOverlay != null) {
			mapOverlays.remove(mRoadOverlay);
		}
		tileMap.map.redrawMap(true);
	}

	void removeRoadNodes() {
		mRoadNodeMarkers.removeAllItems();
		tileMap.map.redrawMap(true);
	}

	void removeAllOverlay() {
		List<Overlay> mapOverlays = tileMap.map.getOverlays();
		if (mRoadOverlay != null) {
			mapOverlays.remove(mRoadOverlay);
		}
		if (mRoadNodeMarkers != null) {
			mapOverlays.remove(mRoadNodeMarkers);
		}
		final ArrayList<ExtendedOverlayItem> roadItems = new ArrayList<ExtendedOverlayItem>();
		mRoadNodeMarkers = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(tileMap, roadItems,
				tileMap.map);
		tileMap.map.getOverlays().add(mRoadNodeMarkers);

		//removePoint(-2);
		removePoint(-1);
		tileMap.map.redrawMap(true);
	}

	/**
	 * Async task to get the road in a separate thread.
	 */
	class UpdateRoadTask extends AsyncTask<WayPoints, Void, Road> {
		@Override
		protected Road doInBackground(WayPoints... wp) {
			ArrayList<GeoPoint> waypoints = wp[0].waypoints;
			//RoadManager roadManager = new GoogleRoadManager();
			//RoadManager roadManager = new OSRMRoadManager();
			RoadManager roadManager = new MapQuestRoadManager();
			Locale locale = Locale.getDefault();
			roadManager.addRequestOption("locale=" + locale.getLanguage() + "_"
					+ locale.getCountry());
			return roadManager.getRoad(waypoints);
		}

		@Override
		protected void onPostExecute(Road result) {
			mRoad = result;
			updateUIWithRoad(result);
		}
	}

	// Just to make JAVA shut up!
	class WayPoints {
		public ArrayList<GeoPoint> waypoints;
	}

	public void getRoadAsync() {
		mRoad = null;
		if (startPoint == null || destinationPoint == null) {
			updateUIWithRoad(mRoad);
			return;
		}
		ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>(2);
		waypoints.add(startPoint);
		//add intermediate via points:
		for (GeoPoint p : viaPoints) {
			waypoints.add(p);
		}
		waypoints.add(destinationPoint);
		WayPoints wp = new WayPoints();
		wp.waypoints = waypoints;
		new UpdateRoadTask().execute(wp);
	}

	GeoPoint tempClickedGeoPoint; //any other way to pass the position to the menu ???

	boolean longPress(GeoPoint p) {
		tempClickedGeoPoint = p;
		return true;
	}

	void singleTapUp() {
		mRoadNodeMarkers.hideBubble();
		itineraryMarkers.hideBubble();
	}

	// ----------- context menu
	boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_departure:
			startPoint = tempClickedGeoPoint;
			markerStart = putMarkerItem(markerStart, startPoint, START_INDEX,
					R.string.departure, R.drawable.marker_departure, -1);
			getRoadAsync();
			return true;

		case R.id.menu_destination:
			destinationPoint = tempClickedGeoPoint;
			markerDestination = putMarkerItem(markerDestination, destinationPoint, DEST_INDEX,
					R.string.destination,
					R.drawable.marker_destination, -1);
			getRoadAsync();
			return true;

		case R.id.menu_viapoint:
			GeoPoint viaPoint = tempClickedGeoPoint;
			addViaPoint(viaPoint);
			getRoadAsync();
			return true;

		case R.id.menu_clear:
			AlertDialog.Builder builder = new AlertDialog.Builder(tileMap);
			//				if (mRoadOverlay != null) {
			//					mapOverlays.remove(mRoadOverlay);
			//				}
			//				if (mRoadNodeMarkers != null) {
			//					mapOverlays.remove(mRoadNodeMarkers);
			//				}
			List<Overlay> mapOverlays = tileMap.map.getOverlays();
			ArrayList<String> list = new ArrayList<String>();

			if (!mapOverlays.contains(mRoadOverlay) && mRoadNodeMarkers.size() != 0) {
				list.clear();
				list.add("Clear Route Node Only");
				list.add("Clear All");
			} else if (mRoadNodeMarkers.size() == 0 && mapOverlays.contains(mRoadOverlay)) {
				list.clear();
				list.add("Clear Route Only");
				list.add("Clear All");
			} else if (!mapOverlays.contains(mRoadOverlay) && mRoadNodeMarkers.size() == 0
					&& markerDestination == null) {
				list.clear();
				list.add("Nothing to Clear");
			} else if (!mapOverlays.contains(mRoadOverlay) && mRoadNodeMarkers.size() == 0
					&& markerDestination != null) {
				list.clear();
				list.add("Clear All");
			} else {
				list.clear();
				list.add("Clear Route Node Only");
				list.add("Clear Route Only");
				list.add("Clear All");
			}
			final String[] test = new String[list.size()];
			for (int i = 0; i < list.size(); i++) {
				test[i] = list.get(i);
			}
			builder.setTitle("Clear");
			builder.setItems(test, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (test[which].equals("Clear Route Only")) {
						removeRoadPath();
					} else if (test[which].equals("Clear Route Node Only")) {
						removeRoadNodes();
					} else if (test[which].equals("Clear All")) {
						removeAllOverlay();
					}
					// The 'which' argument contains the index position
					// of the selected item
				}
			});
			AlertDialog alertDialog = builder.create();

			alertDialog.show();
			return true;

		default:
		}
		return false;
	}

	class ViaPointInfoWindow extends DefaultInfoWindow {

		int mSelectedPoint;

		public ViaPointInfoWindow(int layoutResId, MapView mapView) {
			super(layoutResId, mapView);

			Button btnDelete = (Button) (mView.findViewById(R.id.bubble_delete));
			btnDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					removePoint(mSelectedPoint);
					close();
				}
			});
		}

		@Override
		public void onOpen(ExtendedOverlayItem item) {
			mSelectedPoint = ((Integer) item.getRelatedObject()).intValue();
			super.onOpen(item);
		}

	}
}
