package com.orangesoft.jook.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Copyright 2016 Orangesoft
 */
public class PrefUtils
{
    private static final String PREF_NAMESPACE = "com.orangesoft.jook.utils.PREFS";
    private static final String FTU_SHOWN = "ftu_shown";

    public static SharedPreferences getPreferences(Context context)
    {
        return context.getSharedPreferences(PREF_NAMESPACE, Context.MODE_PRIVATE);
    }

    public static void setFtuShown(Context context, boolean shown)
    {
        getPreferences(context).edit().putBoolean(FTU_SHOWN, shown).apply();
    }

    public static boolean isFtuShown(Context context)
    {
        return getPreferences(context).getBoolean(FTU_SHOWN, false);
    }
}
