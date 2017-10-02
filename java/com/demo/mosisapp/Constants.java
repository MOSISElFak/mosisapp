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

    // Search constants
    String ALL = "all";
    String TYPE = "type";
    String ATTRIBUTE = "attribute";
    String DATE = "date";

    // Request codes
    int RC_CAMERA = 22;
    int RC_LOCATION = 23;
    int RC_CHECK_LOCATION = 55;

    // Geofence constants
    public static final long GEO_DURATION = 5 * 60 * 1000; //in miliseconds
    public static final float GEOFENCE_RADIUS = 100.0f; // in meters
    public static final int GEOFENCE_REQ_CODE = 16;

    int RECEIVER_KEY_NEW_LOCATION = 103;
    String RECEIVER_NEW_LOCATION = "location";
}
