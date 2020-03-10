package net.veldor.rutrackertv.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.rutrackertv.http.TorWebClient;
import net.veldor.rutrackertv.utils.TorrentOpener;

import java.io.File;

public class DownloadTorrentWorker extends Worker {
    public static final String TORRENT_LINK = "torrent link";

    public DownloadTorrentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        String href = data.getString(TORRENT_LINK);
        TorWebClient webClient = new TorWebClient();
        File torrent = webClient.downloadTorrent(href);
        if(torrent != null){
            // запрошу открытие торрента
            TorrentOpener.requestOpen(torrent);
        }
        return Result.success();
    }
}
