package com.demo.mosisapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.ServerValue;
import java.text.SimpleDateFormat;

import java.util.Date;

public class LocationBean
{
    //TODO timestamp: is there an universal timestamp or is it locale dependant (time-zones)
    //private Date timeStamp; //String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    private Double lat; // -90S ||| +90N
    private Double lon; // -180W --- +180E

    public LocationBean() {}

    public LocationBean(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public LatLng makeCoordinates() {
        return new LatLng(getLat(),getLon());
    }

    @Override
    public String toString() {
        return getLat().toString() +".."+ getLon().toString();
    }
}
