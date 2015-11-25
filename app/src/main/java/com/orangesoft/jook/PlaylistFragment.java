package com.orangesoft.jook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.GetPlaylistsRequest;
import com.orangesoft.jook.subsonic.SubsonicFragmentBase;
import com.orangesoft.jook.subsonic.model.JookPlaylist;
import com.orangesoft.jook.subsonic.view.PlaylistArrayAdapter;
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
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    public void fetchData()
    {
        GetPlaylistsRequest getPlaylistsRequest = new GetPlaylistsRequest(connection.getConnection());
        connection.sendRequest(getPlaylistsRequest, new GetPlaylistsRequestListener());
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
            getActivity().setProgressBarIndeterminateVisibility(false);
            List<Playlist> playlistList = result.getList();
            Playlist[] playlists = playlistList.toArray(new Playlist[playlistList.size()]);
            JookPlaylist[] jookPlaylists = new JookPlaylist[playlists.length];
            int index = 0;
            for (Playlist playlist : playlists)
            {
                JookPlaylist jookPlaylist = new JookPlaylist(playlist.getName(), playlist.getSongCount());
                jookPlaylists[index++] = jookPlaylist;
            }
            PlaylistArrayAdapter adapter = new PlaylistArrayAdapter(getActivity().getApplicationContext(),
                    jookPlaylists);
            ListView listView = (ListView) getActivity().findViewById(R.id.playlistList);
            listView.setAdapter(adapter);
        }
    }

}
