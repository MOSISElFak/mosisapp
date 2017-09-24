package com.demo.mosisapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

@IgnoreExtraProperties
public class PlaceBean
{
    private Double platitude;
    private Double plongitude;
    private String type;
    private String attribute;
    private Long date;
    private int points;
    private String author;

    public Double getPlatitude() {
        return platitude;
    }

    public void setPlatitude(Double platitude) {
        this.platitude = platitude;
    }

    public Double getPlongitude() {
        return plongitude;
    }

    public void setPlongitude(Double plongitude) {
        this.plongitude = plongitude;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public PlaceBean() {}

    // This is for creating Place, server should fill in the rest of the fields
    public PlaceBean(Double platitude, Double plongitude, String type, String attribute) {
        this.platitude = platitude;
        this.plongitude = plongitude;
        this.type = type;
        this.attribute = attribute;
    }

    // Date is deprecated
    public Calendar simpleDate(){
        Calendar cal = Calendar.getInstance();//now
        cal.setTimeInMillis(getDate());
        cal.getDisplayName(Calendar.DATE, Calendar.SHORT, Locale.ENGLISH);
        return cal;
        //SimpleDateFormat fmt = new SimpleDateFormat("dd MM yyyy",Locale.getDefault());
        //fmt.format(cal.getTime()); //This returns a string formatted in the above way.
    }

    protected LatLng location(){
        return new LatLng(getPlatitude(),getPlongitude());
    }
}
