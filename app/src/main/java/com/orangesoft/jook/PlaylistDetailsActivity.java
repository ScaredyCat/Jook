package com.orangesoft.jook;

import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.orangesoft.jook.model.JookPlaylist;
import com.orangesoft.jook.model.PlaylistListener;
import com.orangesoft.jook.subsonic.view.EntryRecyclerAdapter;
import com.orangesoft.jook.ui.BaseActivity;
import com.orangesoft.jook.ui.MusicPlayerFullScreenActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailsActivity extends BaseActivity implements PlaylistListener
{
    public static final String JOOK_PLAYLIST_ID = "JookPlaylistId";
    public static final String TAG = "PlaylistDetailsActivity";

    private String playlistId;
    private EntryRecyclerAdapter adapter;
    private List<MediaMetadata> jookEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_details);
        initializeToolbar();

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout)findViewById(R.id.
                collapsing_toolbar);
        collapsingToolbarLayout.setTitle("Playlist");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.play);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAll();
            }
        });
        Intent intent = getIntent();
        playlistId = intent.getStringExtra(JOOK_PLAYLIST_ID);
        initializeRecyclerView();
    }

    private void initializeRecyclerView()
    {
        jookEntries = new ArrayList<>();
        adapter = new EntryRecyclerAdapter(getApplicationContext(),
                jookEntries, new CustomItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                MediaMetadata jookEntry = jookEntries.get(position);
                playEntry(jookEntry.getDescription().getMediaId());
            }
        });
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @Override
    public void onPlaylists(List<JookPlaylist> playlists) {
        // not needed
    }

    @Override
    public void onPlaylist(JookPlaylist playlist, List<MediaMetadata> entries)
    {
        setProgressBarIndeterminateVisibility(false);
        jookEntries.clear();
        jookEntries.addAll(entries);
        adapter.setEntries(jookEntries);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void pushMedia()
    {
        Log.v(TAG, "pushMedia for Playlist");
        Log.d(TAG, "jookEntries has " + jookEntries.size() + " entries");
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_CMD);
        intent.putExtra(MusicService.CMD_NAME, MusicService.CUSTOM_ACTION_SET_PLAY_QUEUE);
        intent.putExtra(MusicService.PLAY_QUEUE, (Serializable) jookEntries);
        startService(intent);
    }

    @Override
    public void fetchData()
    {
        musicProvider.fetchPlaylist(this, playlistId);
    }

    private void playEntry(String id)
    {
        // Play the playlist item
    }

    private void playAll()
    {
        pushMedia();
        startPlayback();
        Intent intent = new Intent(getApplicationContext(), MusicPlayerFullScreenActivity.class);
        intent.putExtra(MusicPlayerFullScreenActivity.JOOK_ENTRIES, (Serializable)jookEntries);
        startActivity(intent);
    }

    private void startPlayback()
    {
        MediaController controller = getMediaController();
        if (controller != null)
            controller.getTransportControls().play();
        else
            Log.e(TAG, "MediaController is null.  Cannot start playback!");
    }
}
