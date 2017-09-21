package com.demo.mosisapp;

/**
 * Defines several constants
 * Also names for database
 */
public interface Constants
{
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public final String DEVICE_NAME = "device_name";
    public final String TOAST = "toast";
    static final String DENIED = "DEN";
    static final String ACCEPTED = "ACC";
    static final String AREFRIENDS = "ARE";

    // Database nodes names
    public final String USERS = "users";
    public final String FRIENDS = "friends";
    public final String LOCATIONS = "location";
    public final String REQUESTS = "requests";
    public final String COORDINATES = "coordinates";
    public final String NOTIFICATION = "notification";
    public final String ADDPLACE = "places/original";
    public final String READPLACE = "places/complete";

    // Geofence constants
    long GEO_DURATION = 2 * 60 * 1000; //in miliseconds
    float GEOFENCE_RADIUS = 100.0f; // in meters
}
