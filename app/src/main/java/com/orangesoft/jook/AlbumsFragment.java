/**
 * Copyright 2015 Orangesoft.
 */
package com.orangesoft.jook;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.GetAlbumsRequest;
import com.orangesoft.jook.subsonic.SubsonicFragmentBase;
import com.orangesoft.jook.subsonic.model.JookAlbum;
import com.orangesoft.jook.subsonic.view.AlbumArrayAdapter;
import com.orangesoft.subsonic.Album;
import com.orangesoft.subsonic.command.GetAlbumList;

import java.util.List;


public class AlbumsFragment extends SubsonicFragmentBase
{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    public void fetchData()
    {
        GetAlbumsRequest getAlbumsRequest = new GetAlbumsRequest(connection.getConnection());
        connection.sendRequest(getAlbumsRequest, new GetAlbumsRequestListener());
    }

    private final class GetAlbumsRequestListener implements RequestListener<GetAlbumList>
    {

        @Override
        public void onRequestFailure(SpiceException spiceException)
        {
            Toast.makeText(getActivity(),
                    "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRequestSuccess(GetAlbumList result)
        {
            if (!result.getStatus())
            {
                Toast.makeText(getActivity(),
                        "Error: " + result.getFailureMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            getActivity().setProgressBarIndeterminateVisibility(false);
            List<Album> albumList = result.getList();
            Album[] albums = albumList.toArray(new Album[albumList.size()]);
            JookAlbum[] jookAlbums = new JookAlbum[albums.length];
            int index = 0;
            for (Album album : albums)
            {
                JookAlbum jookAlbum = new JookAlbum(album.getAlbumName(), album.getArtist());
                jookAlbums[index++] = jookAlbum;
            }
            AlbumArrayAdapter adapter = new AlbumArrayAdapter(getActivity().getApplicationContext(),
                    jookAlbums);
            ListView listView = (ListView) getActivity().findViewById(R.id.albumList);
            listView.setAdapter(adapter);
        }
    }
}
