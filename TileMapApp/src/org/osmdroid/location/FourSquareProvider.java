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
package org.osmdroid.location;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.osmdroid.utils.BonusPackHelper;

import android.util.Log;

public class FourSquareProvider {

	//	https://developer.foursquare.com/docs/venues/search
	//	https://developer.foursquare.com/docs/responses/venue
	//	https://apigee.com/console/foursquare

	protected String mApiKey;

	/**
	 * @param apiKey
	 *            the registered API key to give to Flickr service.
	 * @see "http://www.flickr.com/help/api/"
	 */
	public FourSquareProvider(String clientId, String clientSecret) {
		mApiKey = "client_id=" + clientId + "&client_secret=" + clientSecret;
	}

	//"https://api.foursquare.com/v2/venues/search?v=20120321&intent=checkin&ll=53.06,8.8&client_id=ZUN4ZMNZUFT3Z5QQZNMQ3ACPL4OJMBFGO15TYX51D5MHCIL3&client_secret=X1RXCVF4VVSG1Y2FUDQJLKQUC1WF4XXKIMK2STXKACLPDGLY
	private String getUrlInside(BoundingBox boundingBox, int maxResults) {
		StringBuffer url = new StringBuffer(
				"https://api.foursquare.com/v2/venues/search?v=20120321"
						+ "&intent=checkin"
						+ "&client_id=ZUN4ZMNZUFT3Z5QQZNMQ3ACPL4OJMBFGO15TYX51D5MHCIL3"
						+ "&client_secret=X1RXCVF4VVSG1Y2FUDQJLKQUC1WF4XXKIMK2STXKACLPDGLY"
						+ "&ll=");
		url.append(boundingBox.getMinLatitude());
		url.append(',');
		url.append(boundingBox.getMinLongitude());

		return url.toString();
	}

	/**
	 * @param fullUrl ...
	 * @return the list of POI
	 */
	public ArrayList<POI> getThem(String fullUrl) {
		// for local debug: fullUrl = "http://10.0.2.2/flickr_mockup.json";
		Log.d(BonusPackHelper.LOG_TAG, "FlickrPOIProvider:get:" + fullUrl);
		String jString = BonusPackHelper.requestStringFromUrl(fullUrl);
		if (jString == null) {
			Log.e(BonusPackHelper.LOG_TAG, "FlickrPOIProvider: request failed.");
			return null;
		}
		try {
			JSONObject jRoot = new JSONObject(jString);

			JSONObject jResponse = jRoot.getJSONObject("response");
			JSONArray jVenueArray = jResponse.getJSONArray("venues");
			int n = jVenueArray.length();
			ArrayList<POI> pois = new ArrayList<POI>(n);
			for (int i = 0; i < n; i++) {
				JSONObject jVenue = jVenueArray.getJSONObject(i);

				POI poi = new POI(POI.POI_SERVICE_4SQUARE);
				JSONObject jLocation = jVenue.getJSONObject("location");
				poi.location = new GeoPoint(
						jLocation.getDouble("lat"),
						jLocation.getDouble("lng"));

				poi.id = jVenue.getString("id");
				poi.type = jVenue.getString("name");

				//	poi.category = 
				//				poi.thumbnailPath = jVenue.getString("url_sq");
				//  String owner = jVenue.getString("owner");
				//	poi.url = "http://www.flickr.com/photos/" + owner + "/" + photoId;
				pois.add(poi);
			}
			//			int total = jPhotos.getInt("total");
			//			Log.d(BonusPackHelper.LOG_TAG, "done:" + n + " got, on a total of:" + total);
			return pois;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param boundingBox ...
	 * @param maxResults ...
	 * @return list of POI, Flickr photos inside the bounding box. Null if
	 *         technical issue.
	 */
	public ArrayList<POI> getPOIInside(BoundingBox boundingBox, int maxResults) {
		String url = getUrlInside(boundingBox, maxResults);
		return getThem(url);
	}
}
