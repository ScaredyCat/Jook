package com.orangesoft.jook;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.GetPlaylistsRequest;
import com.orangesoft.jook.subsonic.SubsonicFragmentBase;
import com.orangesoft.jook.subsonic.model.JookPlaylist;
import com.orangesoft.jook.subsonic.view.PlaylistRecyclerAdapter;
import com.orangesoft.subsonic.Playlist;
import com.orangesoft.subsonic.command.GetPlaylists;

import java.util.ArrayList;
import java.util.List;


/**
 * Copyright 2015 Orangesoft
 */
public class PlaylistFragment extends SubsonicFragmentBase
{
    private PlaylistRecyclerAdapter adapter;
    private List<Playlist> playlists;

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

    public void fetchData()
    {
        GetPlaylistsRequest getPlaylistsRequest = new GetPlaylistsRequest(connection.
                getConnection());
        connection.sendRequest(getPlaylistsRequest, new GetPlaylistsRequestListener());
    }

    private void showPlaylistDetails(String id)
    {
        Intent intent = new Intent(getContext(), PlaylistDetailsActivity.class);
        intent.putExtra(PlaylistDetailsActivity.JOOK_PLAYLIST_ID, id);
        startActivity(intent);
    }

    private final class GetPlaylistsRequestListener implements RequestListener<GetPlaylists>
    {

        @Override
        public void onRequestFailure(SpiceException spiceException)
        {
            Toast.makeText(getActivity(),
                    "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRequestSuccess(GetPlaylists result)
        {
            if (!result.getStatus())
            {
                Toast.makeText(getActivity(),
                        "Error: " + result.getFailureMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            getActivity().setProgressBarIndeterminateVisibility(false);
            playlists.clear();
            playlists.addAll(result.getList());
            List<JookPlaylist> jookPlaylists = new ArrayList<>();
            int index = 0;
            for (Playlist playlist : playlists)
            {
                JookPlaylist jookPlaylist = new JookPlaylist(playlist.getName(), playlist.
                        getSongCount(), playlist);
                jookPlaylists.add(jookPlaylist);
            }
            adapter.setCustomItemClickListener( new CustomItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Playlist playlist = playlists.get(position);
                    showPlaylistDetails(playlist.getId());
                }
            });
            adapter.updatePlaylist(jookPlaylists);
            adapter.notifyDataSetChanged();
        }
    }

}
