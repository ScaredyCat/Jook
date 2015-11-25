package com.orangesoft.jook.subsonic.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.orangesoft.jook.R;
import com.orangesoft.jook.subsonic.model.JookPlaylist;

/**
 * Copyright 2015 Orangesoft
 */
public class PlaylistArrayAdapter extends ArrayAdapter<JookPlaylist>
{
    private final Context context;
    private final JookPlaylist[] values;

    public PlaylistArrayAdapter(Context context, JookPlaylist[] values)
    {
        super(context, R.layout.playlist_list, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.playlist_list, parent, false);

        TextView textView = (TextView) rowView.findViewById(R.id.playlistName);
        textView.setText(values[position].toString());
        return rowView;
    }
}
