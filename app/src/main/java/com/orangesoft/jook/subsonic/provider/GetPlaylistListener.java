package com.orangesoft.jook.subsonic.provider;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadata;
import android.util.Log;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.model.JookPlaylist;
import com.orangesoft.jook.model.PlaylistListener;
import com.orangesoft.jook.subsonic.SubsonicConnection;
import com.orangesoft.jook.subsonic.model.JookEntry;
import com.orangesoft.subsonic.Entry;
import com.orangesoft.subsonic.Playlist;
import com.orangesoft.subsonic.command.GetPlaylist;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2016 Orangesoft
 */
public final class GetPlaylistListener implements RequestListener<GetPlaylist>
{
    private final static String TAG = GetPlaylistListener.class.getSimpleName();
    private final Context context;
    private final PlaylistListener listener;

    public GetPlaylistListener(Context context, PlaylistListener listener)
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
    public void onRequestSuccess(GetPlaylist result)
    {
        Playlist playlist = result.getPlaylist();
        final List<Entry> entries = playlist.getEntries();
        ArrayList<MediaMetadata> media = new ArrayList<>();
        for (Entry entry : entries)
        {
            try
            {
                MediaMetadata item = new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, entry.getId())
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, entry.getAlbum())
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, entry.getArtist())
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, entry.getDuration())
                        .putString(MediaMetadata.METADATA_KEY_GENRE, entry.getGenre())
                        .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, entry.getCoverArt())
                        .putString(MediaMetadata.METADATA_KEY_TITLE, entry.getTitle())
                        .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, entry.getTrack())
                        .build();
                media.add(item);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Unable to parse entry: ", e);
            }
        }
        JookPlaylist jookPlaylist = new JookPlaylist(playlist.getName(), playlist.getSongCount(),
                SubsonicConnection.PROVIDER_NAME, playlist.getId());
        listener.onPlaylist(jookPlaylist, media);
    }
}
