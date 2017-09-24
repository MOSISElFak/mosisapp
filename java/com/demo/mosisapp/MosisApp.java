package com.demo.mosisapp;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class MosisApp extends Application
{
    private static final MosisApp ourInstance = new MosisApp();
    protected boolean logoutFlag = false;
    protected int RC_NOTIFICATIONS = 70;

    public static MosisApp getInstance() {
        return ourInstance;
    }

    //private MosisApp() {}

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     * Implementations should be as quick as possible (for example using
     * lazy initialization of state) since the time spent in this function
     * directly impacts the performance of starting the first activity,
     * service, or receiver in a process.
     * If you override this method, be sure to call super.onCreate().
     */
    @Override
    public void onCreate() {
        super.onCreate();
        //only caches data when there is a Listener attached to that node (when the data has been read at least once).
        if (!FirebaseApp.getApps(this).isEmpty())
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    public int getNotifRC(){
        RC_NOTIFICATIONS++;
        return RC_NOTIFICATIONS;
    }
}
