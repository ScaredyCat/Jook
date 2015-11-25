package com.orangesoft.jook.subsonic.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.orangesoft.jook.subsonic.model.JookAlbum;
import com.orangesoft.jook.R;

/**
 * Copyright 2015 Orangesoft
 */
public class AlbumArrayAdapter extends ArrayAdapter<JookAlbum>
{
    private final Context context;
    private final JookAlbum[] values;

    public AlbumArrayAdapter(Context context, JookAlbum[] values)
    {
        super(context, R.layout.album_list, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.album_list, parent, false);

        TextView textView = (TextView) rowView.findViewById(R.id.albumName);
        textView.setText(values[position].toString());
        return rowView;
    }
}
