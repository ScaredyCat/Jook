/**
 * Copyright 2016 Orangesoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in cmoplaince with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * and has been modified to DO STUFF!
 */
package com.orangesoft.jook.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.orangesoft.jook.AlbumArtCache;
import com.orangesoft.jook.MusicService;
import com.orangesoft.jook.R;

/**
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment
{
    private static final String TAG = PlaybackControlsFragment.class.getSimpleName();

    private ImageButton playPause;
    private TextView title;
    private TextView subtitle;
    private TextView extraInfo;
    private ImageView albumArt;
    private String artUrl;

    // Receive callbacks from the MediaController.  Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaController.Callback callback = new MediaController.Callback()
    {
        @Override
        public void onPlaybackStateChanged(PlaybackState state)
        {
            Log.d(TAG, "Received playback state change to state " + state.getState());
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata)
        {
            if (null != metadata)
            {
                Log.d(TAG, "Received metadata state change to mediaId=" + metadata.getDescription().
                        getMediaId() + "song=" + metadata.getDescription().getTitle() );
                PlaybackControlsFragment.this.onMetadataChanged(metadata);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        playPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        playPause.setEnabled(true);
        playPause.setOnClickListener(buttonListener);

        title = (TextView) rootView.findViewById(R.id.title);
        subtitle = (TextView) rootView.findViewById(R.id.artist);
        extraInfo = (TextView) rootView.findViewById(R.id.extra_info);
        albumArt = (ImageView) rootView.findViewById(R.id.album_art);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MusicPlayerFullScreenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ((BaseActivity)getActivity()).pushMedia();
                MediaMetadata metadata = getActivity().getMediaController().getMetadata();
                if (metadata != null)
                {
                    intent.putExtra(MusicPlayerFullScreenActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION,
                            metadata.getDescription());
                }
                startActivity(intent);
            }
        });
        return rootView;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.v(TAG, "fragment.onStart");
        if (getActivity().getMediaController() != null)
            onConnected();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.v(TAG, "fragment.onStop");
        if (getActivity().getMediaController() != null)
            getActivity().getMediaController().unregisterCallback(callback);
    }

    public void onConnected()
    {
        MediaController controller = getActivity().getMediaController();
        Log.d(TAG, "onConnected, mediaController==null? " + (controller == null));
        if (controller != null)
        {
            onMetadataChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(callback);
        }
    }

    private void onMetadataChanged(MediaMetadata metadata)
    {
        Log.d(TAG, "onMetadataChanged " + metadata);
        if (null == getActivity())
        {
            Log.w(TAG, "onMetadataChanged called when getActivity null, this should not happen if "
                + "the callback was properly unregistered.  Ignoring.");
            return;
        }
        if (null == metadata)
            return;

        title.setText(metadata.getDescription().getTitle());
        subtitle.setText(metadata.getDescription().getSubtitle());
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null)
            artUrl = metadata.getDescription().getIconUri().toString();
        if (!TextUtils.equals(artUrl, this.artUrl))
        {
            this.artUrl = artUrl;
            Bitmap art = metadata.getDescription().getIconBitmap();
            AlbumArtCache cache = AlbumArtCache.getInstance();
            if (null == art)
                art = cache.getIconImage(this.artUrl);
            if (art != null)
                albumArt.setImageBitmap(art);
            else
            {
                cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                    @Override
                    public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                        if (icon != null)
                        {
                            Log.d(TAG, "album art icon of w=" + icon.getWidth() + " h=" +
                                icon.getHeight());
                            if (isAdded())
                                albumArt.setImageBitmap(icon);
                        }
                    }
                });
            }
        }
    }

    public void setExtraInfo(String extraInfo)
    {
        if (null == extraInfo)
            this.extraInfo.setVisibility(View.GONE);
        else
        {
            this.extraInfo.setText(extraInfo);
            this.extraInfo.setVisibility(View.VISIBLE);
        }
    }

    private void onPlaybackStateChanged(PlaybackState state)
    {
        Log.d(TAG, "onPlaybackStateChanged " + state);
        if (null == getActivity())
        {
            Log.w(TAG, "onPlaybackStateChanged called when getActivity null, this should not " +
                    "happen if the callback was properly unregistered.  Ignoring.");
            return;
        }
        if (null == state)
            return;

        boolean enablePlay = false;
        switch (state.getState())
        {
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackState.STATE_ERROR:
                Log.e(TAG, "error playbackstate: " + state.getErrorMessage());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay)
            playPause.setImageDrawable(getActivity().getDrawable(R.drawable.ic_play_arrow_black_36dp));
        else
            playPause.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause_black_36dp));

        MediaController controller = getActivity().getMediaController();
        String extraInfo = null;
        if (controller != null && controller.getExtras() != null)
        {
            String castName = controller.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
            if (castName != null)
                extraInfo = getResources().getString(R.string.casting_to_device, castName);
        }
        setExtraInfo(extraInfo);
    }

    private final View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PlaybackState stateObj = getActivity().getMediaController().getPlaybackState();
            final int state = stateObj == null ? PlaybackState.STATE_NONE : stateObj.getState();
            Log.d(TAG, "Button pressed, in state " + state);
            switch (v.getId())
            {
                case R.id.play_pause:
                    Log.d(TAG, "Play button pressed, in state " + state);
                    if (state == PlaybackState.STATE_PAUSED ||
                            state == PlaybackState.STATE_STOPPED ||
                            state == PlaybackState.STATE_NONE)
                        playMedia();
                    else if (state == PlaybackState.STATE_PLAYING ||
                            state == PlaybackState.STATE_BUFFERING ||
                            state == PlaybackState.STATE_CONNECTING)
                        pauseMedia();
                    break;
            }
        }
    };

    private void playMedia()
    {
        MediaController controller = getActivity().getMediaController();
        if (controller != null)
            controller.getTransportControls().play();
    }

    private void pauseMedia()
    {
        MediaController controller = getActivity().getMediaController();
        if (controller != null)
            controller.getTransportControls().pause();
    }
}
