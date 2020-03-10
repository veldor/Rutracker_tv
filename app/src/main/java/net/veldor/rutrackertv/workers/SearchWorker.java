package net.veldor.rutrackertv.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.http.TorWebClient;
import net.veldor.rutrackertv.utils.ParseHandler;

import java.io.InputStream;

public class SearchWorker extends Worker {
    public SearchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static final String SEARCH_STRING = "search string";

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        String searchString = data.getString(SEARCH_STRING);
        TorWebClient webClient = new TorWebClient();
        InputStream response = webClient.search(searchString);
        if(response != null){
            // теперь нужно распарсить HTML
            ParseHandler.parseHTML(response);
            App.getInstance().mSearchInProgress.postValue(false);
            return Result.success();
        }
        return Result.failure();
    }
}
