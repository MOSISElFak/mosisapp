package com.demo.mosisapp;

import com.google.android.gms.maps.model.LatLng;
import java.util.HashMap;

/*
* Hard codded Geofences from Udacity Google course
*/
public final class GeofencesConstants
{
    private GeofencesConstants() {
    }

    //public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 2 * 60 * 1000; // 2 minutes
    //public static final float GEOFENCE_RADIUS_IN_METERS = 10f;

    public static final HashMap<String,LatLng> BAY_AREA_LANDMARKS = new HashMap<String, LatLng>();
    static {
        // San Francisco International Airport.
        BAY_AREA_LANDMARKS.put("Irish Pub", new LatLng(48.1381126,11.5785943));

        // Googleplex.
        BAY_AREA_LANDMARKS.put("GOOGLE", new LatLng(48.1386469,11.5773091));

        // Test
        BAY_AREA_LANDMARKS.put("Peterskirche", new LatLng(48.1370821,11.5752764));
    }
}
