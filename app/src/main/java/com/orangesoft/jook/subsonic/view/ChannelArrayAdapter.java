package com.orangesoft.jook.subsonic.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.orangesoft.jook.R;
import com.orangesoft.jook.subsonic.model.JookChannel;

/**
 * Copyright 2015 Orangesoft
 */
public class ChannelArrayAdapter extends ArrayAdapter<JookChannel>
{
    private final Context context;
    private final JookChannel[] values;

    public ChannelArrayAdapter(Context context, JookChannel[] values)
    {
        super(context, R.layout.podcast_list, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.podcast_list, parent, false);

        TextView textView = (TextView) rowView.findViewById(R.id.podcastName);
        textView.setText(values[position].toString());
        return rowView;
    }
}
