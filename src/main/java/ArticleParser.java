import Jama.Matrix;
import Jama.SingularValueDecomposition;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ArticleParser {


    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, TransformerException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Do you want to parse articles? Yes/No");
        String answer1 = scanner.nextLine();
        if (answer1.equals("Yes")) {


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
            System.out.println("write phrase");
            String phrase = scanner.nextLine();
            typePhrase(phrase);
        }
        //tf-idf

        System.out.println("Do you want to find tf-idf? Yes/No");
        String answer3 = scanner.nextLine();
        if (answer3.equals("Yes")) {
            System.out.println("write word");
            String words = scanner.nextLine();
            System.out.println("write type: " +
                    "porter or " +
                    "mystem");
            String typeStr = scanner.nextLine();
            if (typeStr.equals("porter") || typeStr.equals("mystem")) {
                FileWriter writer = new FileWriter("task5.txt", true);
                writer.write("phrase: ");
                writer.write(words);
                writer.append('\n');
                writer.flush();


                //double score;

                String[] wordsArr = words.split(" ");
                Double[] scores = new Double[10];
                for (int i = 0; i < scores.length; i++) {
                    scores[i] = Double.valueOf(0);
                }
                for (String word : wordsArr) {
                    writer = new FileWriter("task5.txt", true);
                    writer.write("word: ");
                    writer.write(word);
                    writer.append('\n');
                    writer.flush();
                    //ArrayList<Integer> intesections = typePhrase(words);
                    for (int i = 0; i < 10; i++) {
                        scores[i] += findTfIdf(word, typeStr, i);

                    }

                }
                Map<Integer, Double> map = new HashMap<>();
                for (int i = 0; i < scores.length; i++) {
                    map.put(i, scores[i]);
                }
                Map<Integer, Double> sortedmap = sortByComparator(map, false);


                writer = new FileWriter("task5.txt", true);
                for (Map.Entry<Integer, Double> entry: sortedmap.entrySet()) {
                    if (entry.getValue() != 0) {
                        writer.write("Score for doc №" + String.valueOf(entry.getKey()) + " : " + String.valueOf(entry.getValue()));
                        writer.append('\n');
                    }
                }
                writer.flush();


            }

        }

        System.out.println("Do you want to find Latent Semantic Indexing? Yes/No");
        String answer4 = scanner.nextLine();
        if (answer4.equals("Yes")) {
            System.out.println("write query");
            String query = scanner.nextLine();
            FileWriter writer = new FileWriter("task6.txt", true);
            writer.write("query: ");
            writer.write(query);
            writer.append('\n');
            writer.flush();
            Matrix a = getAMatrix();
            Matrix q = getQMarix(query);
            getLSI(a, q);
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
    public static ArrayList<Integer> typePhrase(String phrase) throws IOException, ParserConfigurationException, SAXException {
        FileWriter writer = new FileWriter("task4.txt", true);
        ArrayList<Integer> intersections = new ArrayList();
        Scanner scanner = new Scanner(System.in);
        writer.write("phrase : ");
        writer.write(phrase);
        writer.append('\n');
        String[] words = phrase.trim().split(" ");
        if (words.length <= 1) {
            System.out.println("Please type more than 1 word");
        } else {
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
            writer.write("intersection : ");
            for (Integer value : result) {
                writer.write(value.toString());
                writer.append(' ');
                System.out.println(value);
                intersections.add(value);

            }
            writer.append('\n');
            writer.flush();
        }
        return intersections;

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
    public static double findTfIdf(String word, String type, Integer docID) throws ParserConfigurationException, IOException, SAXException {
        FileWriter writer = new FileWriter("task5.txt", true);
        File inputFile = new File(type + ".xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        Integer docsWithWord;
        Integer docCount = 10;
        if (type.equals("porter")) {
            word = getPorterSting(word).trim();
        } else if (type.equals("mystem")) {
            word = getMyStemSting(word).trim();
        }
        Integer wordcount = getHowMuch(docID, word, type);

        docsWithWord = getDocsWithWord(word);
        double tf_idf = tf_idf(wordcount, docCount, docsWithWord);
       // System.out.println("document ID: " + docID + " tf-idf: " + tf_idf);
        //writer.write("document ID: " + docID.toString() + " tf-idf: " + tf_idf);
       // writer.append('\n');
        writer.flush();
        return tf_idf;


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
            if (Integer.parseInt(id) == docID) {
                if (type.equals("porter")) {
                    title = articleElement.getElementsByTagName("title").item(1).getTextContent();
                    annotation = articleElement.getElementsByTagName("annotation").item(1).getTextContent();
                } else if (type.equals("mystem")) {
                    title = articleElement.getElementsByTagName("title").item(2).getTextContent();
                    annotation = articleElement.getElementsByTagName("annotation").item(2).getTextContent();
                }
                String[] arr = title.split(" ");
                for (String s : arr) {
                    if (s.equals(word)) {
                        result++;
                    }
                }
                arr = annotation.split(" ");
                for (String s : arr) {
                    if (s.equals(word)) {
                        result++;
                    }
                }
                break;
            }

        }
        return result;
    }

    //создание документа
    public static Document getDoc(String type) throws ParserConfigurationException, IOException, SAXException {

        File inputFile = new File(type + ".xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        return doc;
    }

    // получение всех уникальных слов
    public static List<String> getAllWords() throws ParserConfigurationException, IOException, SAXException {
        List<String> list = new ArrayList<>();
        Document doc = getDoc("porter");
        NodeList nList = doc.getElementsByTagName("word");
        for (int i = 0; i < nList.getLength(); i++) {
            Node wordNode = nList.item(i);
            Element wordElement = (Element) wordNode;
            String value = wordElement.getElementsByTagName("value").item(0).getTextContent();
            list.add(value);
        }
        return list;
    }

    // поиск числа документов содержащих слово
    public static int getDocsWithWord(String word) throws IOException, SAXException, ParserConfigurationException {
        Integer docsWithWord = 0;
        Document doc = getDoc("porter");
        NodeList nList = doc.getElementsByTagName("word");
        for (int i = 0; i < nList.getLength(); i++) {
            Node wordNode = nList.item(i);
            Element wordElement = (Element) wordNode;
            String value = wordElement.getElementsByTagName("value").item(0).getTextContent();
            if (word.equals(value)) {
                docsWithWord = Integer.valueOf(wordElement.getElementsByTagName("article_count").item(0).getTextContent());
                break;
            }
        }
        return docsWithWord;

    }

    // полчение матрицы А
    public static Matrix getAMatrix() throws ParserConfigurationException, IOException, SAXException {

        List<String> nList = getAllWords();
        double[][] arr = new double[nList.size()][10];
        for (int i = 0; i < nList.size(); i++) {
            for (int j = 0; j < 10; j++) {
                arr[i][j] = findTfIdf(nList.get(i), "porter", j);
            }

        }
        Matrix a = new Matrix(arr);
        a.print(2, 3);
        return a;
    }

    //получение матрицы q
    public static Matrix getQMarix(String query) throws IOException, SAXException, ParserConfigurationException {
        query = getPorterSting(query);
        String[] arr = query.split(" ");
        ArrayList<String> querylist = new ArrayList<>();
        querylist.addAll(Arrays.asList(arr));
        Collections.sort(querylist);
        List<String> nList = getAllWords();
        double[][] matrixArr = new double[1][nList.size()];


        for (int i = 0; i < nList.size(); i++) {
            String word = nList.get(i);
            if (querylist.contains(word)) {
                int count = 0;
                for (String w : querylist) {
                    if (w.equals(word)) {
                        count++;
                    }
                }
                double localTf = tf(count, querylist.size());
                int docsWithWord = getDocsWithWord(word);
                double localIdf = idf(10, docsWithWord);

                matrixArr[0][i] = localTf * localIdf;
            }
        }
        return new Matrix(matrixArr);
    }

    // вычисление LSI
    public static void getLSI(Matrix a, Matrix q) throws IOException {
        FileWriter writer = new FileWriter("task6.txt", true);
        SingularValueDecomposition sin = new SingularValueDecomposition(a);
        Matrix u = sin.getU();
        Matrix s = sin.getS();

        Matrix v = sin.getV();


        Matrix uK = u.getMatrix(0, u.getRowDimension() - 1, 0, 5);
        Matrix sK = s.getMatrix(0, s.getRowDimension() - 1, 0, 5);
        sK = sK.getMatrix(0, 5, 0, sK.getColumnDimension() - 1);
        Matrix vK = v.getMatrix(0, v.getRowDimension() - 1, 0, 5);


        Matrix newQ = q.times(uK).times(sK.inverse());


        for (int i = 0; i < vK.getRowDimension(); i++) {
            double sim;
            double simNum = 0;
            double simDen = 0;
            for (int j = 0; j < newQ.getColumnDimension(); j++) {
                simDen += Math.pow(newQ.get(0, j), 2);
            }
            simDen = Math.sqrt(simDen);
            double simDenVk = 0;
            for (int j = 0; j < vK.getColumnDimension(); j++) {
                simNum += vK.get(i, j) * newQ.get(0, j);
                simDenVk += Math.pow(vK.get(i, j), 2);
            }
            simDen = simDen * Math.sqrt(simDenVk);
            if (simDen == 0) {
                sim = 0;
            } else {
                sim = simNum / simDen;
            }
            System.out.printf("%.6f", sim);
            System.out.println();
            writer.write("sim (q, d"+ String.valueOf(i) + ") :" + String.format("%.6f", sim));
            writer.append('\n');


        }
        writer.flush();
    }

    private static Map<Integer, Double> sortByComparator(Map<Integer, Double> unsortMap, final boolean order) {

        List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> o1,
                               Map.Entry<Integer, Double> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<Integer, Double> sortedMap = new LinkedHashMap<Integer, Double>();
        for (Map.Entry<Integer, Double> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}



