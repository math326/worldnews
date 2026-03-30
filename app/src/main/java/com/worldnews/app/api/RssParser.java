package com.worldnews.app.api;

import android.util.Xml;
import com.worldnews.app.model.RssItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RssParser {

    private static final String TAG_ITEM = "item";
    private static final String TAG_TITLE = "title";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_LINK = "link";
    private static final String TAG_PUB_DATE = "pubDate";
    private static final String TAG_MEDIA_THUMBNAIL = "thumbnail"; // media:thumbnail
    private static final String TAG_ENCLOSURE = "enclosure";
    private static final String ATTR_URL = "url";
    private static final String ATTR_TYPE = "type";

    /**
     * Busca e parseia o RSS da URL informada.
     * DEVE ser chamado em background thread.
     */
    public List<RssItem> parse(String rssUrl) throws IOException, XmlPullParserException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(rssUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "WorldNews/1.0 Android RSS Reader");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new ArrayList<>();
            }

            inputStream = connection.getInputStream();
            return parseFeed(inputStream);
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private List<RssItem> parseFeed(InputStream inputStream) throws XmlPullParserException, IOException {
        List<RssItem> items = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        RssItem currentItem = null;
        boolean inItem = false;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();

            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (TAG_ITEM.equals(tagName)) {
                        currentItem = new RssItem();
                        inItem = true;
                    } else if (inItem) {
                        if (TAG_TITLE.equals(tagName)) {
                            currentItem.setTitle(readText(parser));
                        } else if (TAG_DESCRIPTION.equals(tagName)) {
                            String raw = readText(parser);
                            // Remove tags HTML da descrição do G1
                            currentItem.setDescription(stripHtml(raw));
                        } else if (TAG_LINK.equals(tagName)) {
                            currentItem.setLink(readText(parser));
                        } else if (TAG_PUB_DATE.equals(tagName)) {
                            currentItem.setPubDate(readText(parser));
                        } else if (TAG_MEDIA_THUMBNAIL.equals(tagName)) {
                            // <media:thumbnail url="..."/>
                            String imgUrl = parser.getAttributeValue(null, ATTR_URL);
                            if (imgUrl != null && !imgUrl.isEmpty()) {
                                currentItem.setImageUrl(imgUrl);
                            }
                        } else if (TAG_ENCLOSURE.equals(tagName)) {
                            // <enclosure url="..." type="image/jpeg"/>
                            String type = parser.getAttributeValue(null, ATTR_TYPE);
                            if (type != null && type.startsWith("image")) {
                                String imgUrl = parser.getAttributeValue(null, ATTR_URL);
                                if (imgUrl != null && !imgUrl.isEmpty()) {
                                    currentItem.setImageUrl(imgUrl);
                                }
                            }
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    if (TAG_ITEM.equals(tagName) && currentItem != null) {
                        items.add(currentItem);
                        currentItem = null;
                        inItem = false;
                    }
                    break;
            }
            eventType = parser.next();
        }
        return items;
    }

    private String readText(XmlPullParser parser) throws XmlPullParserException, IOException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result != null ? result.trim() : "";
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        // Remove tags CDATA wrapper
        String text = html.replaceAll("<!\\[CDATA\\[", "").replaceAll("\\]\\]>", "");
        // Remove tags HTML
        text = text.replaceAll("<[^>]+>", "");
        // Decodifica entidades HTML comuns
        text = text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'")
                   .replace("&nbsp;", " ");
        return text.trim();
    }
}
