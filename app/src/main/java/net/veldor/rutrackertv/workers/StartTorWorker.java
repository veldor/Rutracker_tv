package net.veldor.rutrackertv.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.rutrackertv.App;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.client.protocol.HttpClientContext;

public class StartTorWorker extends Worker {

    public StartTorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (startTor())
            return Result.success();
        return Result.failure();
    }

    public static boolean startTor() {
        AndroidOnionProxyManager tor;
        if (App.getInstance().Tor.getValue() != null) {
            tor = App.getInstance().Tor.getValue();
        } else {
            tor = new AndroidOnionProxyManager(App.getInstance(), App.TOR_FILES_LOCATION);
        }
        // просто создание объекта, не запуск
        // тут- время, которое отводится на попытку запуска
        int totalSecondsPerTorStartup = (int) TimeUnit.MINUTES.toSeconds(3);
        // количество попыток запуска
        int totalTriesPerTorStartup = 1;
        try {
            boolean ok = tor.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
            if (!ok) {
                // TOR не запущен, оповещу о том, что запуск не удался
                return false;
            }
            if (tor.isRunning()) {
                //Returns the socks port on the IPv4 localhost address that the Tor OP is listening on
                int port = tor.getIPv4LocalHostSocksPort();
                InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext context = HttpClientContext.create();
                context.setAttribute("socks.address", socksaddr);
                App.getInstance().Tor.postValue(tor);
                App.getInstance().isTorLoaded = true;
                Log.d("surprise", "StartTorWorker doWork: tor start");
            } else {
                Log.d("surprise", "StartTorWorker doWork: tor start failed");
                // TOR не запущен, оповещу о том, что запуск не удался
                return false;
            }
        } catch (InterruptedException e) {
            Log.d("surprise", "запуск TOR прерван");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("surprise", "StartTorWorker doWork: tor start failed");
            if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                return false;
            }
            e.printStackTrace();
        }
        return true;
    }
}
