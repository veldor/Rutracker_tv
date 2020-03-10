package net.veldor.rutrackertv.utils;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.content.FileProvider;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.BuildConfig;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class TorrentOpener {
    public static void requestOpen(File torrent) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = FileProvider.getUriForFile(App.getInstance(), BuildConfig.APPLICATION_ID +".provider",torrent);
        intent.setDataAndType(data,"application/x-bittorrent");
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION|FLAG_ACTIVITY_NEW_TASK);
        App.getInstance().startActivity(intent);
    }

    private static boolean intentCanBeHandled(Intent intent){
        PackageManager packageManager = App.getInstance().getPackageManager();
        return intent.resolveActivity(packageManager) != null;
    }
}
