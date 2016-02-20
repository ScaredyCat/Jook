/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Work originally authored by
 *
 *  Copyright 2014 The Android Open Source Project, Inc.
 *
 * under the android-UniversalMusicPlayer project
 */
package com.orangesoft.jook.ui.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.net.Uri;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.orangesoft.jook.AlbumArtCache;
import com.orangesoft.jook.R;

public class CardPresenter extends Presenter
{
    private static final String TAG = CardPresenter.class.getSimpleName();
    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 250;

    private static Context context;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent)
    {
        Log.v(TAG, "onCreateViewHolder");
        context = parent.getContext();

        ImageCardView cardView = new ImageCardView(context);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setBackgroundColor(context.getResources().getColor(R.color.default_background));
        return new CardViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item)
    {
        MediaDescription description = getMediaDescription(item);

        final CardViewHolder cardViewHolder = createCardViewHolder(viewHolder, description);

        Uri artUri = description.getIconUri();
        if (artUri == null)
            setCardImage(cardViewHolder, description.getIconBitmap());
        else
        {
            setCardImageFromCache(artUri, cardViewHolder, description);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder)
    {
        Log.v(TAG, "onUnbindViewHolder");
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder)
    {
        Log.v(TAG, "onViewAttachedToWindow");
    }

    private void setCardImageFromCache(Uri artUri, final CardViewHolder cardViewHolder,
                                       MediaDescription description)
    {
        String artUrl = artUri.toString();
        AlbumArtCache cache = AlbumArtCache.getInstance();
        if (cache.getBigImage(artUrl) != null)
            setCardImage(cardViewHolder, cache.getBigImage(artUrl));
        else
        {
            setCardImage(cardViewHolder, description.getIconBitmap());
            cache.fetch(artUrl, new AlbumArtCache.FetchListener()
            {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon)
                {
                    setCardImage(cardViewHolder, bitmap);
                }
            });
        }
    }

    private MediaDescription getMediaDescription(Object item)
    {
        MediaDescription description;

        if (item instanceof MediaBrowser.MediaItem)
        {
            MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) item;
            Log.d(TAG, "onBindViewHolder mediaItem: " + mediaItem.toString());
            description = mediaItem.getDescription();
        }
        else if (item instanceof MediaSession.QueueItem)
        {
            MediaSession.QueueItem queueItem = (MediaSession.QueueItem) item;
            description = queueItem.getDescription();
        }
        else
            throw new IllegalArgumentException("Object mus be MediaItem or QueueItem, not " +
                    item.getClass().getSimpleName());

        return description;
    }

    private CardViewHolder createCardViewHolder(Presenter.ViewHolder viewHolder,
                                                MediaDescription description)
    {
        CardViewHolder cardViewHolder = (CardViewHolder) viewHolder;

        cardViewHolder.cardView.setTitleText(description.getTitle());
        cardViewHolder.cardView.setContentText(description.getSubtitle());
        cardViewHolder.cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        return cardViewHolder;
    }

    private void setCardImage(CardViewHolder cardViewHolder, Bitmap art)
    {
        if (null != cardViewHolder.cardView)
        {
            Drawable artDrawable = null;
            if (null != art)
                artDrawable = new BitmapDrawable(context.getResources(), art);
            else
            {
                CharSequence title = cardViewHolder.cardView.getTitleText();
                if (null != title && title.length() > 0)
                    artDrawable = new TextDrawable(String.valueOf(title.charAt(0)));
            }
            cardViewHolder.cardView.setMainImage(artDrawable);
        }
    }

    private static class CardViewHolder extends Presenter.ViewHolder
    {
        private final ImageCardView cardView;

        public CardViewHolder(View view)
        {
            super(view);
            cardView = (ImageCardView) view;
        }
    }

    private static class TextDrawable extends Drawable
    {
        private final String text;
        private final Paint paint;

        public TextDrawable(String text)
        {
            this.text = text;
            this.paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(280f);
            paint.setAntiAlias(true);
            paint.setFakeBoldText(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void draw(Canvas canvas)
        {
            Rect r = getBounds();
            int count = canvas.save();
            canvas.translate(r.left, r.top);
            float midW = r.width() / 2;
            float midH = r.height() / 2 - ((paint.descent() + paint.ascent()) / 2);
            canvas.drawText(text, midW, midH, paint);
            canvas.restoreToCount(count);
        }

        @Override
        public void setAlpha(int alpha)
        {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf)
        {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity()
        {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
