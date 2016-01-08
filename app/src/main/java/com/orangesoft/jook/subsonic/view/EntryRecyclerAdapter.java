package com.orangesoft.jook.subsonic.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orangesoft.jook.CustomItemClickListener;
import com.orangesoft.jook.R;
import com.orangesoft.jook.subsonic.model.JookEntry;
import com.orangesoft.subsonic.Entry;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2015 Orangesoft
 */
public class EntryRecyclerAdapter extends RecyclerView.Adapter<EntryRecyclerAdapter.ViewHolder>
{
    final static String TAG = "EntryRecyclerAdapter";
    private final List<JookEntry> entries;

    Context context;
    CustomItemClickListener listener;

    public EntryRecyclerAdapter(Context context, List<JookEntry> newEntries,
                                CustomItemClickListener listener)
    {
        this.entries = new ArrayList<>();
        this.context = context;
        this.listener = listener;
    }

    public void setEntries(List<JookEntry> newEntries)
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
        Entry entry = entries.get(i).getEntry();
        viewHolder.titleView.setText(entry.getTitle());
        viewHolder.artistView.setText(entry.getArtist());
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
