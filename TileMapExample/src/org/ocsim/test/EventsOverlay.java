package org.ocsim.test;

import org.oscim.overlay.Overlay;
import org.oscim.view.MapView;

import android.util.Log;
import android.view.MotionEvent;

public class EventsOverlay extends Overlay {
	public EventsOverlay(MapView mapView) {
		super(mapView);
	}
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		Log.d("app", e.toString());
		return false; 
	}
}
