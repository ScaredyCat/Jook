package com.orangesoft.jook.spice;

import android.app.Application;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.binary.InFileBitmapObjectPersister;
import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.string.InFileStringObjectPersister;

/**
 * Copyright 2016 Orangesoft
 */
public class SimpleSpiceService extends SpiceService
{
    final static String TAG = "SimpleSpiceService";
    final static int THREAD_COUNT = 3;

    @Override
    public CacheManager createCacheManager(Application application) throws CacheCreationException
    {
        CacheManager cacheManager = new CacheManager();

        InFileStringObjectPersister inFileStringObjectPersister = new InFileStringObjectPersister(
                application);
        InFileBitmapObjectPersister inFileBitmapObjectPersister = new InFileBitmapObjectPersister(
                application);

        cacheManager.addPersister(inFileBitmapObjectPersister);
        cacheManager.addPersister(inFileStringObjectPersister);
        return cacheManager;
    }

    @Override
    public int getThreadCount()
    {
        return THREAD_COUNT;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        super.onStart(intent, startId);
        Log.v(TAG, "Starting service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.v(TAG, "Starting service");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.v(TAG, "Stopping service");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.v(TAG, "Bound service");
        return super.onBind(intent);
    }

    @Override
    public void onRebind(Intent intent)
    {
        Log.v(TAG, "Rebound service");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.v(TAG, "Unbound service");
        return super.onUnbind(intent);
    }
}
