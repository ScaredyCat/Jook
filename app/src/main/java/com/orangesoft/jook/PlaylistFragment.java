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

import java.util.List;


/**
 * Copyright 2015 Orangesoft
 */
public class PlaylistFragment extends SubsonicFragmentBase
{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_view, container, false);
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
            final List<Playlist> playlistList = result.getList();
            Playlist[] playlists = playlistList.toArray(new Playlist[playlistList.size()]);
            JookPlaylist[] jookPlaylists = new JookPlaylist[playlists.length];
            int index = 0;
            for (Playlist playlist : playlists)
            {
                JookPlaylist jookPlaylist = new JookPlaylist(playlist.getName(), playlist.
                        getSongCount(), playlist);
                jookPlaylists[index++] = jookPlaylist;
            }
            final PlaylistRecyclerAdapter adapter = new PlaylistRecyclerAdapter(getContext(),
                    jookPlaylists, new CustomItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Playlist playlist = playlistList.get(position);
                    showPlaylistDetails(playlist.getId());
                }
            });
            RecyclerView recyclerView = (RecyclerView) getActivity().findViewById(R.id.recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(adapter);
        }
    }

}
