package com.orangesoft.jook.subsonic;

import android.support.v4.app.Fragment;

/**
 * Copyright 2015 Orangesoft
 */
public abstract class SubsonicFragmentBase extends Fragment
{
    public SubsonicConnection connection;

    @Override
    public void onStart()
    {
        super.onStart();
        connection = new SubsonicConnection(this.getActivity());
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
