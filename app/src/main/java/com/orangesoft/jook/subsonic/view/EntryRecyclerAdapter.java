package com.orangesoft.jook.subsonic.view;

import android.content.Context;
import android.media.MediaMetadata;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orangesoft.jook.CustomItemClickListener;
import com.orangesoft.jook.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2015 Orangesoft
 */
public class EntryRecyclerAdapter extends RecyclerView.Adapter<EntryRecyclerAdapter.ViewHolder>
{
    private final List<MediaMetadata> entries;

    Context context;
    CustomItemClickListener listener;

    public EntryRecyclerAdapter(Context context, List<MediaMetadata> newEntries,
                                CustomItemClickListener listener)
    {
        this.entries = new ArrayList<>();
        this.context = context;
        this.listener = listener;
    }

    public void setEntries(List<MediaMetadata> newEntries)
    {
        this.entries.clear();
        this.entries.addAll(newEntries);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_card_row,
                viewGroup,false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
               // listener.onItemClick(v, viewHolder.getLayoutPosition());
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i)
    {
        MediaMetadata entry = entries.get(i);
        viewHolder.titleView.setText(entry.getDescription().getTitle());
        viewHolder.artistView.setText(entry.getDescription().getSubtitle());
    }

    @Override
    public int getItemCount()
    {
        return entries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView titleView;
        private final TextView artistView;

        ViewHolder(View view)
        {
            super(view);
            titleView = (TextView)view.findViewById(R.id.song_title);
            artistView = (TextView)view.findViewById(R.id.artist);
        }

    }
}
