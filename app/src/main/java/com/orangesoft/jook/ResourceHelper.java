package com.orangesoft.jook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

/**
 * Copyright 2016 Orangesoft
 */
public class ResourceHelper
{
    private final static String TAG = ResourceHelper.class.getSimpleName();

    public static int getThemeColor(Context context, int attribute, int defaultColor)
    {
        int themeColor = 0;
        String packageName = context.getPackageName();
        try
        {
            Context packageContext = context.createPackageContext(packageName, 0);
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(
                    packageName, 0);
            packageContext.setTheme(applicationInfo.theme);
            Resources.Theme theme = packageContext.getTheme();
            TypedArray typedArray = theme.obtainStyledAttributes(new int[] {attribute});
            themeColor = typedArray.getColor(0, defaultColor);
            typedArray.recycle();
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e(TAG, "Error in getThemeColor", e);
        }
        return themeColor;
    }
}
