package net.veldor.rutrackertv.http;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import net.veldor.rutrackertv.App;

/**
 * Created by Bond on 01-Dec-15.
 */
 public class CookieManager {
     private static final String KEY = "cookie";
    private static final String TAG = "CookieManager";

     public static String get() {
         SharedPreferences preferences = App.getInstance().SharedPreferences;
         return preferences.getString(KEY, null);
    }

    @SuppressLint("CommitPrefEdits")
     static void clear(Context mContext) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(KEY);
        editor.apply();
        Log.d(TAG, "Cleared saved cookie");
    }

    @SuppressLint("CommitPrefEdits")
     static void put(String token) {
        SharedPreferences preferences = App.getInstance().SharedPreferences;
        preferences.edit().putString(KEY, token).apply();
    }
}

