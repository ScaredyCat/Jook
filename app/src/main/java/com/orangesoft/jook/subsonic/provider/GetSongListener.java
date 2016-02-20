package com.orangesoft.jook.subsonic.provider;

import android.content.Context;
import android.media.MediaMetadata;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.model.ProviderConnection;
import com.orangesoft.jook.model.SongListener;
import com.orangesoft.jook.subsonic.GetStreamRequest;
import com.orangesoft.subsonic.Song;
import com.orangesoft.subsonic.command.GetSong;
import com.orangesoft.subsonic.command.Stream;

import java.util.HashMap;

/**
 * Copyright 2016 Orangesoft
 */
public class GetSongListener implements RequestListener<GetSong>
{
    private final String TAG = GetSongListener.class.getSimpleName();
    private Context context;
    private SongListener songListener;
    private ProviderConnection connection;

    public GetSongListener(Context context, SongListener listener, ProviderConnection connection)
    {
        this.context = context;
        this.songListener = listener;
        this.connection = connection;
    }

    @Override
    public void onRequestFailure(SpiceException spiceException)
    {
        Toast.makeText(context,
                "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestSuccess(GetSong getSong)
    {
        Song song = getSong.getSong();

        MediaMetadata metadata = new MediaMetadata.Builder().
                putString(MediaMetadata.METADATA_KEY_MEDIA_ID, song.getId()).
                putString(MediaMetadata.METADATA_KEY_TITLE, song.getTitle()).
                putString(MediaMetadata.METADATA_KEY_ALBUM, song.getAlbum()).
                putString(MediaMetadata.METADATA_KEY_ARTIST, song.getArtist()).
                putLong(MediaMetadata.METADATA_KEY_DURATION, song.getDuration()).
                putString(MediaMetadata.METADATA_KEY_GENRE, song.getGenre()).
                putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, song.getCoverArt()).
                putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, song.getTrack()).
                build();
        getStream(metadata);

    }

    private void getStream(MediaMetadata metadata)
    {
        connection.fetchStream(context, songListener, metadata);
    }
}
