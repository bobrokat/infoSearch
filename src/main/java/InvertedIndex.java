import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;

public class InvertedIndex {



    Set<String> set = new HashSet<>();
    Map<String, List<Integer>> map = new TreeMap<>();



    public void getIndex(String textStr, int i){
        String[] s_arr = textStr.split(" ");
        set.addAll(Arrays.asList(s_arr));
        set.remove("");
        for(String string: s_arr){
            if (set.contains(string)){
                if (!map.containsKey(string)){
                    List<Integer> list = new ArrayList<>();
                    list.add(i);
                    map.put(string,list );
                }
                else {
                    if (!map.get(string).contains(i)){
                        map.get(string).add(i);
                    }

                }

            }
        }

    }



    public void createFile(String filename) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("index");
        doc.appendChild(rootElement);

        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
            Element word = doc.createElement("word");
            rootElement.appendChild(word);

            Element value = doc.createElement("value");
            value.appendChild(doc.createTextNode(entry.getKey()));
            word.appendChild(value);

            Element articles = doc.createElement("articles");
            word.appendChild(articles);

            for( Integer integer : entry.getValue()) {
                Element articleId = doc.createElement("articleId");
                articleId.appendChild(doc.createTextNode(integer.toString()));
                articles.appendChild(articleId);
            }

            Element article_count = doc.createElement("article_count");
            article_count.appendChild(doc.createTextNode(String.valueOf(entry.getValue().size())));
            word.appendChild(article_count);


        }

        // write the content into  xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File( filename + ".xml"));
        transformer.transform(source, result);



    }
}
