package edu.uci.ics.crawler4j.examples.basic;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;




public class BasicCrawler extends WebCrawler {
	private static final AtomicInteger linksCount = new AtomicInteger(0);
	private static final AtomicInteger urlVisitCount = new AtomicInteger(0);
	private static final AtomicInteger currCountCounter = new AtomicInteger(0);
	private static final Map<String, String> mapURLS = new ConcurrentHashMap<>();
	Map<String, String> localURLs;

	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js"
            + "|wav|avi|mov|mpeg|mpg|ram|m4v|wma|wmv|mid|txt" + "|mp2|mp3|mp4|zip|rar|gz|exe))$");
	
	CrawlState crawlState;

    public BasicCrawler() {
        crawlState = new CrawlState();
        localURLs = new ConcurrentHashMap<>();
    }
	private static File storageFolder;

    public static void configure(String storageFolderName) {
        storageFolder = new File(storageFolderName);
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }
    }
	
	@Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
		int count = urlVisitCount.incrementAndGet();
		logger.info("Should Visit Count is" + count);
		String href = url.getURL().replaceAll(",", "-");
		String contentType = referringPage.getContentType();
		if(contentType != null) {
			contentType = contentType.split(";")[0];
		}
       /* // Ignore the url if it has an extension that matches our defined set of image extensions.
		if (contentType == null || ((href.startsWith("https://www.reuters.com/") || href.startsWith("http://www.reuters.com/")) &&
				(contentType.equals("text/html") || contentType.equals("application/pdf") 
		        		|| contentType.contains("image/jpeg") || contentType.contains("image/gif")
		        		|| contentType.contains("image/png")))) {
			URL url1;
			try {
				url1 = new URL(url.toString());
				URLConnection conn = url1.openConnection();
				// get header by 'key'
				contentType = conn.getHeaderField("Content-Type");
				if (contentType != null) {
					contentType = contentType.split(";")[0];
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			} catch (MalformedURLException e) {
				logger.info("Error in get header field", e);
			} catch (IOException e) {
				logger.info("Error in get header field", e);
			} catch (Exception e) {
				logger.info("Error in get header field", e);
			}
		}*/
        String type = "";
        if(href.startsWith("https://www.reuters.com/") || href.startsWith("http://www.reuters.com/"))
        {
        	type="OK";
        }
        else
        {
        	type="N_OK";
        }
       
        crawlState.discoveredUrls.add(new UrlInfo(href, type));
        if(mapURLS.containsKey(href)) {
        	logger.info("URL - " + href + " is already visited");
        	return false;
        }
        boolean checkContentType = false;
        if (contentType != null && (contentType.equals("text/html") || contentType.equals("application/pdf") 
        		|| contentType.contains("image/jpeg") || contentType.contains("image/gif")
        		|| contentType.contains("image/png"))) {
        	String[] splitURL = href.split("/");
        	String lastContent = splitURL[splitURL.length -1];
        	if(href.equals("https://www.reuters.com/investigates/special-report/assets/usa-taser-vulnerable/related-graphic") 
        			|| lastContent.contains("css") || lastContent.contains("js") || lastContent.contains("favicon")) {
            	checkContentType = false;
        	} else {
        		checkContentType = true;
        	}
        }
        logger.info("CONTENT-TYPE CHECK=====" + checkContentType + "URL=======" + url + "CONTENT-TYPE======" + contentType);
        return checkContentType && !FILTERS.matcher(href).matches() && (href.startsWith("https://www.reuters.com/") || href.startsWith("http://www.reuters.com/"));
    }

	@Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL().replaceAll(",", "-");
		mapURLS.put(url, "");
        String contentType = page.getContentType().split(";")[0];
        logger.info("URL: {}", url);
        
        UrlInfo urlInfo;
        int value = 0;
        if (contentType.equals("text/html")) { // html
            if (page.getParseData() instanceof HtmlParseData) {
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                Set<WebURL> links = htmlParseData.getOutgoingUrls();
                int currCount = 0;
                for (WebURL link : links) {
                	String urlLink = link.getURL().replaceAll(",", "-");
                	if(!localURLs.containsKey(urlLink)) {
                		localURLs.put(urlLink, "");
                		currCount++;
                	}
                }
                logger.info("Curr Count = " + currCountCounter.getAndAdd(currCount));
                value= linksCount.addAndGet(links.size());
                urlInfo = new UrlInfo(url, page.getContentData().length, currCount, "text/html", ".html");
                crawlState.visitedUrls.add(urlInfo);
            }
            
            else {
                urlInfo = new UrlInfo(url, page.getContentData().length, 0, "text/html", ".html");
                crawlState.visitedUrls.add(urlInfo);
            }
        } else if (contentType.equals("application/pdf")) { // pdf
            urlInfo = new UrlInfo(url, page.getContentData().length, 0, "application/pdf", ".pdf");
            crawlState.visitedUrls.add(urlInfo);
        } else if(contentType.contains("image/jpeg") || contentType.contains("image/gif")
        		|| contentType.contains("image/png")) { 
        	urlInfo = new UrlInfo(url, page.getContentData().length, 0, contentType, ".jpg");
        	crawlState.visitedUrls.add(urlInfo);
        }
        else {
        	logger.info("URL_UNKNOWN: " + url + "----Type: " + contentType);
        }
        logger.info("Visited urls added is " + value);
	}
        @Override
        protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
            crawlState.attemptUrls.add(new UrlInfo(webUrl.getURL(), statusCode));
        }

        @Override
        public Object getMyLocalData() {
            return crawlState;
        }
    }