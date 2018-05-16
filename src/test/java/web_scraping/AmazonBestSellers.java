package web_scraping;

import java.util.List;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class AmazonBestSellers {
	
	private static final String url = "https://www.amazon.com/Best-Sellers-Books-Literature-Fiction/zgbs/books/17";
	
	public static void main(String[] args) throws Exception {
		
		HtmlPage page;
		
		try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);

			page = webClient.getPage(url);
		}
		
		List<HtmlElement> items = page.getByXPath("//div[@class='a-section a-spacing-none p13n-asin']");  
		
		for(HtmlElement item : items) {
			// get <a> text
			HtmlAnchor itemAnchor =  ((HtmlAnchor) item.getFirstByXPath(".//a"));
			System.out.println(itemAnchor.asText());
			
			// get book url
			String link = itemAnchor.getHrefAttribute();
			// Simplify link by removing the last two /'s
			for(int i=0; i<2; i++)
				link = link.substring(0, link.lastIndexOf('/'));
			System.out.println("amazon.co.uk" + link);
			
			// get price
			HtmlElement priceElement =  item.getFirstByXPath(".//span[contains(@class,'price')]");
			System.out.println(priceElement.asText());
		}
	}
}
