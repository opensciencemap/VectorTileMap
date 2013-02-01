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
package org.osmdroid.overlays;

import java.util.HashMap;
import java.util.List;

import org.oscim.app.R;
import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.overlay.OverlayItem;
import org.oscim.view.MapView;
import org.osmdroid.location.FlickrPOIProvider;
import org.osmdroid.location.POI;
import org.osmdroid.location.POIProvider;
import org.osmdroid.utils.BonusPackHelper;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class POIOverlay extends ItemizedOverlayWithBubble<ExtendedOverlayItem> {

	POIProvider mPoiProvider;
	UpdateTask mUpdateTask;
	boolean mTaskRunning;
	Drawable mMarker;
	BoundingBox mBoundingBox;

	public POIOverlay(MapView mapView, Context context, List<ExtendedOverlayItem> aList,
			InfoWindow bubble) {
		super(mapView, context, aList, bubble);

		mUpdateTask = new UpdateTask();
		FlickrPOIProvider provider = new FlickrPOIProvider("c39be46304a6c6efda8bc066c185cd7e");
		provider.setPrevious(mPOIMap);

		mPoiProvider = provider;
		mMarker = context.getResources().getDrawable(R.drawable.marker_poi_flickr);
	}

	public void setPoiProvider(POIProvider poiProvider) {
		mPoiProvider = poiProvider;
	}

	@Override
	public void onUpdate(MapPosition mapPosition, boolean changed) {
		super.onUpdate(mapPosition, changed);

		if (changed && !mTaskRunning) {
			mMapView.postDelayed(mUpdateTask, 1000);
			mTaskRunning = true;
		}
	}

	class UpdateTask implements Runnable {

		@Override
		public void run() {
			mTaskRunning = false;

			BoundingBox bb = mMapView.getBoundingBox();

			if (mBoundingBox == null || !mBoundingBox.equals(bb)) {
				//				synchronized (mBoundingBox) {
				mBoundingBox = bb;
				//				}

				// check bounding box
				Log.d(BonusPackHelper.LOG_TAG, " update pois");

				new POITask().execute();
			}
		}
	}

	HashMap<String, POI> mPOIMap = new HashMap<String, POI>(100);

	class POITask extends AsyncTask<Object, Void, List<POI>> {

		@Override
		protected List<POI> doInBackground(Object... params) {

			return mPoiProvider.getPOIInside(mBoundingBox, "", 20);
		}

		@Override
		protected void onPostExecute(List<POI> pois) {
			//			removeAllItems();

			if (pois != null) {
				for (POI poi : pois) {
					ExtendedOverlayItem poiMarker = new ExtendedOverlayItem(poi.type,
							poi.description, poi.location);

					poiMarker.setMarker(mMarker);
					poiMarker.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
					//thumbnail loading moved in POIInfoWindow.onOpen for better performances. 
					poiMarker.setRelatedObject(poi);

					addItem(poiMarker);

					mPOIMap.put(poi.id, poi);
				}
			}

			mMapView.redrawMap(true);
		}
	}

	public static class POIInfoWindow extends DefaultInfoWindow {

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

					if (poi != null && poi.url != null) {
						Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(poi.url));
						view.getContext().startActivity(myIntent);
					}
				}
			});

			//			getView().setOnClickListener(new View.OnClickListener() {
			//				@Override
			//				public void onClick(View view) {
			//					POI poi = (POI) view.getTag();
			//
			//					if (poi != null) {
			//						Intent intent = new Intent(tileMap, POIActivity.class);
			//						intent.putExtra("ID", poiMarkers.getBubbledItemId());
			//						tileMap.startActivityForResult(intent, TileMap.POIS_REQUEST);
			//					}
			//				}
			//			});
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

}
