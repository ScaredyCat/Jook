package com.orangesoft.jook;

import android.app.Fragment;
import android.content.Intent;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orangesoft.jook.model.PlaylistListener;
import com.orangesoft.jook.model.JookPlaylist;
import com.orangesoft.jook.subsonic.MusicProviderFragmentBase;
import com.orangesoft.jook.subsonic.view.PlaylistRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;


/**
 * Copyright 2015 Orangesoft
 */
public class PlaylistFragment extends MusicProviderFragmentBase implements PlaylistListener
{
    private PlaylistRecyclerAdapter adapter;
    private List<JookPlaylist> playlists;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_list_view, container, false);
        playlists = new ArrayList<>();
        adapter = new PlaylistRecyclerAdapter(getContext());
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onPlaylists(List<JookPlaylist> jookPlaylists)
    {
        getActivity().setProgressBarIndeterminateVisibility(false);
        playlists.clear();
        playlists.addAll(jookPlaylists);
        adapter.setCustomItemClickListener( new CustomItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                JookPlaylist playlist = playlists.get(position);
                showPlaylistDetails(playlist.getId());
            }
        });
        adapter.updatePlaylist(playlists);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onPlaylist(JookPlaylist playlist, List<MediaMetadata> entries)
    {
        // Not needed
    }

    public void fetchData()
    {
        musicProvider.fetchPlaylists(this);
    }

    private void showPlaylistDetails(String id)
    {
        Intent intent = new Intent(getContext(), PlaylistDetailsActivity.class);
        intent.putExtra(PlaylistDetailsActivity.JOOK_PLAYLIST_ID, id);
        startActivity(intent);
    }

}
