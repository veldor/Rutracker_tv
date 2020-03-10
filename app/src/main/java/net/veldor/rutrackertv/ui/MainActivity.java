package net.veldor.rutrackertv.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.LeanbackActivity;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.http.CookieManager;
import net.veldor.rutrackertv.viewmodel.MyViewModel;

public class MainActivity extends LeanbackActivity {

    // код запуска активити инициализации
    private static final int WAIT = 1;
    // код запуска аутентификации
    private static final int LOGIN = 2;
    private static final int REQUEST_WRITE_READ = 3;
    private MyViewModel ViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        handleObservers();

    }

    private void handleObservers() {
        // добавлю отслеживание начала поиска
        LiveData<Boolean> searchStart = App.getInstance().mSearchInProgress;
        searchStart.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    Log.d("surprise", "MainActivity onChanged start waiter!");
                    // покажу окно ожидания поиска
                    startActivity(new Intent(MainActivity.this, SearchWaiterActivity.class));
                }
            }
        });
    }

    private void checkPermissions() {
        int writeResult;
        int readResult;
        int voiceResult;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeResult = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            readResult = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            voiceResult = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        } else {
            writeResult = PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            readResult = PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            voiceResult = PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        }
        if(writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED && voiceResult == PackageManager.PERMISSION_GRANTED){
            // если предоставлены разрешения- продолжаю старт приложения
            waitForTorLoad();
            setupViewModel();
        }
        else{
            Log.d("surprise", "MainActivity checkPermissions: have no permissions!");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_WRITE_READ);
            }
            else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_WRITE_READ);
            }
        }
    }

    private void setupViewModel() {
        // добавлю viewModel
        ViewModel = new ViewModelProvider(this).get(MyViewModel.class);
    }

    private void waitForTorLoad() {
        // придётся запускать активити для ожидания
        startActivityForResult(new Intent(this, WaiterActivity.class), WAIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == WAIT){
            // если запуск TOR успешен, проверю наличие учётных данных
            if(resultCode == RESULT_OK){
                if(CookieManager.get() == null){
                    // запущу актвити с логином
                    startActivityForResult(new Intent(this, LoginActivity.class), LOGIN);
                }
                else{
                    ViewModel.search("Дождливый день в Нью-Йорке");
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_READ && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                checkPermissions();
            } else {
                waitForTorLoad();
                setupViewModel();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
