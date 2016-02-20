package com.orangesoft.jook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.GetPodcastsRequest;
import com.orangesoft.jook.subsonic.MusicProviderFragmentBase;
import com.orangesoft.jook.subsonic.model.JookChannel;
import com.orangesoft.jook.subsonic.view.ChannelAdapter;
import com.orangesoft.subsonic.Channel;
import com.orangesoft.subsonic.command.GetPodcasts;

import java.util.ArrayList;
import java.util.List;


/**
 * Copyright 2015 Orangesoft
 */
public class PodcastFragment extends MusicProviderFragmentBase
{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_podcast, container, false);
    }

    public void fetchData()
    {
        //GetPodcastsRequest getPodcastsRequest = new GetPodcastsRequest(connection.getConnection());
        //connection.sendRequest(getPodcastsRequest, new GetPodcastsRequestListener());
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
            List<JookChannel> jookChannels = new ArrayList<>();
            for (Channel channel : channelList)
                jookChannels.add(new JookChannel(channel));
            ChannelAdapter adapter = new ChannelAdapter(getActivity().getApplicationContext(),
                    jookChannels);
            ExpandableListView listView = (ExpandableListView) getActivity().findViewById(R.id.podcastList);
            listView.setAdapter(adapter);
        }
    }
}
