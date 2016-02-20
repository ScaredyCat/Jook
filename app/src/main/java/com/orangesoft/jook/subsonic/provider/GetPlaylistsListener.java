package com.orangesoft.jook.subsonic.provider;

import android.content.Context;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.model.JookPlaylist;
import com.orangesoft.jook.model.PlaylistListener;
import com.orangesoft.jook.subsonic.SubsonicConnection;
import com.orangesoft.subsonic.Playlist;
import com.orangesoft.subsonic.command.GetPlaylists;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2016 Orangesoft
 */

public final class GetPlaylistsListener implements RequestListener<GetPlaylists>
{

    private final Context context;
    private final PlaylistListener listener;

    public GetPlaylistsListener(Context context, PlaylistListener listener)
    {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void onRequestFailure(SpiceException spiceException)
    {
        Toast.makeText(context,
                "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestSuccess(GetPlaylists result)
    {
        if (!result.getStatus())
        {
            Toast.makeText(context,
                    "Error: " + result.getFailureMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        List<JookPlaylist> jookPlaylists = new ArrayList<>();
        List<Playlist> playlists = result.getList();
        for (Playlist playlist : playlists)
        {
            JookPlaylist jookPlaylist = new JookPlaylist(playlist.getName(), playlist.
                    getSongCount(), SubsonicConnection.PROVIDER_NAME, playlist.getId());
            jookPlaylists.add(jookPlaylist);
        }

        listener.onPlaylists(jookPlaylists);
    }
}
