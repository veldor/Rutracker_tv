package net.veldor.rutrackertv;

import android.app.Application;
import android.preference.PreferenceManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.rutrackertv.selections.Distribution;
import net.veldor.rutrackertv.utils.ParseHandler;
import net.veldor.rutrackertv.workers.DownloadTorrentWorker;
import net.veldor.rutrackertv.workers.SearchWorker;
import net.veldor.rutrackertv.workers.StartTorWorker;

import java.util.ArrayList;
import java.util.HashMap;

import static net.veldor.rutrackertv.workers.LoginWorker.LOGIN_ACTION;
import static net.veldor.rutrackertv.workers.PageDetailsWorker.PAGE_DETAILS_ACTION;

public class App extends Application {
    // название папки для хранения файлов TOR
    public static final String TOR_FILES_LOCATION = "tor";
    private static final String START_TOR = "start tor";
    private static final String PREFERENCE_AUTH_COOKIE = "auth cookie";
    private static App instance;
    public LiveData<WorkInfo> TorStartWork;
    public LiveData<WorkInfo> SearchWork;
    // место для хранения TOR клиента
    public final MutableLiveData<AndroidOnionProxyManager> Tor = new MutableLiveData<>();
    // хранилище статуса HTTP запроса
    public final MutableLiveData<String> RequestStatus = new MutableLiveData<>();

    // хранилище настроек
    public android.content.SharedPreferences SharedPreferences;
    public MutableLiveData<ArrayList<Distribution>> SearchedDistributions = new MutableLiveData<>();
    public LiveData<WorkInfo> SearchWorkStatus;
    public MutableLiveData<net.veldor.rutrackertv.selections.ExtendedDistributionInfo> ExtendedDistributionInfo = new MutableLiveData<>();
    public boolean isTorLoaded;
    public String mSearchRequest;
    public String mSelectedCategory;
    public HashMap<String, String> mCategories;
    public MutableLiveData<Boolean> mSearchInProgress = new MutableLiveData<>();
    public MutableLiveData<Integer> isDetailsUpdated = new MutableLiveData<Integer>();

    @org.jetbrains.annotations.Contract(pure = true)
    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        // создам массив категорий
        super.onCreate();
        instance = this;
        mCategories = ParseHandler.getCategories();

        // подключу менеждер настроек
        SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // запуск TOR
        startTor();
    }

    public void startTor() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // запускаю tor
        OneTimeWorkRequest startTorWork = new OneTimeWorkRequest.Builder(StartTorWorker.class).addTag(START_TOR).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniqueWork(START_TOR, ExistingWorkPolicy.REPLACE, startTorWork);
        TorStartWork = WorkManager.getInstance(this).getWorkInfoByIdLiveData(startTorWork.getId());
    }

    public String getAuthCookie() {
        return SharedPreferences.getString(PREFERENCE_AUTH_COOKIE, null);
    }

    public android.content.SharedPreferences getSharedPreferences(){
        return SharedPreferences;
    }

    public void downloadTorrent(String torrentHref) {
        // скачаю торрент
        // запущу рабочего, который загрузит сведения о странице
        Data inputData = new Data.Builder()
                .putString(DownloadTorrentWorker.TORRENT_LINK, torrentHref)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest downloadTorrentWork = new OneTimeWorkRequest.Builder(DownloadTorrentWorker.class).addTag(PAGE_DETAILS_ACTION).setInputData(inputData).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(PAGE_DETAILS_ACTION, ExistingWorkPolicy.REPLACE, downloadTorrentWork);
        WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(downloadTorrentWork.getId());
    }

    public LiveData<WorkInfo> search() {
        // запущу рабочего, который выполнит поиск
        Data inputData = new Data.Builder()
                .putString(SearchWorker.SEARCH_STRING, mSearchRequest)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest searchWork = new OneTimeWorkRequest.Builder(SearchWorker.class).addTag(LOGIN_ACTION).setInputData(inputData).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(LOGIN_ACTION, ExistingWorkPolicy.REPLACE, searchWork);
        App.getInstance().mSearchInProgress.postValue(true);
        return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(searchWork.getId());
    }
}
