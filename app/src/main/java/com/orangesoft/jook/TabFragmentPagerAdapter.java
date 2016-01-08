package com.orangesoft.jook;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Copyright 2015 Orangesoft
 */
public class TabFragmentPagerAdapter extends FragmentStatePagerAdapter
{
    public static int int_items = 3;

    public TabFragmentPagerAdapter(FragmentManager fm)
    {
        super(fm);
    }

    /**
     * Return fragment with respect to Position
     */
    @Override
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case 0 : return new PlaylistFragment();
            case 1 : return new AlbumsFragment();
            case 2 : return new PodcastFragment();
        }
        return null;
    }

    @Override
    public int getCount()
    {
        return int_items;
    }

    /**
     * This method returns the title of the tab according to the position.
     */
    @Override
    public CharSequence getPageTitle(int position)
    {
        switch (position)
        {
            case 0 :
                return "Playlists";
            case 1 :
                return "Albums";
            case 2 :
                return "Podcasts";
        }
        return null;
    }
}
