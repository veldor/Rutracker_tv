package net.veldor.rutrackertv.http;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.workers.StartTorWorker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.DnsResolver;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.ssl.SSLContexts;

import static net.veldor.rutrackertv.ui.SettingsActivity.KEY_LOW_TO_HIGH;
import static net.veldor.rutrackertv.ui.SettingsActivity.KEY_SORT_BY;

public class TorWebClient {

    private final SharedPreferences mPreferences;


    public TorWebClient() {
        mPreferences = App.getInstance().getSharedPreferences();
    }

    private HttpClient getNewHttpClient() {
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
                .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg, new FakeDnsResolver());
        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    public boolean login(Uri uri) {
        Log.d("surprise", "TorWebClient login start logging in");
        HttpResponse response;
        UrlEncodedFormEntity params;
        params = get2post(uri);
        try {
            response = executeRequest("https://rutracker.org/forum/login.php", null, params);
            App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.response_received_message));
            int status = response.getStatusLine().getStatusCode();
            Log.d("surprise", "TorWebClient login status is " + status);
            // получен ответ, попробую извлечь куку
            Header[] cookies = response.getHeaders("set-cookie");
            if (cookies.length > 1) {
                String value = cookies[1].getValue();
                value = value.substring(0, value.indexOf(";"));
                String authCookie = value.trim();
                CookieManager.put(authCookie);
                App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.success_login_message));
                return true;
            } else {
                Log.d("surprise", "TorWebClient login no cookie :(");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("surprise", "TorWebClient login error logging in");
            return false;
        }
    }

    private static UrlEncodedFormEntity get2post(Uri url) {
        Set<String> params = url.getQueryParameterNames();
        if (params.isEmpty()) {
            return null;
        }
        List<NameValuePair> paramsArray = new ArrayList<>();

        Map<String, String> map = getQueryMap(url.toString());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            try {
                value = URLDecoder.decode(value, "windows-1251");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            paramsArray.add(new BasicNameValuePair(name, value));
        }
        try {
            return new UrlEncodedFormEntity(paramsArray, "windows-1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    private HttpResponse executeRequest(String url, Map<String, String> headers, UrlEncodedFormEntity params) throws IOException {
        try {
            AndroidOnionProxyManager tor = App.getInstance().Tor.getValue();
            if (tor != null) {
                HttpClient httpClient = getNewHttpClient();
                int port = tor.getIPv4LocalHostSocksPort();
                InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext clientContext = HttpClientContext.create();
                clientContext.setAttribute("socks.address", socketAddress);

                HttpPost request = new HttpPost(url);

                if (params != null) {
                    request.setEntity(params);
                }
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        request.setHeader(entry.getKey(), entry.getValue());
                    }
                }
                request.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
                request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
                request.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
                request.setHeader(HttpHeaders.CONNECTION, "keep-alive");
                request.setHeader("DNT", "1");
                request.setHeader(HttpHeaders.HOST, "rutracker.org");
                request.setHeader(HttpHeaders.PRAGMA, "no-cache");
                request.setHeader("Sec-Fetch-Dest", "document");
                request.setHeader("Sec-Fetch-Mode", "navigate");
                request.setHeader("Sec-Fetch-Site", "none");
                request.setHeader("Upgrade-Insecure-Requests", "1");
                request.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.116 Safari/537.36");
                String authCookie = CookieManager.get();
                if (authCookie != null) {
                    request.setHeader("Cookie", "bb_ssl=1; " + authCookie);
                }
                return httpClient.execute(request, clientContext);
            }
        } catch (RuntimeException e) {
            if(restartTor()){
                // повторю запрос
                return executeRequest(url, headers, params);
            }
            else{
                Toast.makeText(App.getInstance(), "Error request", Toast.LENGTH_LONG).show();
            }
        }
        return null;
    }

    private boolean restartTor() {
        Log.d("surprise", "TorWebClient restartTor: restart TOR");
        return StartTorWorker.startTor();
    }

    private HttpResponse executeGetRequest(String url) throws IOException {
        try {
            HttpClient httpClient = getNewHttpClient();
            AndroidOnionProxyManager tor = App.getInstance().Tor.getValue();
            if (tor != null) {
                int port = tor.getIPv4LocalHostSocksPort();
                InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
                HttpClientContext clientContext = HttpClientContext.create();
                clientContext.setAttribute("socks.address", socketAddress);

                HttpGet request = new HttpGet(url);
                request.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
                request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
                request.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
                request.setHeader(HttpHeaders.CONNECTION, "keep-alive");
                request.setHeader("DNT", "1");
                request.setHeader(HttpHeaders.HOST, "rutracker.org");
                request.setHeader(HttpHeaders.PRAGMA, "no-cache");
                request.setHeader("Sec-Fetch-Dest", "document");
                request.setHeader("Sec-Fetch-Mode", "navigate");
                request.setHeader("Sec-Fetch-Site", "none");
                request.setHeader("Upgrade-Insecure-Requests", "1");
                request.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.116 Safari/537.36");
                String authCookie = CookieManager.get();
                if (authCookie != null) {
                    request.setHeader("Cookie", "bb_ssl=1; " + authCookie);
                }
                return httpClient.execute(request, clientContext);
            }
        }catch (RuntimeException e) {
                if(restartTor()){
                    // повторю запрос
                    return executeGetRequest(url);
                }
                else{
                    Toast.makeText(App.getInstance(), "Error request", Toast.LENGTH_LONG).show();
                }
            }
        return null;
    }

    public InputStream search(String searchString) {
        try {
            App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.prepare_search_message));
            String url = "https://rutracker.org/forum/tracker.php?&nm=" + URLEncoder.encode(searchString, "windows-1251");
            // post =================================
            List<NameValuePair> paramsArray = new ArrayList<>();
            if (App.getInstance().mSelectedCategory != null) {
                // определю категорию поиска
                // найду идентификатор категории
                HashMap<String, String> categories = App.getInstance().mCategories;
                String category = categories.get(App.getInstance().mSelectedCategory);
                if (category != null && !category.isEmpty()) {
                    paramsArray.add(new BasicNameValuePair("f[]", category));
                }
                // сброшу выбранную категорию
                App.getInstance().mSelectedCategory = null;
            }
            paramsArray.add(new BasicNameValuePair("nm", searchString));
            // добавлю настройки поиска
            // сортировка
            String value = mPreferences.getString(KEY_SORT_BY, "10");
            paramsArray.add(new BasicNameValuePair("o", value));

            // от большего к меньшему
            value = mPreferences.getBoolean(KEY_LOW_TO_HIGH, false) ? "1" : "2";
            paramsArray.add(new BasicNameValuePair("s", value));
            // post =================================
            App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.send_request_message));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(paramsArray, "windows-1251");
            HttpResponse response = executeRequest(url, null, form);
            App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.response_received_message));
            return response.getEntity().getContent();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public InputStream getPageData(String href) {
        String url = "https://rutracker.org/forum/" + href;
        try {
            HttpResponse result = executeGetRequest(url);
            App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.response_received_message));
            if (result != null) {
                return result.getEntity().getContent();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File downloadTorrent(String href) {
        // получу папку с загрузками
        String url = "https://rutracker.org/forum/" + href;
        try {
            HttpResponse result = executeGetRequest(url);
            if (result != null) {
                String filename = "noname.torrent";
                Header[] headers = result.getAllHeaders();
                for (Header h : headers) {
                    if (h.getName().equals("Content-Disposition")) {
                        // получу имя файла
                        filename = URLDecoder.decode(h.getValue().substring(h.getValue().indexOf("filename*=UTF-8''") + 17), "UTF-8");
                    }
                }
                File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                InputStream is = result.getEntity().getContent();
                FileOutputStream outputStream = new FileOutputStream(path);
                int read;
                byte[] buffer = new byte[1024];
                while ((read = is.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                is.close();
                Log.d("surprise", "TorWebClient downloadTorrent torrent downloaded!");
                return path;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public InputStream getFileName(String torrentName) {
        // отправлю пост-запрос
        try {
            torrentName = URLDecoder.decode(torrentName, "windows-1251");
            List<NameValuePair> paramsArray = new ArrayList<>();
            paramsArray.add(new BasicNameValuePair("t", torrentName));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(paramsArray, "windows-1251");
            HttpResponse response = executeRequest("https://rutracker.org/forum/viewtorrent.php", null, form);
            if (response != null) {
                return response.getEntity().getContent();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("surprise", "TorWebClient getFileName wow, error then request file name!");
        }
        return null;
    }


    public static class FakeDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return new InetAddress[]{InetAddress.getByAddress(new byte[]{1, 1, 1, 1})};
        }
    }
}
