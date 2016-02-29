package com.orangesoft.jook.subsonic;

import android.content.Context;
import android.media.MediaMetadata;
import android.util.Base64;
import android.util.Log;

import com.orangesoft.jook.model.PlaylistListener;
import com.orangesoft.jook.model.ProviderConnection;
import com.orangesoft.jook.model.SongListener;
import com.orangesoft.jook.subsonic.provider.GetPlaylistListener;
import com.orangesoft.jook.subsonic.provider.GetPlaylistsListener;
import com.orangesoft.jook.subsonic.provider.GetSongListener;
import com.orangesoft.jook.subsonic.provider.StreamListener;
import com.orangesoft.subsonic.system.RestConnection;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2015 Orangesoft.
 */
public class SubsonicConnection extends ProviderConnection
{
    private static final String TAG = SubsonicConnection.class.getSimpleName();
    public static final String PROVIDER_NAME = "SUBSONIC";

    private RestConnection restConnection;

    public SubsonicConnection(Context context)
    {
        super(PROVIDER_NAME, context);
    }

    @Override
    public void fetchPlaylists(PlaylistListener listener)
    {
        GetPlaylistsRequest getPlaylistsRequest = new GetPlaylistsRequest(getConnection());
        sendRequest(getPlaylistsRequest, new GetPlaylistsListener(context, listener));
    }

    @Override
    public void fetchPlaylist(PlaylistListener listener, String playlistId)
    {
        Map<String, String> params = new HashMap<>();
        params.put("id", playlistId);
        GetPlaylistRequest getPlaylistRequest = new GetPlaylistRequest(getConnection(),
                params);
        sendRequest(getPlaylistRequest, new GetPlaylistListener(context, listener));
    }

    @Override
    public void fetchSong(SongListener listener, String songId)
    {
        Map<String, String> params = new HashMap<>();
        params.put("id", songId);
        GetSongRequest getSongRequest = new GetSongRequest(getConnection(), params);
        sendRequest(getSongRequest, new GetSongListener(context, listener, this));
    }

    @Override
    public void fetchStream(Context context, SongListener listener, MediaMetadata metadata)
    {
        Map<String, String> params = new HashMap<>();
        params.put("id", metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
        GetStreamRequest getStreamRequest = new GetStreamRequest(getConnection(), params);
        sendRequest(getStreamRequest, new StreamListener(context, listener, metadata));
    }

    public RestConnection getConnection() throws IllegalStateException
    {
        if (null == restConnection)
            initializeConnection();
        return restConnection;
    }

    private void initializeConnection() throws IllegalStateException
    {
        if (!checkConnectionParams())
            throw new IllegalStateException("Connection parameters have not been set.");
        String encodedAuth = getEncodedAuth(getUser(), getPassword());
        restConnection = new RestConnection(getHost(), encodedAuth);
    }

    private boolean checkConnectionParams()
    {
        return (getHost() != null && getHost().length() > 0 &&
                getUser() != null && getUser().length() > 0 &&
                getPassword() != null && getPassword().length() > 0);
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
            Log.e(TAG, "Bad encoding:", e);
        }
        return "";
    }
}
