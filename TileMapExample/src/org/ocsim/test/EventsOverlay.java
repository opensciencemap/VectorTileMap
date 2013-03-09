package org.ocsim.test;

import org.oscim.overlay.Overlay;
import org.oscim.view.MapView;

import android.util.Log;
import android.view.MotionEvent;

public class EventsOverlay extends Overlay {
	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		Log.d("app", e.toString());
		return false;
	}
}
