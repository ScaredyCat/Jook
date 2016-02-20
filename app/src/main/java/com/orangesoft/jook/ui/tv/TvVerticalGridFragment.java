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
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * VericalGridFragment shows a grid of music songs
 */
public class TvVerticalGridFragment extends VerticalGridFragment
{
    private static final String TAG = TvVerticalGridFragment.class.getSimpleName();

    private static final int NUM_COLUMNS = 5;

    private ArrayObjectAdapter adapter;
    private String mediaId;
    private TvBrowseFragment.MediaFragmentListener mediaFragmentListener;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        setupFragment();
    }

    private void setupFragment()
    {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        adapter = new ArrayObjectAdapter(new CardPresenter());
        setAdapter(adapter);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        mediaFragmentListener = (TvBrowseFragment.MediaFragmentListener) activity;
    }

    protected void setMediaId(String mediaId)
    {
        Log.d(TAG, "setMediaId: " + mediaId);
        if (TextUtils.equals(this.mediaId, mediaId))
            return;
        MediaBrowser mediaBrowser = mediaFragmentListener.getMediaBrowser();

        if (this.mediaId != null)
            mediaBrowser.unsubscribe(mediaId);
        if (null == mediaId)
            mediaId = mediaBrowser.getRoot();
        this.mediaId = mediaId;
        mediaBrowser.subscribe(this.mediaId, subscriptionCallback);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        MediaBrowser mediaBrowser = mediaFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mediaId != null)
            mediaBrowser.unsubscribe(mediaId);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mediaFragmentListener = null;
    }

    public interface MediaFragmentListener
    {
        MediaBrowser getMediaBrowser();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener
    {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row)
        {
            getActivity().getMediaController().getTransportControls().playFromMediaId(
                    ((MediaBrowser.MediaItem) item).getMediaId(), null);
            Intent intent = new Intent(getActivity(), TvPlaybackActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    TvVerticalGridActivity.SHARED_ELEMENT_NAME).toBundle();
            getActivity().startActivity(intent, bundle);
        }
    }

    private final MediaBrowser.SubscriptionCallback subscriptionCallback =
            new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowser.MediaItem> children)
                {
                    adapter.clear();
                    for (MediaBrowser.MediaItem item : children)
                    {
                        if (!item.isPlayable())
                            Log.e(TAG, "Cannot show non-playable items.  Ignoring " +
                                item.getMediaId());
                        else
                            adapter.add(item);
                    }
                    adapter.notifyArrayItemRangeChanged(0, children.size());
                }

                @Override
                public void onError(@NonNull String parentId) {
                    Log.e(TAG, "browse fragment subscription onError, id=" + parentId);
                }
            };
}
