package com.demo.mosisapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

// The class must have a default constructor that takes no arguments.
// The class must define public getters for the properties to be assigned.
public class ProfileBean implements Serializable
{
    //beans
    private String photoUrl;
    private String username;
    private String name;
    private String lastName;
    private String phone;
    private String reserve;

    //ctor
    public ProfileBean() {}

    public ProfileBean(String photoUrl, String username, String name, String lastName, String phone, String reserve) {
        this.photoUrl = photoUrl;
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.reserve = reserve;
    }

    //get set
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getReserve() {
        return reserve;
    }

    public void setReserve(String reserve) {
        this.reserve = reserve;
    }
}
