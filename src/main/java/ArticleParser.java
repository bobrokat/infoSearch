import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ArticleParser {

    public static void main(String[] args) throws IOException {

        String site = "http://www.mathnet.ru";
        String url = site +
                "/php/archive.phtml?jrnid=ivm&wshow=issue&year=2015&volume=&volume_alt=&issue=6&issue_alt=&option_lang=rus";
        String GET_ALL_LINKS = "//td[@width='90%']/a[@class='SLink']";

        WebClient webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage page = webClient.getPage(url);

        List<HtmlAnchor> links = page.getByXPath(GET_ALL_LINKS);

        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("article_list");
            doc.appendChild(rootElement);
            for (HtmlAnchor link : links) {
                HtmlPage articletxt = webClient.getPage(site + link.getHrefAttribute());

                // article elements
                Element article = doc.createElement("article");
                rootElement.appendChild(article);

                // title elements

                String titleStr = articletxt.getFirstByXPath("//span[@class='red']/font/text()").toString();
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

                //mystem
                Element title_mystem = doc.createElement("title");
                title_mystem.setAttribute("type", "mystem");
                title_mystem.appendChild(doc.createTextNode(getMyStemSting(titleStr)));
                article.appendChild(title_mystem);


                // url elements
                Element href = doc.createElement("url");
                href.appendChild(doc.createTextNode(site + link.getHrefAttribute()));
                article.appendChild(href);

                // annotation elements
                String annotationStr = articletxt.getFirstByXPath("//table//text()" +
                        "[preceding-sibling::b[contains(text(), 'Аннотация') " +
                        "and following-sibling::b[1]]][1]").toString();


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

                //mystem
                Element annotation_mystem = doc.createElement("annotation");
                annotation_mystem.appendChild(doc.createTextNode(getMyStemSting(annotationStr)));
                annotation_mystem.setAttribute("type", "mystem");
                article.appendChild(annotation_mystem);


                // keyword elements
                String[] keywordstxt = articletxt.getFirstByXPath("//i[preceding-sibling::b[contains(text(), 'Ключевые')]]/text()").toString().split(",");
                Element keywords = doc.createElement("keywords");
                article.appendChild(keywords);
                for (String keywordStr : keywordstxt) {

                    Element keyword = doc.createElement("keyword");
                    keyword.appendChild(doc.createTextNode(keywordStr));
                    keywords.appendChild(keyword);
                }


            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("task2.xml"));
            transformer.transform(source, result);

            System.out.println("File saved!");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

    public static String getPorterSting(String s) {
        String[] s_arr = s.split(" ");
        s = "";
        Porter porter = new Porter();
        for (String string : s_arr) {
            s += " " + porter.stem(string);
        }
        return s;
    }

    public static String getMyStemSting(String s) throws IOException {
        s = s.replaceAll("[^А-Яа-я\\s]", "");
        String[] s_arr = s.split(" ");
        s = "";
        MyStem myStem = new MyStem();
        for (String string : s_arr) {
            s += " " + myStem.stem(string);
        }
        return s;
    }
}

