import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
	ArrayList<String> imageSources;

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
				response.getWriter().write(imageSrc);
				response.getWriter().write("<p></p>");
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

}
