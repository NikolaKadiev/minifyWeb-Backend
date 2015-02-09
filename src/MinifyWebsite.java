import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Host;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@WebServlet("/minifyWebsite")
public class MinifyWebsite extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private Document htmlDoc;
    private static final String ORIGINAL_IMAGES_PATH = "/home/nikola/workspace/minify/WebContent/originalImages/";
    private static final String OPTIMIZED_IMAGES_PATH = "/home/nikola/workspace/minify/WebContent/optimizedImages/";
    private static final String MALFORMED_URL_MESSAGE = "The url address you endered is malformed! ";
    private static final String IOEXCEPTION_MESSAGE = "There was a problem while processing your request! ";
    private static final String UNKNOWN_HOST_MESAGE = "The host you entered does not exist! ";
    static Map<String, BufferedImage> images = new HashMap<>();
  
    public MinifyWebsite()
    {
        super();
    }

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
    {
        ArrayList<String> JPEGImageSources = null;
        String userAgent = request.getHeader("user-agent");
        String url = request.getParameter("url");

        try
        {
            @SuppressWarnings("unused")
            // test for malformed url
            URL enteredUrl = new URL(url);
            images.clear();
            deleteExistingImages(new File(ORIGINAL_IMAGES_PATH));
            deleteExistingImages(new File(OPTIMIZED_IMAGES_PATH));
            htmlDoc = getHtmlDoc(url, userAgent);
            String charset = getCharset(htmlDoc);
            response.setCharacterEncoding(charset);
            JPEGImageSources = extractJPEGImagesSources(url, htmlDoc);

            long start = System.currentTimeMillis();
            saveOriginalJPEGImages(JPEGImageSources);
            long end = System.currentTimeMillis();
            System.out.println("Completed saving images  in : "
                    + ((end - start) / 1000) + "seconds");
            System.out.println("finished saving original images");
            System.out.println("Response charset : " + charset);

            optimalJPEGCompress();
            replaceImages(htmlDoc);
            replaceLinks(htmlDoc, url);
            changeStylesheetsSourcesToAbsoluteUrls(htmlDoc, url);
            changeScriptSourcesToAbsoluteUrls(htmlDoc, url);
            response.getWriter().write(htmlDoc.toString());

        } catch (MalformedURLException e)
        {
            try
            {

                response.getWriter().write(
                        MALFORMED_URL_MESSAGE + e.getMessage());
            } catch (IOException e1)
            {
                e1.printStackTrace();
            }
        } catch (UnknownHostException e)
        {
            try
            {

                response.getWriter()
                        .write(UNKNOWN_HOST_MESAGE + e.getMessage());
            } catch (IOException e1)
            {
                e1.printStackTrace();
            }
        } catch (IOException e)
        {
            try
            {

                response.getWriter()
                        .write(IOEXCEPTION_MESSAGE + e.getMessage());
                e.printStackTrace();
            } catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }

    }

    private Document getHtmlDoc(String url, String userAgent)
            throws IOException
    {
        Document doc = Jsoup.connect(url).userAgent(userAgent).get();
        return doc;
    }

    private ArrayList<String> extractJPEGImagesSources(String url, Document doc)
            throws MalformedURLException
    {
        ArrayList<String> imageSources = new ArrayList<>();
        URL pageUrl = new URL(url);
        String host = pageUrl.getHost();
        System.out.println(host);

        for (Element image : htmlDoc.getElementsByTag("img"))
        {
            String src = image.attr("src");
            if (src.contains("jpg") || src.contains("jpeg"))
            {
                if (src.contains("http://"))
                {
                    imageSources.add(src);
                } else
                {
                    src = addLeadingSlash(src);
                    imageSources.add("http://" + host + src);
                }
            }

        }
        return imageSources;
    }

    private void getImages(ArrayList<String> imageSources) throws IOException
    {
        Iterator<String> iter = imageSources.iterator();

        while (iter.hasNext())
        {
            String imageSrc = iter.next();
            if (imageSrc.contains("jpeg") || imageSrc.contains("jpg"))
            {
                URL imgSrc = new URL(imageSrc);
                BufferedImage image = ImageIO.read(imgSrc);
                images.put(imageSrc, image);
            }
        }

    }

    private void saveOriginalJPEGImages(ArrayList<String> JPEGimageSources)
    {
        try
        {
            getImages(JPEGimageSources);
            System.out.println("Images list size: " + images.size());
            for (Entry<String, BufferedImage> e : images.entrySet())
            {
                if (e.getValue() != null)
                {
                    BufferedImage img = e.getValue();
                    String shortName = getShortendedImageName(e.getKey());
                    File imgPath = new File(ORIGINAL_IMAGES_PATH + shortName);
                    Iterator<ImageWriter> imgWriters = ImageIO
                            .getImageWritersByFormatName("jpg");
                    if (!imgWriters.hasNext())
                    {
                        throw new IllegalStateException("No writers found");

                    }

                    ImageWriter writer = imgWriters.next();
                    ImageOutputStream ios = ImageIO
                            .createImageOutputStream(imgPath);
                    writer.setOutput(ios);

                    ImageWriteParam parametars = writer.getDefaultWriteParam();
                    parametars
                            .setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    parametars.setCompressionQuality(1.0f);
                    writer.write(null, new IIOImage(img, null, null),
                            parametars);
                }
            }

        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void optimalJPEGCompress()
    {

        try
        {
            File originalImagesDir = new File(ORIGINAL_IMAGES_PATH);
            File[] listOfImages = originalImagesDir.listFiles();
            Process proc = null;
            for (File file : listOfImages)
            {
                long start = System.currentTimeMillis();
                String originalImagePath = ORIGINAL_IMAGES_PATH
                        + file.getName();
                String optimizedImagePath = OPTIMIZED_IMAGES_PATH
                        + file.getName();
                System.out.println(originalImagePath);
                System.out.println(optimizedImagePath);
                proc = Runtime.getRuntime().exec(
                        "imgmin " + originalImagePath + " "
                                + optimizedImagePath);
                InputStream in = proc.getInputStream();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(in));
                String line = "";
                while ((line = br.readLine()) != null)
                {
                    System.out.println(line);
                }
                long end = System.currentTimeMillis();
                System.out.println("Compressed image in " + (end - start)
                        + "miliseconds");
            }

        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void replaceImages(Document doc)
    {
        String optimizedImagesProjectPath = "/minify/optimizedImages/";
        for (Element image : doc.getElementsByTag("img"))
        {
            if (image.attr("src").contains("jpg")
                    || image.attr("src").contains("jpeg"))
            {
                String imgSrc = image.attr("src");

                image.attr("src", optimizedImagesProjectPath
                        + getShortendedImageName(imgSrc));
            }
        }
    }

    private void deleteExistingImages(File directory) throws IOException
    {
        FileUtils.cleanDirectory(directory);
    }

    private static String getShortendedImageName(String imgSrc)
    {
        LinkedList<Character> shortName = new LinkedList<Character>();
        StringBuilder builder = new StringBuilder();
        int nameEndPosition = imgSrc.length() - 1;
        Character c;

        while ((c = imgSrc.charAt(nameEndPosition)) != '/')
        {
            if (c == '?')
                c = '-';
            shortName.push(c);
            nameEndPosition--;
        }
        int size = shortName.size();
        for (int i = 0; i < size; i++)
        {
            builder.append(shortName.pop());
        }
        return builder.toString();

    }

    private String getCharset(Document doc)
    {
        for (Element metaTag : doc.getElementsByTag("meta"))
        {
            if (metaTag.hasAttr("charset"))
            {
                return metaTag.attr("charset").trim();
            }

            else if (metaTag.attr("content").contains("charset"))
            {
                String value = metaTag.attr("content");
                String values[] = value.split(";");
                values[1] = values[1].replace("charset=", "");
                String charset = values[1];
                return charset.trim();
            }

        }
        return null;
    }

    private void changeStylesheetsSourcesToAbsoluteUrls(Document doc, String url)
            throws MalformedURLException
    {
        URL pageUrl = new URL(url);
        String host = pageUrl.getHost();
        for (Element element : doc.getElementsByTag("link"))
        {

            if (element.hasAttr("href"))
            {
                String stylesheetUrl = element.attr("href");
                if (stylesheetUrl.contains("http://") == false)
                    element.attr("href", "http://" + host + stylesheetUrl);
            }

        }
    }

    private void changeScriptSourcesToAbsoluteUrls(Document doc, String url)
            throws MalformedURLException
    {
        URL pageUrl = new URL(url);
        String host = pageUrl.getHost();
        for (Element element : doc.getElementsByTag("script"))
        {
            if (element.hasAttr("src"))
            {
                String scriptSrc = element.attr("src");
                if (scriptSrc.contains("http://") == false)
                    element.attr("src", "http://" + host + scriptSrc);
            }
        }
    }

    private void replaceLinks(Document doc, String url)
            throws MalformedURLException
    {
        String minifyWebAddress = "http://minifyweb.ddns.net/minify/minifyWebsite?url=";
        for (Element link : doc.getElementsByTag("a"))
        {
            String href = link.attr("href");
            URL pageUrl = new URL(url);
            String host = pageUrl.getHost();
            if (href.contains("http://"))
            {
                link.attr("href", minifyWebAddress + href);
            } else
            {
                href = addLeadingSlash(href);
                link.attr("href", minifyWebAddress + "http://" + host + href);
            }
        }
    }

    private String addLeadingSlash(String href)
    {
        if (href.startsWith("/") == false)
        {
            return href = "/" + href;
        } else
        {
            return href;
        }
    }

}
