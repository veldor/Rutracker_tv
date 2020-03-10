package net.veldor.rutrackertv.utils;

import net.veldor.rutrackertv.App;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class FileHandler {
    private static final String SEARCH_AUTOCOMPLETE_FILE = "search_results.txt";
    private static final String SEARCH_VALUE_NAME = "string";
    private static final String SEARCH_AUTOCOMPLETE_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><search> </search>";

    private static String getSearchData() {

        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        if (!autocompleteFile.exists()) {
            makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(autocompleteFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }


    private static void makeFile(File file, String content) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static ArrayList<String> getSearchAutocomplete() {
        ArrayList<String> searchValues = new ArrayList<>();
        Document searchList = getDocument(getSearchData());
        if (searchList != null) {
            // найду значения строк
            NodeList values = searchList.getElementsByTagName(SEARCH_VALUE_NAME);
            int counter = 0;
            while (values.item(counter) != null) {
                searchValues.add(values.item(counter).getFirstChild().getNodeValue());
                ++counter;
            }
            return searchValues;
        }
        return null;
    }


    private static Document getDocument(String rawText) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(rawText));
            return dBuilder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void putSearchValue(String query) {
        ArrayList<String> values = getSearchAutocomplete();
        if (values != null) {
            if (!values.contains(query)) {
                Document searchList = getDocument(getSearchData());
                if (searchList != null) {
                    Element elem = searchList.createElement(SEARCH_VALUE_NAME);
                    Text text = searchList.createTextNode(query);
                    elem.appendChild(text);
                    searchList.getDocumentElement().insertBefore(elem, searchList.getDocumentElement().getFirstChild());
                    saveSearchAutocomplete(getStringFromDocument(searchList));
                }
            }
        }
    }


    private static String getStringFromDocument(Document doc) {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            transformer.transform(domSource, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return writer.toString();
    }


    private static void saveSearchAutocomplete(String value) {
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        makeFile(autocompleteFile, value);
    }

    public static void clearSearchHistory() {
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW);
    }

    public static void removeFromHistory(CharSequence value) {
        String searchData = getSearchData();
        saveSearchAutocomplete(searchData.replace("<string>" + value + "</string>", ""));
    }
}
