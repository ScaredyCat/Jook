package com.orangesoft.jook;

import android.media.MediaMetadata;
import android.media.session.MediaSession;

import com.orangesoft.jook.model.MusicProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2016 Orangesoft
 */
public class QueueHelper
{
    private static final String TAG = QueueHelper.class.getSimpleName();

    public static List<MediaSession.QueueItem> getPlayingQueue(String mediaId,
                                                               MusicProvider musicProvider)
    {
        return convertToQueue(musicProvider.getTracksSync(mediaId));
    }

    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue, String mediaId)
    {
        int index = 0;
        for (MediaSession.QueueItem item : queue)
        {
            if (mediaId.equals(item.getDescription().getMediaId()))
                return index;
            index++;
        }
        return -1;
    }

    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue, long queueId)
    {
        int index = 0;
        for (MediaSession.QueueItem item : queue)
        {
            if (queueId == item.getQueueId())
                return index;
            index++;
        }
        return -1;
    }

    public static boolean isIndexPlayable(int index, List<MediaSession.QueueItem> queue)
    {
        return (queue != null && index >= 0 && index < queue.size());
    }

    public static List<MediaSession.QueueItem> createQueueFromMetadata( List<MediaMetadata> tracks)
    {
        return convertToQueue(tracks);
    }

    private static List<MediaSession.QueueItem> convertToQueue( Iterable<MediaMetadata> tracks )
    {
        List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadata track : tracks)
        {
            MediaSession.QueueItem item = new MediaSession.QueueItem(track.getDescription(), count++);
            queue.add(item);
        }
        return queue;
    }
}
