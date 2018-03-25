import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArticleParser {


    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, TransformerException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Do you want to parse articles? Yes/No");
         String answer1 = scanner.nextLine();
        if (answer1.equals("Yes")){


        InvertedIndex porterIndex = new InvertedIndex();
        InvertedIndex mystemIndex = new InvertedIndex();


        String site = "http://www.mathnet.ru";
        String url = site +
                "/php/archive.phtml?jrnid=ivm&wshow=issue&year=2015&volume=&volume_alt=&issue=6&issue_alt=&option_lang=rus";
        String GET_ALL_LINKS = "//td[@width='90%']/a[@class='SLink']";

        WebClient webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage page = webClient.getPage(url);

        List<HtmlAnchor> links = page.getByXPath(GET_ALL_LINKS);

        int i = 0;


        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();


        Document doc = docBuilder.newDocument();


        // корневой элемент
        Element rootElement = doc.createElement("article_list");
        doc.appendChild(rootElement);
        for (HtmlAnchor link : links) {
            i++;
            HtmlPage articletxt = webClient.getPage(site + link.getHrefAttribute());

            // статья
            Element article = doc.createElement("article");
            rootElement.appendChild(article);
            Element id = doc.createElement("id");

            id.appendChild(doc.createTextNode(String.valueOf(i)));
            article.appendChild(id);

            // название

            String titleStr = articletxt.getFirstByXPath("//span[@class='red']/font/text()").toString().trim();
            //default
            Element title_default = doc.createElement("title");
            title_default.setAttribute("type", "default");
            title_default.appendChild(doc.createTextNode(titleStr));
            article.appendChild(title_default);

            //porter
            Element title_porter = doc.createElement("title");
            title_porter.setAttribute("type", "porter");
            title_porter.appendChild(doc.createTextNode(getPorterSting(titleStr)));
            article.appendChild(title_porter);
            porterIndex.getIndex(getPorterSting(titleStr), i);


            //mystem
            Element title_mystem = doc.createElement("title");
            title_mystem.setAttribute("type", "mystem");
            title_mystem.appendChild(doc.createTextNode(getMyStemSting(titleStr)));
            article.appendChild(title_mystem);
            mystemIndex.getIndex(getMyStemSting(titleStr), i);


            // url
            Element href = doc.createElement("url");
            href.appendChild(doc.createTextNode(site + link.getHrefAttribute()));
            article.appendChild(href);

            // аннотация
            String annotationStr = "";
            List<Object> annotationlist = articletxt.getByXPath("//b[contains(text(),'Аннотация')]" +
                    "/following::text()[preceding::b[1][contains(text(),'Аннотация')] and not(parent::b)]");
            for (Object o : annotationlist) {
                annotationStr += o.toString().trim();
            }

            //default
            Element annotation_default = doc.createElement("annotation");
            annotation_default.appendChild(doc.createTextNode(annotationStr));
            annotation_default.setAttribute("type", "default");
            article.appendChild(annotation_default);

            //porter
            Element annotation_porter = doc.createElement("annotation");
            annotation_porter.appendChild(doc.createTextNode(getPorterSting(annotationStr)));
            annotation_porter.setAttribute("type", "porter");
            article.appendChild(annotation_porter);
            porterIndex.getIndex(getPorterSting(annotationStr), i);

            //mystem
            Element annotation_mystem = doc.createElement("annotation");
            annotation_mystem.appendChild(doc.createTextNode(getMyStemSting(annotationStr)));
            annotation_mystem.setAttribute("type", "mystem");
            article.appendChild(annotation_mystem);
            mystemIndex.getIndex(getMyStemSting(annotationStr), i);


            // ключевые слова
            String[] keywordstxt = articletxt.getFirstByXPath("//i[preceding-sibling::b[contains(text(), 'Ключевые')]]/text()").toString().split(",");
            Element keywords = doc.createElement("keywords");
            article.appendChild(keywords);
            for (String keywordStr : keywordstxt) {

                Element keyword = doc.createElement("keyword");
                keyword.appendChild(doc.createTextNode(keywordStr.trim()));
                keywords.appendChild(keyword);
            }


        }


        porterIndex.createFile("porter");
        mystemIndex.createFile("mystem");

        // запись в xml
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File("task3.xml"));
        transformer.transform(source, result);


        System.out.println("File saved!");

         }
         // пересечние
        System.out.println("Do you want to find intersection? Yes/No");
        String answer2 = scanner.nextLine();
        if (answer2.equals("Yes")) {
            typePhrase();
        }
        //tf-idf

        System.out.println("Do you want to find tf-idf? Yes/No");
        String answer3 = scanner.nextLine();
        if (answer3.equals("Yes")) {
            System.out.println("write word");
            String word = scanner.nextLine();
            System.out.println("write type: " +
                    "porter or " +
                    "mystem");
            String typeStr = scanner.nextLine();
            if (typeStr.equals("porter") || typeStr.equals("mystem")) {
                for(int j = 1; j <= 10; j++ ){
                    findTfIdf(word, typeStr, j);
                }


            }

        }


    }

    //делаем porter строку
    public static String getPorterSting(String s) {
        s = s.replaceAll("[^А-Яа-я\\s]", "");
        String[] s_arr = s.split(" ");
        s = "";
        Porter porter = new Porter();
        for (String string : s_arr) {
            if (!string.equals("")) {
                s += " " + porter.stem(string);
            }
        }
        return s;
    }
    //делаем mystem строку
    public static String getMyStemSting(String s) throws IOException {
        s = s.replaceAll("[^А-Яа-я\\s]", "");
        String[] s_arr = s.split(" ");
        s = "";
        MyStem myStem = new MyStem();
        for (String string : s_arr) {
            if (!string.equals("")) {
                String stemstr = myStem.stem(string);
                if (stemstr.substring(stemstr.length() - 1).equals("?")) {
                    stemstr = stemstr.substring(0, stemstr.length() - 1);
                    s += " " + stemstr;
                }

            }
        }
        return s;
    }
    //ввод фразы для поиска перечечений
    public static void typePhrase() throws IOException, ParserConfigurationException, SAXException {
        System.out.println("write phrase");
        Scanner scanner = new Scanner(System.in);
        String phrase = scanner.nextLine();
        String[] words = phrase.trim().split(" ");
        if (words.length <= 1){
            System.out.println("Please type more than 1 word");
        }
        else {
            String typeStr = "";

            boolean flag = false;
            while (flag == false) {
                System.out.println("write type: " +
                        "porter or " +
                        "mystem");
                typeStr = scanner.nextLine();

                if (typeStr.equals("porter") || typeStr.equals("mystem")) {
                    flag = true;
                } else {
                    System.out.println("write correct type");
                }

            }

            Map<Integer, List<Integer>> parsed = parsePhrase(words, typeStr);
            Map<Integer, List<Integer>> parsedsorted = new TreeMap<>(parsed);
            Object[] keys = parsedsorted.keySet().toArray();


            List<Integer> list1 = parsedsorted.get(keys[0]);
            List<Integer> list2 = parsedsorted.get(keys[1]);
            List<Integer> result = intersection(list1, list2);
            for (int i = 2; i < keys.length; i++) {
                result = intersection(result, parsedsorted.get(keys[i]));
            }
            for (Integer value : result) {
                System.out.println(value);
            }
        }

    }

    //парсинг фразы. Получаем мап, который содержит кол-во документов, в котром содержится слово, и список их id
    public static Map<Integer, List<Integer>> parsePhrase(String[] words, String type) throws IOException, ParserConfigurationException, SAXException {

        Map<Integer, List<Integer>> map = new LinkedHashMap<>();
        for (String word : words) {
            word = word.trim();
            boolean needsign = false;
            if (word.charAt(0) == '-') {
                needsign = true;
            }
            if (type.equals("porter")) {
                word = getPorterSting(word);
            } else if (type.equals("mystem")) {
                word = getMyStemSting(word);
            }
            word = word.trim();
            if (needsign) {
                word = "-" + word;
            }

            List<Integer> list = findWord(word, type);
            map.put(list.size(), list);
        }
        return map;
    }

    //находим id документов, в которых есть слово
    public static List<Integer> findWord(String word, String type) throws IOException, ParserConfigurationException, SAXException {
        List<Integer> ids = new ArrayList<>();
        File inputFile = new File(type + ".xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("word");
        boolean need = false;
        if (word.charAt(0) == '-') {
            word = word.substring(1);
            need = true;
            for (int k = 0; k < 10; k++) {
                ids.add(k);
            }
        }
        for (int i = 0; i < nList.getLength(); i++) {
            Node wordNode = nList.item(i);
            Element wordElement = (Element) wordNode;
            String value = wordElement.getElementsByTagName("value").item(0).getTextContent();

            if (word.equals(value)) {
                Element articlesElement = (Element) wordElement.getElementsByTagName("articles").item(0);
                NodeList articleids = articlesElement.getElementsByTagName("articleId");
                for (int j = 0; j < articleids.getLength(); j++) {
                    Node ArticledNode = articleids.item(j);
                    Element articleElement = (Element) ArticledNode;
                    if (need) {
                        ids.remove(Integer.valueOf(articleElement.getTextContent()));
                    } else {
                        ids.add(Integer.valueOf(articleElement.getTextContent()));
                    }

                }

            }

        }
        return ids;
    }


    //находим пересечение двух списков
    public static <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();

        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }

    public static double tf(Integer wordcount, Integer docCount) {
        double result = (double) wordcount / docCount;
        return result;
    }

    public static double idf(Integer docCount, Integer docsWithWord) {
        double result;
        if (docsWithWord != 0) {
            result = Math.log(docCount / docsWithWord);
        } else {
            result = 0;
        }
        return result;
    }

    public static double tf_idf(Integer wordcount, Integer docCount, Integer docsWithWord) {
        return tf(wordcount, docCount) * idf(docCount, docsWithWord);


    }

    // ищем tf-idf для введенного слова в определенном документе
    public static void findTfIdf(String word, String type, Integer docID) throws ParserConfigurationException, IOException, SAXException {
        File inputFile = new File(type + ".xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("word");
        Integer docsWithWord = 0;
        Integer docCount = 10;
        if (type.equals("porter")) {
            word = getPorterSting(word).trim();
        }
        else if(type.equals("mystem")){
            word = getMyStemSting(word).trim();
        }
        Integer wordcount = getHowMuch(docID, word, type);

        for (int i = 0; i < nList.getLength(); i++) {
            Node wordNode = nList.item(i);
            Element wordElement = (Element) wordNode;
            String value = wordElement.getElementsByTagName("value").item(0).getTextContent();
            if (word.equals(value)) {
                docsWithWord = Integer.valueOf(wordElement.getElementsByTagName("article_count").item(0).getTextContent());
                break;
            }
        }
        double tf_idf = tf_idf(wordcount, docCount, docsWithWord);
        System.out.println("document ID: " + docID + " tf-idf: " + tf_idf);


    }

    //считаем количество вхождений слова в документ (по названию и аннотации)
    public static Integer getHowMuch(Integer docID, String word, String type) throws ParserConfigurationException, IOException, SAXException {
        Integer result = 0;
        String title = "";
        String annotation = "";
        File inputFile = new File("task3.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("article");
        for (int i = 0; i < nList.getLength(); i++) {
            Node articleNode = nList.item(i);
            Element articleElement = (Element) articleNode;
            String id = articleElement.getElementsByTagName("id").item(0).getTextContent();
            if(Integer.parseInt(id) == docID){
                if (type.equals("porter")) {
                    title = articleElement.getElementsByTagName("title").item(1).getTextContent();
                    annotation = articleElement.getElementsByTagName("annotation").item(1).getTextContent();
                }
                else if(type.equals("mystem")){
                    title = articleElement.getElementsByTagName("title").item(2).getTextContent();
                    annotation = articleElement.getElementsByTagName("annotation").item(2).getTextContent();
                }
                String[] arr = title.split(" ");
                for(String s : arr){
                    if(s.equals(word)){
                        result++;
                    }
                }
                arr = annotation.split(" ");
                for(String s : arr){
                    if(s.equals(word)){
                        result++;
                    }
                }
                break;
            }

        }
        return result;
    }
}




