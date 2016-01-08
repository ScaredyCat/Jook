package com.orangesoft.jook.subsonic;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Copyright 2016 Orangesoft
 */
public abstract class SubsonicBaseActivity extends AppCompatActivity
{
    public static final String TAG = "SubsonicBaseActivity";
    public SubsonicConnection connection;

    @Override
    public void onStart()
    {
        Log.v(TAG, "In onStart");
        super.onStart();
        connection = new SubsonicConnection(this);
        fetchData();
    }

    @Override
    public void onStop()
    {
        Log.v(TAG, "In onStop");
        connection.close();
        super.onStop();
    }

    public abstract void fetchData();
}
