package net.veldor.rutrackertv.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.http.TorWebClient;
import net.veldor.rutrackertv.selections.Distribution;
import net.veldor.rutrackertv.utils.ParseHandler;

import java.io.InputStream;
import java.util.ArrayList;

public class ListInfoWorker extends Worker {

    public static final String MASS_PAGE_DETAILS_ACTION = "mass page details";

    public ListInfoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // получу данные о элементах
        ArrayList<Distribution> parts = App.getInstance().SearchedDistributions.getValue();
        if(parts != null){
            int size = parts.size();
            if(size > 0){
                int counter = 0;
                Distribution part;
                while (counter < size){
                    if(isStopped()){
                        return Result.success();
                    }
                        // гружу данные о странице
                        part = parts.get(counter);
                        if(part != null){
                            TorWebClient webClient = new TorWebClient();
                            InputStream response = webClient.getPageData(part.Href);
                            if(response != null){
                                part.ExtendedInfo = ParseHandler.fastParseDetails(response, part.TorrentHref);
                            }
                            if(isStopped()){
                                return Result.success();
                            }
                            App.getInstance().isDetailsUpdated.postValue(counter);
                        }
                    counter++;
                }
            }
        }
        return Result.success();
    }
}
