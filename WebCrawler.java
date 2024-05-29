package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;



public class FinalVersion {
	private static final int MAX_RETRIES = 3;
	private static final int CONNECTION_TIMEOUT = 15000; // 15 seconds

	public static void main(String[] args) {
		String url = "https://www.taiwanmobile.com/index.html";
		int dataCount = 0;
		ArrayList<ArrayList<String>> listOfLists = initializeLists();
		ArrayList<String> unableConnectedUrl = new ArrayList<>();
		crawl(1, url, listOfLists, unableConnectedUrl);
		
//		ArrayList<ArrayList<String>> transposed = transpose(listOfLists);
//		for (int i = 0 ; i < transposed.size() ; i++) {
//			for (int j = 0; j< transposed.get(i).size(); j++) {
//				System.out.println(transposed.get(i).get(j));
//			}
//			System.out.println("--------------------------------------------------------------------------------------------------------");	
//		}
		
		for (ArrayList<String> row : listOfLists ) {
			boolean countTrue = true;
			for (String element : row) {
				//System.out.println(element);
				if (countTrue) {
					dataCount ++;
				}
			}
			countTrue = false;
			//System.out.println("--------------------------------------------------------------------------------------------------------");	
		}
		
		for (String element : unableConnectedUrl) {
			System.out.println(element);
		}
		
		System.out.println("Successful Collected Number: " + dataCount);
		System.out.println("Unable Collected Number: " + unableConnectedUrl.size());
		boolean hasDuplicates = hasDuplicates(listOfLists.get(1));
        System.out.println("List has duplicates: " + hasDuplicates);
	}

