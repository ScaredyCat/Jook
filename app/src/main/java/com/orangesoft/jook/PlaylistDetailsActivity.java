package com.orangesoft.jook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.GetPlaylistRequest;
import com.orangesoft.jook.subsonic.SubsonicBaseActivity;
import com.orangesoft.jook.subsonic.model.JookEntry;
import com.orangesoft.jook.subsonic.view.EntryRecyclerAdapter;
import com.orangesoft.subsonic.Entry;
import com.orangesoft.subsonic.Playlist;
import com.orangesoft.subsonic.command.GetPlaylist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistDetailsActivity extends SubsonicBaseActivity
{
    public static final String JOOK_PLAYLIST_ID = "JookPlaylistId";
    public static final String TAG = "PlaylistDetailsActivity";

    private String playlistId;
    private EntryRecyclerAdapter adapter;
    private List<JookEntry> jookEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout)findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle("Playlist");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.play);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Play functionality coming soon!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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
                JookEntry jookEntry = jookEntries.get(position);
                Entry entry = jookEntry.getEntry();
                playEntry(entry.getId());
            }
        });
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @Override
    public void fetchData()
    {
        Log.v(TAG, "In fetchData");
        Map<String, String> params = new HashMap<>();
        params.put("id", playlistId);
        GetPlaylistRequest getPlaylistRequest = new GetPlaylistRequest(connection.getConnection(),
                params);
        connection.sendRequest(getPlaylistRequest, new GetPlaylistRequestListener(this));
    }

    private void playEntry(String id)
    {
        // Play the playlist item
    }

    private final class GetPlaylistRequestListener implements RequestListener<GetPlaylist>
    {
        final Activity activity;

        GetPlaylistRequestListener(Activity activity)
        {
            this.activity = activity;
        }

        @Override
        public void onRequestFailure(SpiceException spiceException)
        {
            Toast.makeText(getApplication(),
                    "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRequestSuccess(GetPlaylist result)
        {
            setProgressBarIndeterminateVisibility(false);
            Playlist playlist = result.getPlaylist();
            final List<Entry> entries = playlist.getEntries();
            jookEntries.clear();
            int index = 0;
            for (Entry entry : entries)
            {
                JookEntry jookEntry = new JookEntry(entry);
                jookEntries.add(jookEntry);
            }
            adapter.setEntries(jookEntries);
            adapter.notifyDataSetChanged();
        }
    }
}
