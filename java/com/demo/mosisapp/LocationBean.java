package com.demo.mosisapp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationBean
{
    //TODO timestamp: is there an universal timestamp or is it locale dependant (time-zones)
    private Date timeStamp; //String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    private Double lat; // -90S ||| +90N
    private Double lon; // -180W --- +180E

    public LocationBean() {}
    /*
    public LocationBean(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
    }*/

    public LocationBean(Date timeStamp, Double lat, Double lon) {

        this.timeStamp = timeStamp;
        this.lat = lat;
        this.lon = lon;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
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
}