	private static void crawl(int level, String url, ArrayList<ArrayList<String>> visited, ArrayList<String> unableConnectedUrl) {
		if(level <= 6) {
			if (isWantedDomain(url)) {	
//				if (isUnsupportedContentType(url)){
//					return;
//				}
				Document doc = fetchDocumentWithRetries(url, MAX_RETRIES, level, visited, unableConnectedUrl);

				if (doc != null) {
					for (Element link : doc.select("a[href]")) {
						String next_link = link.absUrl("href").trim();
						if (!next_link.endsWith(".pdf") && !next_link.endsWith(".png")) {
							try {
								URI uri = new URI(next_link);
								String cleanedUrl = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null).toString();
								cleanedUrl = cleanedUrl.trim();
								if(!visited.get(1).contains(cleanedUrl)){
									crawl(level+1, cleanedUrl, visited, unableConnectedUrl);
								}
							} catch (URISyntaxException e) {
								//System.err.println("Invalid URI: " + e.getMessage());
							}
						}
					}
				}
			}
		}	
	}
	
	// Acquire the website's HTML documents
	private static Document fetchDocumentWithRetries(String url, int maxRetries, int level, ArrayList<ArrayList<String>> v, ArrayList<String> unableConnectedUrl) {
		int attempt = 0;
		// Attempts to extract the contents of a URL, retrying up to a maximum number of attempts (MAX_RETRIES) if the initial attempts fail.
		while (attempt < maxRetries) {
			disableSSLVerification();
			try {
				Connection.Response response = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(CONNECTION_TIMEOUT).ignoreContentType(true).execute();
				// Checking response content type, making sure it's supported by Jsoup package.
				String contentType = response.contentType();
				if (isSupportedContentType(contentType)){		
					// if it's supported by Jsoup, we parse the content such as print out URL, title, keywords, and descriptions.
					Document doc;
					if(response.statusCode() ==200) {
						doc = response.parse();
						// 0. level
						v.get(0).add(Integer.toString(level));
						//System.out.println("Level: " + level);
						// 1. Current URL
						v.get(1).add(url);
						//System.out.println("Link: "+ url);
						// 2. Website's title
						v.get(2).add(doc.title());
						//System.out.println(doc.title());
						// 3. <meta name = "keywords"> contents
						Element keywordsMetaTag = doc.selectFirst("meta[name=keywords]");
						if (keywordsMetaTag != null) {
							String keywordsContent = keywordsMetaTag.attr("content");
							v.get(3).add(keywordsContent);
							//System.out.println("keywords: " + keywordsContent);
						} else {
							v.get(3).add("Keywords meta tag are not found. ");
							//System.out.println("Keywords meta tag are not found. ");
						}

						// 4. <meta name = "descriptions"> contents
						Element descriptionMetaTag = doc.selectFirst("meta[name=description]");
						if (descriptionMetaTag != null) {
							String descriptionContent = descriptionMetaTag.attr("content");
							v.get(4).add(descriptionContent);
							//System.out.println("Description: " + descriptionContent);
						} else {
							v.get(4).add("Description meta tag not found.");
							//System.out.println("Description meta tag not found.");
						}
						// Separation Line.
						//System.out.println("---------------------------------------------------------------------------");
						return doc;
					}
					return null;
				} else {
					// Print out the unsupported types.
					System.err.println("Unsupported content type: " + contentType);
					System.out.println(url);
					return null;
				}
			} catch (IOException e) { // print out the error Message
				attempt++;
				System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
				if (!unableConnectedUrl.contains(url)) {
					unableConnectedUrl.add(url);
				}
				if (attempt >= maxRetries) {
					return null;
				}
				try {
					Thread.sleep(2000); // Wait for 2 seconds before retrying
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}
		return null;
	}
	
	// Disable the SSL verification
	private static void disableSSLVerification() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}

						public void checkClientTrusted(X509Certificate[] certs, String authType) {
						}

						public void checkServerTrusted(X509Certificate[] certs, String authType) {
						}
					}
			};

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			HostnameVerifier allHostsValid = (hostname, session) -> true;
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Checking whether the url is the format of "http" or "https".
	private static boolean isValidHttpUrl(String url) {
		return url.startsWith("http://") || url.startsWith("https://");
	}
	
	private static boolean isWantedDomain(String url) {
		ArrayList<String> DomainList = new ArrayList<>();
		DomainList.add("https://www.taiwanmobile.com");
		DomainList.add("https://m.taiwanmobile.com");
		DomainList.add("https://cs.taiwanmobile.com");
		DomainList.add("https://www.twmsolution.com");
		DomainList.add("https://corp.taiwanmobile.com");
		DomainList.add("https://payment.taiwanmobile.com");
		DomainList.add("https://search.taiwanmobile.com");
		DomainList.add("https://tstarcs.taiwanmobile.com");
		DomainList.add("https://ow.tstarcs.taiwanmobile.com");
		DomainList.add("https://tspimg.tstarcs.taiwanmobile.com");
		DomainList.add("https://english.taiwanmobile.com");
		DomainList.add("https://service.taiwanmobile.com");
		for(int i= 0; i< DomainList.size(); i++ ) {
			if (url.startsWith(DomainList.get(i))) {
				return true;
			} 
		}
		return false;
	}
	
	
	// Distinguish whether content type of url is supported by jsoup
	private static boolean isSupportedContentType(String contentType) {
		if (contentType != null && (contentType.contains("text/html") || 
				contentType.contains("application/xhtml+xml") ||
				contentType.contains("application/xml") ||
				contentType.contains("text/xml") ||
				contentType.contains("text/plain") ||
				contentType.matches(".*\\+xml.*"))) {
			return true;
		} return false;
	}
	
	// Initialize ArrayList of ArrayList
	private static ArrayList<ArrayList<String>> initializeLists(){
		ArrayList<ArrayList<String>> listOfLists = new ArrayList<>();
		ArrayList<String> levelList = new ArrayList<>();
		ArrayList<String> urlList = new ArrayList<>();
		ArrayList<String> titleList = new ArrayList<>();
		ArrayList<String> keywordsList = new ArrayList<>();
		ArrayList<String> descriptionsList = new ArrayList<>();
		Collections.addAll(listOfLists, levelList, urlList, titleList, keywordsList, descriptionsList);
		return listOfLists;
	}
	
	// Transpose the Matrix in order to print out the result
	private static ArrayList<ArrayList<String>> transpose(ArrayList<ArrayList<String>> original) {
		if (original == null || original.isEmpty()) {
			return new ArrayList<>();
		}

		int originalRowCount = original.size();
		int originalColCount = original.get(0).size();

		// Initializing the transposed ArrayList of ArrayList
		ArrayList<ArrayList<String>> transposed = new ArrayList<>(originalColCount);
		for (int i = 0; i < originalColCount; i++) {
			transposed.add(new ArrayList<>(originalRowCount));
		}

		// Populating the transposed ArrayList
		for (int i = 0; i < originalRowCount; i++) {
			for (int j = 0; j < originalColCount; j++) {
				transposed.get(j).add(original.get(i).get(j));
			}
		}
		return transposed;
	}
	
	// Double check there is no duplicate.
	public static <T> boolean hasDuplicates(ArrayList<T> list) {
		Set<T> set = new HashSet<>();
		for (T item : list) {
			if (!set.add(item)) {
				return true; // Duplicate found
			}
		}
		return false; // No duplicates found
	}
	
	private static boolean isUnsupportedContentType(String urlString) {
	    try {
	    	disableSSLVerification();
	        URL url = new URL(urlString);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("HEAD");
	        connection.connect();
	        String contentType = connection.getContentType();
	        connection.disconnect();
	        return contentType.contains("pdf");
	    } catch (IOException e) {
	        e.printStackTrace();
	        return false; // Treat as unsupported if we cannot determine the content type
	    }
	}

}