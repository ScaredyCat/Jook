/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Work originally authored by
 *
 *  Copyright 2014 The Android Open Source Project, Inc.
 *
 * under the android-UniversalMusicPlayer project
 */
package com.orangesoft.jook.ui.tv;


import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orangesoft.jook.QueueHelper;
import com.orangesoft.jook.R;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Browse media categories and current playing queue.
 * <p/>
 * WARNING: This sample's UI is implemented for a specific MediaBrowser tree structure.
 * It expects a tree that is three levels deep under root:
 * - level 0: root
 * - level 1: categories of categories (like "by genre", "by artist", "playlists")
 * - level 2: song categories (like "by genre -> Rock", "by  artist -> artistname" or
 * "playlists -> my favorite music")
 * - level 3: the actual music
 * <p/>
 * If you are reusing this TV code, make sure you adapt it to your MediaBrowser structure, in case
 * it is not the same.
 * <p/>
 * <p/>
 * It uses a {@link MediaBrowser} to connect to the {@link com.orangesoft.jook.MusicService}.
 * Once connected, the fragment subscribes to get the children of level 1 and then, for each
 * children, it adds a ListRow and subscribes for its children, which, when received, are
 * added to the ListRow. These items (like "Rock"), when clicked, will open a
 * TvVerticalGridActivity that lists all songs of the specified category on a grid-like UI.
 * <p/>
 * This fragment also shows the MediaSession queue ("now playing" list), in case there is
 * something playing.
 */
public class TvBrowseFragment extends BrowseFragment
{
    private static final String TAG = TvBrowseFragment.class.getSimpleName();

    private ArrayObjectAdapter rowsAdapter;
    private ArrayObjectAdapter listRowAdapter;
    private MediaFragmentListener mediaFragmentListener;

    private MediaBrowser mediaBrowser;
    private HashSet<String> subscribedMediaIds;

    // Receive callbacks from the MediaController.  Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaController.Callback mediaControllerCallback = new MediaController.Callback()
    {
        @Override
        public void onMetadataChanged(MediaMetadata metadata)
        {
            if (null != metadata)
            {
                MediaController mediaController = getActivity().getMediaController();
                updateQueue(mediaController.getQueue());
            }
        }
        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue)
        {
           updateQueue(queue);
        }

