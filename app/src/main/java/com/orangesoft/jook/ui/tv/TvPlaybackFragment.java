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

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.util.Log;

import com.orangesoft.jook.AlbumArtCache;

import java.util.List;

/**
 * Show details of the currently playing song, along with playback controls and the playing queue.
 */
public class TvPlaybackFragment extends PlaybackOverlayFragment
{
    private static final String TAG = TvPlaybackFragment.class.getSimpleName();

    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_DARK;
    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int UPDATE_PERIOD = 16;

    private ArrayObjectAdapter rowsAdapter;
    private ArrayObjectAdapter primaryActionsAdapter;
    protected PlaybackControlsRow.PlayPauseAction playPauseAction;
    private PlaybackControlsRow.SkipNextAction skipNextAction;
    private PlaybackControlsRow.SkipPreviousAction skipPreviousAction;
    private PlaybackControlsRow playbackControlsRow;
    private List<MediaSession.QueueItem> playlistQueue;
    private int duration;
    private Handler handler;
    private Runnable runnable;

    private long lastPosition;
    private long lastPositionUpdateTime;

    private BackgroundManager backgroundManager;
    private ArrayObjectAdapter listRowAdapter;
    private ListRow listRow;

    private ClassPresenterSelector presenterSelector;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        handler = new Handler();
        listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        presenterSelector = new ClassPresenterSelector();
        rowsAdapter = new ArrayObjectAdapter(presenterSelector);

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);
    }

    private void initializePlaybackControls(MediaMetadata metadata)
    {
        setupRows();
        addPlaybackControlsRow(metadata);
        setAdapter(rowsAdapter);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void setupRows()
    {
        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        playbackControlsRowPresenter = new PlaybackControlsRowPresenter(new DescriptionPresenter());
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (null == getActivity() || null == getActivity().getMediaController())
                    return;
                MediaController.TransportControls controls = getActivity().getMediaController().
                        getTransportControls();
                if (action.getId() == playPauseAction.getId()) {
                    if (playPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.PLAY)
                        controls.play();
                    else
                        controls.pause();
                } else if (action.getId() == skipNextAction.getId()) {
                    controls.skipToNext();
                    resetPlaybackRow();
                } else if (action.getId() == skipPreviousAction.getId()) {
                    controls.skipToPrevious();
                    resetPlaybackRow();
                }

                if (action instanceof PlaybackControlsRow.MultiAction) {
                    ((PlaybackControlsRow.MultiAction) action).nextIndex();
                    notifyChanged(action);
                }
            }
        });

        presenterSelector.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
    }

    private void addPlaybackControlsRow(MediaMetadata metadata)
    {
        playbackControlsRow = new PlaybackControlsRow(new MutableMediaMetadataHolder(metadata));
        rowsAdapter.add(playbackControlsRow);

        resetPlaybackRow();

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        primaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        playbackControlsRow.setPrimaryActionsAdapter(primaryActionsAdapter);

        playPauseAction = new PlaybackControlsRow.PlayPauseAction(getActivity());
        skipNextAction = new PlaybackControlsRow.SkipNextAction(getActivity());
        skipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(getActivity());

        primaryActionsAdapter.add(skipPreviousAction);
        primaryActionsAdapter.add(playPauseAction);
        primaryActionsAdapter.add(skipNextAction);
    }

    private boolean equalsQueue(List<MediaSession.QueueItem> list1,
                             List<MediaSession.QueueItem> list2)
    {
        if (list1 == list2)
            return true;

        if (null == list1 || null == list2)
            return false;

        if (list1.size() != list2.size())
            return false;

        for (int i=0; i<list1.size(); i++)
        {
            if (list1.get(i).getQueueId() != list2.get(i).getQueueId())
                return false;
            if (!TextUtils.equals(list1.get(i).getDescription().getMediaId(),
                    list2.get(i).getDescription().getMediaId()))
                return false;
        }
        return true;
    }

    protected void updatePlayListRow(List<MediaSession.QueueItem> playlistQueue)
    {
        if (equalsQueue(this.playlistQueue, playlistQueue))
            return;
        Log.v(TAG, "Updating playlist queue ('now playing')");
        this.playlistQueue = playlistQueue;
        resetListRows(playlistQueue);

        if (null == listRow)
        {
            HeaderItem header = createHeader();
            presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
            listRow = new ListRow(header, listRowAdapter);
            rowsAdapter.add(listRow);
        }
        else
            rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(listRow), 1);
    }

    private void resetListRows(List<MediaSession.QueueItem> playlistQueue)
    {
        if (null == playlistQueue || playlistQueue.isEmpty())
        {
            rowsAdapter.remove(listRow);
            listRow = null;
            return;
        }
        listRowAdapter.clear();
        for (MediaSession.QueueItem item : playlistQueue)
            listRowAdapter.add(item);
    }

    private HeaderItem createHeader()
    {
        int queueSize = 0;
        if (getActivity().getMediaController() != null &&
                getActivity().getMediaController().getQueue() != null)
            queueSize = getActivity().getMediaController().getQueue().size();
        return new HeaderItem(0, queueSize + " song(s) in this playlist");
    }

    private void notifyChanged(Action action)
    {
        ArrayObjectAdapter adapter = primaryActionsAdapter;
        if (adapter.indexOf(action) >= 0)
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
    }

    private void resetPlaybackRow()
    {
        duration = 0;
        playbackControlsRow.setTotalTime(0);
        playbackControlsRow.setCurrentTime(0);
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);
    }

    private int getUpdatePeriod()
    {
        if (null == getView() || playbackControlsRow.getTotalTime() <= 0)
            return DEFAULT_UPDATE_PERIOD;
        return Math.max(UPDATE_PERIOD, playbackControlsRow.getTotalTime() / getView().getWidth());
    }

    protected void startProgressAutomation()
    {
        if (handler != null && runnable != null)
            handler.removeCallbacks(runnable);
        runnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = SystemClock.elapsedRealtime() - lastPositionUpdateTime;
                int currentPosition = Math.min(duration, (int) (lastPosition + elapsedTime));
                playbackControlsRow.setCurrentTime(currentPosition);
                handler.postDelayed(this, getUpdatePeriod());
            }
        };
        handler.postDelayed(runnable, getUpdatePeriod());
        setFadingEnabled(true);
    }

    protected void stopProgressAutomation()
    {
        if (handler != null && runnable != null)
        {
            handler.removeCallbacks(runnable);
            setFadingEnabled(false);
        }
    }

    private void updateAlbumArt(Uri artUri)
    {
        AlbumArtCache.getInstance().fetch(artUri.toString(), new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                if (bitmap != null) {
                    Drawable artDrawable = new BitmapDrawable(TvPlaybackFragment.this.getResources(),
                            bitmap);
                    Drawable bgDrawable = new BitmapDrawable(TvPlaybackFragment.this.getResources(),
                            bitmap);
                    playbackControlsRow.setImageDrawable(artDrawable);
                    backgroundManager.setDrawable(bgDrawable);
                    rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow),
                            1);
                }
            }
        });
    }

    protected void updateMetadata(MediaMetadata metadata)
    {
        if (null == playbackControlsRow)
            initializePlaybackControls(metadata);
        duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        playbackControlsRow.setTotalTime(duration);
        ((MutableMediaMetadataHolder) playbackControlsRow.getItem()).metadata = metadata;
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);
        updateAlbumArt(metadata.getDescription().getIconUri());
    }

    protected void updatePlaybackState(PlaybackState state)
    {
        if (null == playbackControlsRow)
            return;
        lastPosition = state.getPosition();
        lastPositionUpdateTime = state.getLastPositionUpdateTime();
        switch (state.getState())
        {
            case PlaybackState.STATE_PLAYING:
                startProgressAutomation();
                playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PAUSE);
                break;
            case PlaybackState.STATE_PAUSED:
                stopProgressAutomation();
                playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PLAY);
                break;
        }

        updatePlayListRow(getActivity().getMediaController().getQueue());
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);
    }

    private static final class DescriptionPresenter extends AbstractDetailsDescriptionPresenter
    {
        @Override
        protected void onBindDescription(AbstractDetailsDescriptionPresenter.ViewHolder viewHolder,
                                         Object item)
        {
            MutableMediaMetadataHolder data = ((MutableMediaMetadataHolder) item);
            viewHolder.getTitle().setText(data.metadata.getDescription().getTitle());
            viewHolder.getSubtitle().setText(data.metadata.getDescription().getSubtitle());
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener
    {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row)
        {
            if (item instanceof MediaSession.QueueItem)
            {
                Log.d(TAG, "item: " + item.toString());
                getActivity().getMediaController().getTransportControls().skipToQueueItem(
                        ((MediaSession.QueueItem) item).getQueueId());
            }
        }
    }

    private static final class MutableMediaMetadataHolder
    {
        MediaMetadata metadata;
        public MutableMediaMetadataHolder(MediaMetadata metadata)
        {
            this.metadata = metadata;
        }
    }
}
