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
                "/php/archive.phtml?jrnid=ivm&wshow=issue&year=2013&volume=&volume_alt=&issue=12&issue_alt=&option_lang=rus";
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
                Element title = doc.createElement("title");
                title.appendChild(doc.createTextNode(articletxt.getFirstByXPath("//span[@class='red']/font/text()").toString()));
                article.appendChild(title);

                // url elements
                Element href = doc.createElement("url");
                href.appendChild(doc.createTextNode(site + link.getHrefAttribute()));
                article.appendChild(href);

                // annotation elements
                Element annotation = doc.createElement("annotation");
                annotation.appendChild(doc.createTextNode(articletxt.getFirstByXPath("//table//text()" +
                        "[preceding-sibling::b[contains(text(), 'Аннотация') " +
                        "and following-sibling::b[1]]][1]").toString()));
                article.appendChild(annotation);



                // keyword elements
                String[] keywordstxt = articletxt.getFirstByXPath("//i[preceding-sibling::b[contains(text(), 'Ключевые')]]/text()").toString().split(",");
                Element keywords = doc.createElement("keywords");
                article.appendChild(keywords);
                for (int i = 0; i < keywordstxt.length; i++) {

                    Element keyword = doc.createElement("keyword");
                    keyword.appendChild(doc.createTextNode(keywordstxt[i]));
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
                StreamResult result = new StreamResult(new File("file.xml"));
                transformer.transform(source, result);

                System.out.println("File saved!");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }
    }

