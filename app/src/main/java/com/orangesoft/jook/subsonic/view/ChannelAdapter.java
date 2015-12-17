package com.orangesoft.jook.subsonic.view;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.orangesoft.jook.R;
import com.orangesoft.jook.subsonic.model.JookChannel;
import com.orangesoft.subsonic.Episode;


import java.util.List;

/**
 * Copyright 2015 Orangesoft
 */
public class ChannelAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final List<JookChannel> values;

    public ChannelAdapter(Context context, List<JookChannel> values) {
        this.context = context;
        this.values = values;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return values.get(groupPosition).getEpisodes().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        final String childText = ((Episode) getChild(groupPosition, childPosition)).getTitle();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.
                    LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.episode, null);
        }

        TextView episodeLabel = (TextView) convertView.findViewById(R.id.episode);
        episodeLabel.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return values.get(groupPosition).getEpisodes().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return values.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return values.size();
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent)
    {
        String headerTitle = values.get(groupPosition).toString();

        if (convertView == null)
        {
            LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.
                    LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.podcast_list, null);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.podcastName);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }
}
