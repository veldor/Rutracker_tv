package net.veldor.rutrackertv.utils;

import android.content.SharedPreferences;
import android.util.Log;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.http.TorWebClient;
import net.veldor.rutrackertv.selections.ContentTypes;
import net.veldor.rutrackertv.selections.Distribution;
import net.veldor.rutrackertv.selections.ExtendedDistributionInfo;
import net.veldor.rutrackertv.ui.SettingsActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class ParseHandler {


    public static HashMap<String, String> getCategories() {
        try {
            HashMap<String, String> categories = new HashMap<>();
            String code;
            String name;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(App.getInstance().getAssets().open("category.xml"), null);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    if (name.equals("option")) {
                        code = parser.getAttributeValue(null, "value");
                        name = parser.nextText();
                        if (name.length() > 4) {
                            name = name.substring(4);
                        }
                        categories.put(name, code);
                    }
                }
                eventType = parser.next();
            }
            return categories;
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void parseHTML(InputStream htmlString) {
        App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.handle_results_message));

        // получу менеджер настроек
        SharedPreferences prefs = App.getInstance().getSharedPreferences();
        // проверю, скрываются ли раздачи без сидов
        boolean hideEmpty = prefs.getBoolean(SettingsActivity.KEY_HIDE_NO_SEED, true);

        try {
            String url = "http://rutracker.org";
            Document dom;
            dom = Jsoup.parse(htmlString, "Windows-1251", url);
            // найду разделы с торрентами
            Elements parts = dom.select("tr.tCenter.hl-tr");
            Distribution distribution;
            ArrayList<Distribution> result = new ArrayList<>();

            for (Element part : parts) {
                distribution = new Distribution();
                // найду количество сидов
                Elements seeds = part.select("b.seedmed");
                if (seeds != null && seeds.size() > 0) {
                    distribution.Seeds = seeds.first().text();
                    if(distribution.Seeds.equals("0") && hideEmpty){
                        continue;
                    }
                } else {
                    if (hideEmpty) {
                        // пропущу торрент, так как нет сидов
                        continue;
                    }
                    // или просто обозначу, что сидов нет
                    distribution.Seeds = "";
                }
                // найду категорию
                distribution.Category = part.select("td.f-name").first().text();
                // найду размер
                String text = part.select("td.tor-size").first().text();
                distribution.Size = text.substring(0, text.length() - 2);
                // найду имя торрента
                Element nameDiv = part.select("td.t-title").first();
                distribution.Name = nameDiv.text();
                String contentType = ContentTypes.getContentType(distribution.Name);
                if (contentType != null) {
                    distribution.IsMovie = true;
                    distribution.ContentType = contentType;
                }
                distribution.Href = nameDiv.select("a.tLink").first().attr("href");
                Elements torrentAnchor = part.select("a.dl-stub");
                if (torrentAnchor.size() > 0) {
                    distribution.TorrentHref = torrentAnchor.first().attr("href");
                }

                // теперь нужно как-то понять, является ли найденное значение медиаконтентом

                result.add(distribution);
            }
            App.getInstance().RequestStatus.postValue(App.getInstance().getString(R.string.results_handled_message));
            // помещу найденные значения в хранилище
            App.getInstance().SearchedDistributions.postValue(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void parsePageDetails(String href, InputStream response) {
        String url = "http://rutracker.org";
        try {
            Document dom = Jsoup.parse(response, "Windows-1251", url);
            // получу информацию о странице
            ExtendedDistributionInfo extendedInfo = new ExtendedDistributionInfo();
            Elements postBody = dom.select("div.post_body");
            if (postBody.size() > 0) {
                extendedInfo.PostBody = postBody.first().html();
            }

            Elements postImage = dom.select(".postImg.postImgAligned");
            if (postImage.size() > 0) {
                extendedInfo.PostImageUrl = postImage.first().attr("title");
            }

            extendedInfo.PageHref = href;

            // добавлю имя файла
            // получу имя торрента
            String torrentName = dom.select("a.dl-link").first().attr("href").substring(9);
            // найду информацию о файлах в торренте
            TorWebClient webClient = new TorWebClient();
            response = webClient.getFileName(torrentName);
            dom = Jsoup.parse(response, "UTF-8", url);
            if (dom != null) {
                Elements files = dom.select("li div>b");
                if (files.size() == 1) {
                    extendedInfo.FileName = files.first().text();
                } else {
                    extendedInfo.FileName = files.get(1).text();
                }
            }

            // добавлю сведения о загруженном торренте
            App.getInstance().ExtendedDistributionInfo.postValue(extendedInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ExtendedDistributionInfo fastParseDetails(InputStream response, String torrentHref) {
        try {
            ExtendedDistributionInfo extendedInfo = new ExtendedDistributionInfo();
            String url = "http://rutracker.org";
            Document dom;
            dom = Jsoup.parse(response, "Windows-1251", url);
            Elements images = dom.select(".postImg.postImgAligned");
            if (images != null && images.size() > 0) {
                    extendedInfo.PostImageUrl = images.first().attr("title");
            }
            if(torrentHref != null && torrentHref.length() > 9){
                // теперь получу данные о расширении файла
                TorWebClient webClient = new TorWebClient();
                InputStream filesResponse = webClient.getFileName(torrentHref.substring(9));
                if (filesResponse != null) {
                    dom = Jsoup.parse(filesResponse, "UTF-8", url);
                    if (dom != null) {
                        Elements files = dom.select("li div>b");
                        if (files.size() == 1) {
                            extendedInfo.FileName = files.first().text();
                        } else {
                            Log.d("surprise", "ParseHandler fastParseDetails: multi file");
                            int c = 1;
                            String name;
                            while (c < files.size()){
                                name = files.get(c).text();
                                Log.d("surprise", "ParseHandler fastParseDetails: name is " + name);
                                if(ContentTypes.getMovieFormat(name) != null){
                                    // найден медиафайл
                                    extendedInfo.FileName = name;
                                    Log.d("surprise", "ParseHandler fastParseDetails: found movie: " + name);
                                    break;
                                }
                                c++;
                            }
                            if(extendedInfo.FileName == null){
                                extendedInfo.FileName = files.get(1).text();
                            }
                        }
                    }
                }
            }
            return extendedInfo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
