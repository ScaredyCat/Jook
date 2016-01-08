package com.orangesoft.jook.subsonic;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Base64;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.UncachedSpiceService;
import com.octo.android.robospice.request.SpiceRequest;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.subsonic.system.RestConnection;

import java.io.UnsupportedEncodingException;

/**
 * Copyright 2015 Orangesoft.
 */
public class SubsonicConnection
{
    private Activity activity;
    private static final String JOOK_PREFS = "JookPrefs";
    private static final String SUBSONIC_HOST = "SubsonicHost";
    private static final String SUBSONIC_USER = "SubsonicUser";
    private static final String SUBSONIC_PASSWORD = "SubsonicPassword";
    private RestConnection restConnection;
    private SpiceManager spiceManager = new SpiceManager(UncachedSpiceService.class);

    public SubsonicConnection(Activity activity)
    {
        this.activity = activity;
        spiceManager.start(activity);
    }

    public void saveConnectionDetails( String host, String user, String password )
    {

        SharedPreferences settings = activity.getSharedPreferences(JOOK_PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SUBSONIC_HOST, host);
        editor.putString(SUBSONIC_USER, user);
        editor.putString(SUBSONIC_PASSWORD, password);
        editor.apply();
    }

    public String getSubsonicHost()
    {
        SharedPreferences settings = activity.getSharedPreferences(JOOK_PREFS, 0);
        return settings.getString(SUBSONIC_HOST, "");
    }

    public String getSubsonicUser()
    {
        SharedPreferences settings = activity.getSharedPreferences(JOOK_PREFS, 0);
        return settings.getString(SUBSONIC_USER, "");
    }

    public String getSubsonicPassword()
    {
        SharedPreferences settings = activity.getSharedPreferences(JOOK_PREFS, 0);
        return  settings.getString(SUBSONIC_PASSWORD, "");
    }

    public RestConnection getConnection()
    {
        if (null == restConnection)
            initializeConnection();
        return restConnection;
    }

    @SuppressWarnings("unchecked")
    public void sendRequest(SpiceRequest request, RequestListener listener)
    {
        spiceManager.execute(request, listener);
    }

    public void close()
    {
        spiceManager.shouldStop();
    }

    private void initializeConnection()
    {
        String encodedAuth = getEncodedAuth(getSubsonicUser(), getSubsonicPassword());
        restConnection = new RestConnection(getSubsonicHost(), encodedAuth);
    }

    private static String getEncodedAuth(String user, String password)
    {
        try
        {
            byte[] data = (user + ":" + password).getBytes("UTF-8");
            return Base64.encodeToString(data, Base64.DEFAULT);
        }
        catch (UnsupportedEncodingException e)
        {
            // Print out some kind of error thingy
        }
        return "";
    }
}
