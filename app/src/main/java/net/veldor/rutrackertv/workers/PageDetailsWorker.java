package net.veldor.rutrackertv.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.rutrackertv.http.TorWebClient;
import net.veldor.rutrackertv.utils.ParseHandler;

import java.io.InputStream;

public class PageDetailsWorker extends Worker {
    public static final String PAGE_HREF = "href";
    public static final String PAGE_DETAILS_ACTION = "page details";

    public PageDetailsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("surprise", "PageDetailsWorker doWork start details work");
        Data data = getInputData();
        String href = data.getString(PAGE_HREF);
        TorWebClient webClient = new TorWebClient();
        InputStream response = webClient.getPageData(href);
        if(response != null){
            // теперь нужно распарсить HTML
            ParseHandler.parsePageDetails(href, response);
            Log.d("surprise", "PageDetailsWorker doWork finish details work");
            return Result.success();
        }
        return Result.failure();
    }
}
