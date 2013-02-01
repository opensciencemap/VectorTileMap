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

import java.util.ArrayList;
import java.util.List;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.overlay.OverlayItem;
import org.oscim.view.MapView;
import org.osmdroid.location.FlickrPOIProvider;
import org.osmdroid.location.FourSquareProvider;
import org.osmdroid.location.GeoNamesPOIProvider;
import org.osmdroid.location.NominatimPOIProvider;
import org.osmdroid.location.POI;
import org.osmdroid.location.PicasaPOIProvider;
import org.osmdroid.overlays.DefaultInfoWindow;
import org.osmdroid.overlays.ExtendedOverlayItem;
import org.osmdroid.overlays.ItemizedOverlayWithBubble;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class POISearch {
	private final ArrayList<POI> mPOIs;
	ItemizedOverlayWithBubble<ExtendedOverlayItem> poiMarkers;
	final TileMap tileMap;
	Drawable[] mMarkers;

	private final static int MDEFAULT = 0;
	private final static int MFLICKR = 1;
	private final static int MPICASA = 2;
	private final static int MWIKI16 = 3;
	private final static int MWIKI32 = 4;

	POISearch(TileMap tileMap) {
		this.tileMap = tileMap;
		mPOIs = new ArrayList<POI>();
		//POI markers:
		final ArrayList<ExtendedOverlayItem> poiItems = new ArrayList<ExtendedOverlayItem>();

		poiMarkers = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(App.map, tileMap,
		poiItems, new POIInfoWindow(App.map));

		App.map.getOverlays().add(poiMarkers);

		//				if (savedInstanceState != null) {
		//					mPOIs = savedInstanceState.getParcelableArrayList("poi");
		//					updateUIWithPOI(mPOIs);
		//				}

		mMarkers = new Drawable[5];
		mMarkers[MDEFAULT] = App.res.getDrawable(R.drawable.marker_poi_default);
		mMarkers[MFLICKR] = App.res.getDrawable(R.drawable.marker_poi_flickr);
		mMarkers[MPICASA] = App.res.getDrawable(R.drawable.marker_poi_picasa_24);
		mMarkers[MWIKI16] = App.res.getDrawable(R.drawable.marker_poi_wikipedia_16);
		mMarkers[MWIKI32] = App.res.getDrawable(R.drawable.marker_poi_wikipedia_32);
	}

	public List<POI> getPOIs() {
		return mPOIs;
	}

	class POITask extends AsyncTask<Object, Void, List<POI>> {
		String mTag;

		@Override
		protected List<POI> doInBackground(Object... params) {
			mTag = (String) params[0];

			if (mTag == null || mTag.equals("")) {
				return null;
			} else if (mTag.equals("wikipedia")) {
				GeoNamesPOIProvider poiProvider = new GeoNamesPOIProvider("mkergall");
				//ArrayList<POI> pois = poiProvider.getPOICloseTo(point, 30, 20.0);
				//Get POI inside the bounding box of the current map view:
				BoundingBox bb = App.map.getBoundingBox();
				ArrayList<POI> pois = poiProvider.getPOIInside(bb, 30);
				return pois;
			} else if (mTag.equals("flickr")) {
				FlickrPOIProvider poiProvider = new FlickrPOIProvider(
				"c39be46304a6c6efda8bc066c185cd7e");
				BoundingBox bb = App.map.getBoundingBox();
				ArrayList<POI> pois = poiProvider.getPOIInside(bb, null, 20);
				return pois;
			} else if (mTag.startsWith("picasa")) {
				PicasaPOIProvider poiProvider = new PicasaPOIProvider(null);
				BoundingBox bb = App.map.getBoundingBox();
				String q = mTag.substring("picasa".length());
				List<POI> pois = poiProvider.getPOIInside(bb, q, 20);
				return pois;
			}
			else if (mTag.startsWith("foursquare")) {
				FourSquareProvider poiProvider = new FourSquareProvider(null, null);
				BoundingBox bb = App.map.getBoundingBox();
				String q = mTag.substring("foursquare".length());
				//				String q = mTag.substring("picasa".length());
				ArrayList<POI> pois = poiProvider.getPOIInside(bb, q, 40);
				return pois;
			}
			else {
				NominatimPOIProvider poiProvider = new NominatimPOIProvider();
				//	poiProvider.setService(NominatimPOIProvider.MAPQUEST_POI_SERVICE);
				poiProvider.setService(NominatimPOIProvider.NOMINATIM_POI_SERVICE);
				ArrayList<POI> pois;
				//				if (destinationPoint == null) {
				BoundingBox bb = App.map.getBoundingBox();
				pois = poiProvider.getPOIInside(bb, mTag, 10);
				
				//pois = poiProvider.getPOI( mTag, 10);
				//	} else {
				//		pois = poiProvider.getPOIAlong(mRoad.getRouteLow(), mTag, 100, 2.0);
				//	}
				return pois;
			}
		}

		@Override
		protected void onPostExecute(List<POI> pois) {
			if (mTag.equals("")) {
				//no search, no message
			} else if (pois == null) {
				Toast
				.makeText(tileMap.getApplicationContext(),
							"Technical issue when getting " + mTag + " POI.", Toast.LENGTH_LONG)
				.show();
			} else {
				Toast.makeText(tileMap.getApplicationContext(),
								"" + pois.size() + " " + mTag + " entries found",
								Toast.LENGTH_LONG).show();
				//	if (mTag.equals("flickr") || mTag.startsWith("picasa") || mTag.equals("wikipedia"))
				//	startAsyncThumbnailsLoading(mPOIs);
			}

			updateUIWithPOI(pois);
		}
	}

	void updateUIWithPOI(List<POI> pois) {
		mPOIs.clear();
		if (pois != null) {
			mPOIs.addAll(pois);

			for (POI poi : pois) {
				String desc = null;
				String name = null;

				if (poi.serviceId == POI.POI_SERVICE_NOMINATIM) {
					name = poi.description;
					String[] split = name.split(", ");
					if (split != null && split.length > 1) {
						name = split[0];
						desc = split[1];
 
						for (int i = 2; i < 3 && i < split.length; i++)
							desc += "," + split[i];
					}

				} else {
					desc = poi.description;
				}

				ExtendedOverlayItem poiMarker = new ExtendedOverlayItem(
				poi.type + (name == null ? "" : ": " + name), desc, poi.location);
				Drawable marker = null;

				if (poi.serviceId == POI.POI_SERVICE_NOMINATIM) {

					marker = mMarkers[MDEFAULT];
				} else if (poi.serviceId == POI.POI_SERVICE_GEONAMES_WIKIPEDIA) {
					if (poi.rank < 90)
						marker = mMarkers[MWIKI16];
					else
						marker = mMarkers[MWIKI32];
				} else if (poi.serviceId == POI.POI_SERVICE_FLICKR) {
					marker = mMarkers[MFLICKR];
				} else if (poi.serviceId == POI.POI_SERVICE_PICASA) {
					marker = mMarkers[MPICASA];
					poiMarker.setSubDescription(poi.category);
				} else if (poi.serviceId == POI.POI_SERVICE_4SQUARE) {
					marker = mMarkers[MDEFAULT];
					poiMarker.setSubDescription(poi.category);
				}

				poiMarker.setMarker(marker);
				poiMarker.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
				//thumbnail loading moved in POIInfoWindow.onOpen for better performances. 
				poiMarker.setRelatedObject(poi);
				poiMarkers.addItem(poiMarker);
			}

		}

		Intent intent = new Intent(tileMap.getApplicationContext(), POIActivity.class);
		intent.putExtra("ID", poiMarkers.getBubbledItemId());
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		tileMap.startActivityForResult(intent, TileMap.POIS_REQUEST);

		App.map.redrawMap(true);
	}

	void getPOIAsync(String tag) {
		poiMarkers.removeAllItems();
		new POITask().execute(tag);
	}

	class POIInfoWindow extends DefaultInfoWindow {

		private Button mButton;
		private ImageView mImage;

		public POIInfoWindow(MapView mapView) {
			super(R.layout.bonuspack_bubble, mapView);

			mButton = (Button) mView.findViewById(R.id.bubble_moreinfo);
			mImage = (ImageView) mView.findViewById(R.id.bubble_image);

			//bonuspack_bubble layouts already contain a "more info" button. 
			mButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					POI poi = (POI) view.getTag();

					if (poi == null)
						return;

					if (poi.serviceId == POI.POI_SERVICE_4SQUARE) {
						FourSquareProvider.browse(view.getContext(), poi);
					} else if (poi.url != null) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(poi.url));
						i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
						view.getContext().startActivity(i);
					}
				}
			});

			getView().setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					POI poi = (POI) view.getTag();

					if (poi != null) {
						Intent intent = new Intent(tileMap, POIActivity.class);
						intent.putExtra("ID", poiMarkers.getBubbledItemId());
						tileMap.startActivityForResult(intent, TileMap.POIS_REQUEST);
					}
				}
			});
		}

		@Override
		public void onOpen(ExtendedOverlayItem item) {
			POI poi = (POI) item.getRelatedObject();

			super.onOpen(item);

			poi.fetchThumbnail(mImage);

			//Show or hide "more info" button:
			if (poi.url != null)
				mButton.setVisibility(View.VISIBLE);
			else
				mButton.setVisibility(View.GONE);

			mButton.setTag(poi);
			getView().setTag(poi);
		}
	}

	void singleTapUp() {
		poiMarkers.hideBubble();
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_nearby:
			Intent intent = new Intent(tileMap, POIActivity.class);
			intent.putExtra("ID", poiMarkers.getBubbledItemId());
			tileMap.startActivityForResult(intent, TileMap.POIS_REQUEST);
			return true;
		default:
		}
		return false;

	}

}
