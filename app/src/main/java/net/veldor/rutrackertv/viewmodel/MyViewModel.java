package net.veldor.rutrackertv.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.workers.LoginWorker;
import net.veldor.rutrackertv.workers.PageDetailsWorker;

import static net.veldor.rutrackertv.workers.LoginWorker.LOGIN_ACTION;
import static net.veldor.rutrackertv.workers.LoginWorker.USER_LOGIN;
import static net.veldor.rutrackertv.workers.LoginWorker.USER_PASSWORD;
import static net.veldor.rutrackertv.workers.PageDetailsWorker.PAGE_DETAILS_ACTION;

public class MyViewModel extends ViewModel {

    public LiveData<WorkInfo> logMeIn(String login, String password) {
        // запущу рабочего, который выполнит вход в аккаунт
        Data inputData = new Data.Builder()
                .putString(USER_LOGIN, login)
                .putString(USER_PASSWORD, password)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest loginWork = new OneTimeWorkRequest.Builder(LoginWorker.class).addTag(LOGIN_ACTION).setInputData(inputData).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(LOGIN_ACTION, ExistingWorkPolicy.REPLACE, loginWork);
        return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(loginWork.getId());
    }

    public LiveData<WorkInfo> search(String searchRequest) {
        App.getInstance().mSearchRequest = searchRequest;
        return App.getInstance().search();
    }

    public void getPageData(String href) {
        // запущу рабочего, который загрузит сведения о странице
        Data inputData = new Data.Builder()
                .putString(PageDetailsWorker.PAGE_HREF, href)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest pageDetalisWork = new OneTimeWorkRequest.Builder(PageDetailsWorker.class).addTag(PAGE_DETAILS_ACTION).setInputData(inputData).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(PAGE_DETAILS_ACTION, ExistingWorkPolicy.REPLACE, pageDetalisWork);
        WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(pageDetalisWork.getId());
    }
}
