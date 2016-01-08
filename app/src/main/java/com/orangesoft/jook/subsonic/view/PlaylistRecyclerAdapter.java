package com.orangesoft.jook.subsonic.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orangesoft.jook.CustomItemClickListener;
import com.orangesoft.jook.R;
import com.orangesoft.jook.subsonic.model.JookPlaylist;
import com.orangesoft.subsonic.Playlist;

/**
 * Copyright 2015 Orangesoft
 */
public class PlaylistRecyclerAdapter extends RecyclerView.Adapter<PlaylistRecyclerAdapter.ViewHolder>
{
    private final JookPlaylist[] values;

    Context context;
    CustomItemClickListener listener;

    public PlaylistRecyclerAdapter(Context context, JookPlaylist[] values, CustomItemClickListener listener)
    {
        this.values = values;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_row, viewGroup,
                false);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                listener.onItemClick(v, viewHolder.getLayoutPosition());
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i)
    {
        String item = values[i].toString();
        viewHolder.textView.setText(item);
    }

    @Override
    public int getItemCount()
    {
        return values.length;
    }

    public Playlist getPlaylist(int index)
    {
        JookPlaylist jookPlaylist = values[index];
        return jookPlaylist.getPlaylist();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView textView;

        ViewHolder(View view)
        {
            super(view);
            textView = (TextView)view.findViewById(R.id.list_item);
        }

    }
}
