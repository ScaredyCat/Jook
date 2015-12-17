package com.orangesoft.jook;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.GetPodcastsRequest;
import com.orangesoft.jook.subsonic.SubsonicFragmentBase;
import com.orangesoft.jook.subsonic.model.JookChannel;
import com.orangesoft.jook.subsonic.model.JookPlaylist;
import com.orangesoft.jook.subsonic.view.ChannelArrayAdapter;
import com.orangesoft.jook.subsonic.view.PlaylistArrayAdapter;
import com.orangesoft.subsonic.Channel;
import com.orangesoft.subsonic.Playlist;
import com.orangesoft.subsonic.command.GetPlaylists;
import com.orangesoft.subsonic.command.GetPodcasts;

import java.util.List;


/**
 * Copyright 2015 Orangesoft
 */
public class PodcastFragment extends SubsonicFragmentBase
{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_podcast, container, false);
    }

    public void fetchData()
    {
        GetPodcastsRequest getPodcastsRequest = new GetPodcastsRequest(connection.getConnection());
        connection.sendRequest(getPodcastsRequest, new GetPodcastsRequestListener());
    }

    private final class GetPodcastsRequestListener implements RequestListener<GetPodcasts>
    {

        @Override
        public void onRequestFailure(SpiceException spiceException)
        {
            Toast.makeText(getActivity(),
                    "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRequestSuccess(GetPodcasts result)
        {
            getActivity().setProgressBarIndeterminateVisibility(false);
            List<Channel> channelList = result.getChannels();
            Channel[] channels = channelList.toArray(new Channel[channelList.size()]);
            JookChannel[] jookChannels = new JookChannel[channels.length];
            int index = 0;
            for (Channel channel : channels)
            {
                JookChannel jookChannel = new JookChannel(channel);
                jookChannels[index++] = jookChannel;
            }
            ChannelArrayAdapter adapter = new ChannelArrayAdapter(getActivity().getApplicationContext(),
                    jookChannels);
            ListView listView = (ListView) getActivity().findViewById(R.id.podcastList);
            listView.setAdapter(adapter);
        }
    }
}
