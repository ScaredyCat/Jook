package com.orangesoft.jook.subsonic;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;

import com.orangesoft.jook.model.MusicProvider;
import com.orangesoft.jook.model.ProviderConnection;

/**
 * Copyright 2015 Orangesoft
 */
public abstract class MusicProviderFragmentBase extends Fragment
{
    public MusicProvider musicProvider;
    public ProviderConnection connection;

    @Override
    public void onStart()
    {
        super.onStart();
        connection = new SubsonicConnection(this.getActivity());
        musicProvider = MusicProvider.getInstance();
        musicProvider.setProviderConnection(connection);
        fetchData();
    }

    @Override
    public void onStop()
    {
        connection.close();
        super.onStop();
    }

    public abstract void fetchData();
}
