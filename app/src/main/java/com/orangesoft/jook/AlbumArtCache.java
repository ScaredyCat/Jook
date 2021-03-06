/**
 * Copyright 2016 Orangesoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in cmoplaince with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * and has been modified to DO STUFF!
 */
package com.orangesoft.jook;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import com.orangesoft.jook.utils.BitmapHelper;

import java.io.IOException;

/**
 * Implements a basic cache of album arts, with async loading support.
 */
public class AlbumArtCache
{
    private final static String TAG = AlbumArtCache.class.getSimpleName();

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 12*1024*1024; // 12 MB
    private static final int MAX_ART_WIDTH = 800;  // pixels
    private static final int MAX_ART_HEIGHT = 480;  // pixels;

    // Resolution reasonable for carrying around as an icon (generally in
    // MediaDescription.getIconBitmap).  This should not be bigger than necessary, because
    // the MediaDescription object should be lightweight.  If you set it too high and try to
    // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
    private static final int MAX_ART_WIDTH_ICON = 128; // pixels
    private static final int MAX_ART_HEIGHT_ICON = 128;  // pixels

    private static final int BIG_BITMAP_INDEX = 0;
    private static final int ICON_BITMAP_INDEX = 1;

    private final LruCache<String, Bitmap[]> cache;

    private final static AlbumArtCache instance = new AlbumArtCache();

    public static AlbumArtCache getInstance()
    {
        return instance;
    }

    private AlbumArtCache()
    {
        // Holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes, bounded by maxmemory/4 and
        // Integer.MAX_VALUE;
        int maxSize = Math.min(MAX_ALBUM_ART_CACHE_SIZE,
                (int) (Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory()/4)));
        cache = new LruCache<String, Bitmap[]>(maxSize) {
            @Override
            protected int sizeOf(String key, Bitmap[] value)
            {
                return value[BIG_BITMAP_INDEX].getByteCount()
                        + value[ICON_BITMAP_INDEX].getByteCount();
            }
        };
    }

    public Bitmap getBigImage(String artUrl)
    {
        Bitmap[] result = cache.get(artUrl);
        return result == null ? null : result[BIG_BITMAP_INDEX];
    }

    public Bitmap getIconImage(String artUrl)
    {
        Bitmap[] result = cache.get(artUrl);
        return result == null ? null : result[ICON_BITMAP_INDEX];
    }

    public void fetch(final String artUrl, final FetchListener listener)
    {
        // WARNING: for the sake of simplicity, simultaneous multi-thread fetch requests
        // are not handled properly: they may cause redundant costly operations, like HTTP
        // requests and bitmap rescales.  For production-level apps, we recommend you use
        // a proper image loading library, like Glide.
        final Bitmap[] bitmap = cache.get(artUrl);
        if (bitmap != null)
        {
            Log.d(TAG, "getOrFetch: album art is in cache, using it : " + artUrl);
            listener.onFetched(artUrl, bitmap[BIG_BITMAP_INDEX], bitmap[ICON_BITMAP_INDEX]);
            return;
        }
        Log.d(TAG, "getOrFetch: starting asyncTask to fetch " + artUrl);

        new AsyncTask<Void, Void, Bitmap[]>() {
            @Override
            protected Bitmap[] doInBackground(Void[] objects)
            {
                Bitmap[] bitmaps;
                try {
                    Bitmap bitmap = BitmapHelper.fetchAndRescaleBitmap(artUrl,
                            MAX_ART_WIDTH, MAX_ART_HEIGHT);
                    Bitmap icon = BitmapHelper.scaleBitmap(bitmap, MAX_ART_WIDTH_ICON,
                            MAX_ART_HEIGHT_ICON);
                    bitmaps = new Bitmap[] {bitmap, icon};
                    cache.put(artUrl, bitmaps);
                } catch (IOException e) {
                    return null;
                }
                Log.d(TAG, "doInBackground: putting bitmap in cache.  cache size=" + cache.size());
                return bitmaps;
            }

            @Override
            protected void onPostExecute(Bitmap[] bitmaps)
            {
                if (null == bitmaps)
                    listener.onError(artUrl, new IllegalArgumentException("got null bitmaps"));
                else
                    listener.onFetched(artUrl, bitmaps[BIG_BITMAP_INDEX], bitmaps[ICON_BITMAP_INDEX]);
            }
        }.execute();
    }

    public static abstract class FetchListener
    {
        public abstract void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage);
        public void onError(String artUrl, Exception e)
        {
            Log.e(TAG, "AlbumArtFetchListener: error while downloading " + artUrl + ", error: " +
                e.getLocalizedMessage());
        }
    }
}