        private void updateQueue(List<MediaSession.QueueItem> queue)
        {
            MediaController mediaController = getActivity().getMediaController();
            long activeQueueId;
            if (null == mediaController.getPlaybackState())
                activeQueueId = MediaSession.QueueItem.UNKNOWN_ID;
            else
                activeQueueId = mediaController.getPlaybackState().getActiveQueueItemId();
            updateNowPlayingList(queue, activeQueueId);
            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
        }
    };

    private void updateNowPlayingList(List<MediaSession.QueueItem> queue, long activeQueueId)
    {
        listRowAdapter.clear();
        if (MediaSession.QueueItem.UNKNOWN_ID != activeQueueId)
        {
            Iterator<MediaSession.QueueItem> iterator = queue.iterator();
            while (iterator.hasNext())
            {
                MediaSession.QueueItem queueItem = iterator.next();
                if (activeQueueId != queueItem.getQueueId())
                    iterator.remove();
                else
                    break;
            }
        }
        listRowAdapter.addAll(0, queue);
    }

    private final MediaBrowser.SubscriptionCallback subscriptionCallback = new MediaBrowser.
            SubscriptionCallback()
    {
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowser.MediaItem> children)
        {
            rowsAdapter.clear();
            CardPresenter cardPresenter = new CardPresenter();

            for (int i=0; i < children.size(); i++)
            {
                MediaBrowser.MediaItem item = children.get(i);
                String title = item.getDescription().getTitle().toString();
                HeaderItem header = new HeaderItem(i, title);
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                rowsAdapter.add(new ListRow(header, listRowAdapter));

                if (item.isPlayable())
                    listRowAdapter.add(item);
                else if (item.isBrowsable())
                    subscribeToMediaId(item.getMediaId(), new
                            RowSubscriptionCallback(listRowAdapter));
                else
                    Log.e(TAG, "Item should be playable or browseable.");
            }

            MediaController mediaController = getActivity().getMediaController();

            if (null != mediaController.getQueue() && !mediaController.getQueue().isEmpty())
            {
                // add Now Playing queue to Browse Home
                HeaderItem header = new HeaderItem(children.size(), getString(R.string.now_playing));
                listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                rowsAdapter.add(new ListRow(header, listRowAdapter));
                long activeQueueId;
                if (null == mediaController.getPlaybackState())
                    activeQueueId = MediaSession.QueueItem.UNKNOWN_ID;
                else
                    activeQueueId = mediaController.getPlaybackState().getActiveQueueItemId();
                updateNowPlayingList(mediaController.getQueue(), activeQueueId);
            }

            rowsAdapter.notifyArrayItemRangeChanged(0, children.size());
        }

        @Override
        public void onError(@NonNull String id)
        {
            Log.e(TAG, "SubscriptionCallback subscription onError, id=" + id);
        }
    };

    /**
     * This callback fills content for a single Row in the BrowseFragment
     */
    private class RowSubscriptionCallback extends MediaBrowser.SubscriptionCallback
    {
        private final ArrayObjectAdapter listRowAdapter;

        public RowSubscriptionCallback(ArrayObjectAdapter listRowAdapter)
        {
            this.listRowAdapter = listRowAdapter;
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowser.MediaItem> children)
        {
            listRowAdapter.clear();
            for (MediaBrowser.MediaItem item :children)
                listRowAdapter.add(item);
            listRowAdapter.notifyArrayItemRangeChanged(0, children.size());
        }

        @Override
        public void onError(@NonNull String id)
        {
            Log.e(TAG, "RowSubscriptionCallback subscription onError, id=" + id);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG, "onActivityCreated");

        subscribedMediaIds = new HashSet<>();

        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.tv_search_button));

        loadRows();
        setupEventListeners();
    }

    private void loadRows()
    {
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(rowsAdapter);
    }

    private void setupEventListeners()
    {
        setOnItemViewClickedListener(new OnItemViewClickedListener()
        {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row)
            {
                if (o instanceof MediaBrowser.MediaItem)
                {
                    MediaBrowser.MediaItem item = (MediaBrowser.MediaItem) o;
                    if (item.isPlayable())
                    {
                        Log.w(TAG, "Ignoring click on PLAYABLE MediaItem in TvBrowseFragment. mediaId="
                            + item.getMediaId());
                        return;
                    }
                    Intent intent = new Intent(getActivity(), TvVerticalGridActivity.class);
                    intent.putExtra(TvBrowseActivity.SAVED_MEDIA_ID, item.getMediaId());
                    intent.putExtra(TvBrowseActivity.BROWSE_TITLE, item.getDescription().getTitle());
                    startActivity(intent);
                }
                else if (o instanceof MediaSession.QueueItem)
                {
                    MediaSession.QueueItem item = (MediaSession.QueueItem) o;
                    getActivity().getMediaController().getTransportControls().skipToQueueItem(
                            item.getQueueId());
                    Intent intent = new Intent(getActivity(), TvPlaybackActivity.class);
                    startActivity(intent);
                }
            }
        });

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "In-app search");
                // TODO: implement in-app search
                Intent intent = new Intent(getActivity(), TvBrowseActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            mediaFragmentListener = (MediaFragmentListener) activity;
        }
        catch (ClassCastException e)
        {
            Log.e(TAG,
                    "TVBrowseFragment can only be attached to an activity that implements MediaFragmentListener",
                    e);
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mediaBrowser != null && mediaBrowser.isConnected())
        {
            for (String mediaId : subscribedMediaIds)
                mediaBrowser.unsubscribe(mediaId);
            subscribedMediaIds.clear();
        }
        if (getActivity().getMediaController() != null)
            getActivity().getMediaController().unregisterCallback(mediaControllerCallback);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mediaFragmentListener = null;
    }

    public void initializeWithMediaId(String mediaId)
    {
        Log.v(TAG, "subscribeToData");
        // fetch browsing information to fill the listview
        mediaBrowser = mediaFragmentListener.getMediaBrowser();

        if (null == mediaId)
            mediaId = mediaBrowser.getRoot();

        subscribeToMediaId(mediaId, subscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
        if (getActivity().getMediaController() != null)
            getActivity().getMediaController().registerCallback(mediaControllerCallback);
    }

    private void subscribeToMediaId(String mediaId, MediaBrowser.SubscriptionCallback callback)
    {
        if (subscribedMediaIds.contains(mediaId))
            mediaBrowser.unsubscribe(mediaId);
        else
            subscribedMediaIds.add(mediaId);
        mediaBrowser.subscribe(mediaId, callback);
    }

    public interface MediaFragmentListener
    {
        MediaBrowser getMediaBrowser();
    }
}
