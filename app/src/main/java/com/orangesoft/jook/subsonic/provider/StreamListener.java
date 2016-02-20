package com.orangesoft.jook.subsonic.provider;

import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.model.MusicProvider;
import com.orangesoft.jook.model.SongListener;
import android.util.Log;
import com.orangesoft.subsonic.command.Stream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Copyright 2016 Orangesoft
 */
public class StreamListener implements RequestListener<Stream>
{
    private final static String TAG = StreamListener.class.getSimpleName();
    private SongListener songListener;
    private MediaMetadata mediaMetadata;
    private Context context;

    public StreamListener(Context context, SongListener listener, MediaMetadata metadata)
    {
        this.context = context;
        this.songListener = listener;
        this.mediaMetadata = metadata;
    }

    @Override
    public void onRequestFailure(SpiceException spiceException)
    {
        Toast.makeText(context,
                "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestSuccess(final Stream stream)
    {
        new AsyncTask<Void, Void, MediaMetadata>() {
            @Override
            protected MediaMetadata doInBackground(Void[] noids)
            {
                InputStream inputStream = stream.getStream();
                try

                {
                    FileOutputStream fos = context.openFileOutput(mediaMetadata.getString(
                            MediaMetadata.METADATA_KEY_TITLE) + ".mp3", Context.MODE_WORLD_READABLE);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    int len = 0;
                    //int offset = 0;
                    while ((len = inputStream.read(buffer)) != -1)
                    {
                        bos.write(buffer, 0, len);
                    }
                    inputStream.close();
                    bos.close();
                    String filename = context.getFileStreamPath(mediaMetadata.getString(
                            MediaMetadata.METADATA_KEY_TITLE)).getAbsolutePath() + ".mp3";
                    Log.i(TAG, "Fixin to stream " + filename);

                    MediaMetadata metadata = new MediaMetadata.Builder().
                            putString(MediaMetadata.METADATA_KEY_MEDIA_ID,
                                    mediaMetadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)).
                            putString(MediaMetadata.METADATA_KEY_TITLE, mediaMetadata.getString(
                                    MediaMetadata.METADATA_KEY_TITLE)).
                            putString(MediaMetadata.METADATA_KEY_ALBUM, mediaMetadata.getString(
                                    MediaMetadata.METADATA_KEY_ALBUM)).
                            putString(MediaMetadata.METADATA_KEY_ARTIST, mediaMetadata.getString(
                                    MediaMetadata.METADATA_KEY_ARTIST)).
                            putLong(MediaMetadata.METADATA_KEY_DURATION, mediaMetadata.getLong(
                                    MediaMetadata.METADATA_KEY_DURATION)).
                            putString(MediaMetadata.METADATA_KEY_GENRE, mediaMetadata.getString(
                                    MediaMetadata.METADATA_KEY_GENRE)).
                            putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, mediaMetadata.getString(
                                    MediaMetadata.METADATA_KEY_ALBUM_ART_URI)).
                            putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, mediaMetadata.getLong(
                                    MediaMetadata.METADATA_KEY_TRACK_NUMBER)).
                            putString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE, filename).
                            build();
                    return metadata;
                } catch (Exception e)
                {
                    Log.e(TAG, "Exception capturing stream", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(MediaMetadata metadata)
            {
                if (metadata != null)
                    songListener.onSongMetadata(metadata);
            }
        }.execute();
    }
}
