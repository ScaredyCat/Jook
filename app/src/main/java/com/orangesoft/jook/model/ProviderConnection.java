package com.orangesoft.jook.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadata;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.request.SpiceRequest;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.spice.SimpleSpiceService;
import com.orangesoft.subsonic.Song;

/**
 * Copyright 2016 Orangesoft
 */
public abstract class ProviderConnection
{
    protected Context context;
    private static final String JOOK_PREFS = "JookPrefs";
    private SpiceManager spiceManager = new SpiceManager(SimpleSpiceService.class);
    private final String hostkey;
    private final String userkey;
    private final String passwordkey;

    protected ProviderConnection(String providerName, Context context)
    {
        this.context = context;
        spiceManager.start(context);
        hostkey = providerName + ":host";
        userkey = providerName + ":user";
        passwordkey = providerName + ":password";
    }

    public void saveConnectionDetails( String host, String user, String password )
    {
        SharedPreferences settings = context.getSharedPreferences(JOOK_PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(hostkey, host);
        editor.putString(userkey, user);
        editor.putString(passwordkey, password);
        editor.apply();
    }

    public String getHost()
    {
        SharedPreferences settings = context.getSharedPreferences(JOOK_PREFS, 0);
        return settings.getString(hostkey, "");
    }

    public String getUser()
    {
        SharedPreferences settings = context.getSharedPreferences(JOOK_PREFS, 0);
        return settings.getString(userkey, "");
    }

    public String getPassword()
    {
        SharedPreferences settings = context.getSharedPreferences(JOOK_PREFS, 0);
        return  settings.getString(passwordkey, "");
    }

    public abstract void fetchPlaylists(PlaylistListener listener);
    public abstract void fetchPlaylist(PlaylistListener listener, String playlistId);
    public abstract void fetchSong(SongListener listener, String songId);
    public abstract void fetchStream(Context context, SongListener listener, MediaMetadata metadata);

    @SuppressWarnings("unchecked")
    public void sendRequest(SpiceRequest request, RequestListener listener)
    {
        spiceManager.execute(request, listener);
    }

    public void close()
    {
        spiceManager.shouldStop();
    }
}
