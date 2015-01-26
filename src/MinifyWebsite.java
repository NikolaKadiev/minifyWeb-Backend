import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@WebServlet("/minifyWebsite")
public class MinifyWebsite extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private Document htmlDoc;
	private ArrayList<String> imageSources;
	private int imgNumber = 0;
	private static final String COMPRESSED_IMAGES_PATH = "/home/nikola/workspace/minify/WebContent/compressedImages/";

	public MinifyWebsite()
	{
		super();
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response)
	{
		response.setCharacterEncoding("utf-8");
		String userAgent = request.getHeader("user-agent");
		String url = request.getParameter("url");
		try
		{
			htmlDoc = getHtmlDoc(url, userAgent);
			imageSources = extractImageSources(url, htmlDoc);
			Iterator<String> iter = imageSources.iterator();
			while (iter.hasNext())
			{
				String imageSrc = iter.next();
				BufferedImage image = getImage(imageSrc);
				if (image != null)
				{
					compressJPEGImage(image);
				}

			}
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Document getHtmlDoc(String url, String userAgent)
			throws IOException
	{
		Document doc = Jsoup.connect(url).userAgent(userAgent).get();
		return doc;
	}

	private ArrayList<String> extractImageSources(String host, Document doc)
	{
		ArrayList<String> imageSources = new ArrayList<>();

		for (Element image : htmlDoc.getElementsByTag("img"))
		{
			String src = image.attr("src");
			if (src.contains("http://"))
			{
				imageSources.add(src);
			} else
			{
				imageSources.add(host + src);
			}

		}
		return imageSources;
	}

	private BufferedImage getImage(String imageSrc) throws IOException
	{
		URL imageUrl = new URL(imageSrc);
		if (imageSrc.contains("jpeg") || imageSrc.contains("jpg"))
		{
			BufferedImage image = ImageIO.read(imageUrl);
			return image;
		}
		return null;
	}

	private void compressJPEGImage(BufferedImage image) throws IOException
	{

		File compressedImage = new File(COMPRESSED_IMAGES_PATH + "image" + imgNumber + ".jpg");

		OutputStream outputStream = new FileOutputStream(compressedImage);
		float quality = 0.7f;
		Iterator<ImageWriter> imgWriters = ImageIO
				.getImageWritersByFormatName("jpg");
		if (!imgWriters.hasNext())
		{
			throw new IllegalStateException("No writers found");

		}

		ImageWriter writer = imgWriters.next();
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
		writer.setOutput(ios);

		ImageWriteParam parametars = writer.getDefaultWriteParam();
		parametars.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		parametars.setCompressionQuality(quality);

		writer.write(null, new IIOImage(image, null, null), parametars);

		outputStream.close();
		ios.close();
		writer.dispose();
		imgNumber++;

	}

}
