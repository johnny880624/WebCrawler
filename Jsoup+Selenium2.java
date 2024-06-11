package selenium;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.JavascriptExecutor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.time.Duration;

public class Final2 {
	private static final int CONNECTION_TIMEOUT = 10000;

	public static void main(String[] args) {
		//String url = "https://member.taiwanmobile.com/MemberCenter/changePassword/begin.do?_gl=1*134sic9*_gcl_au*MTg5MjYyNTMxNi4xNzE4MDY4NTk0*_ga*NTU5ODI0NTMyLjE3MTgwNjg1ODg.*_ga_7BMCQP1B1C*MTcxODA2ODU5NC4xLjEuMTcxODA2ODYwMy41MS4wLjA.";
		String url = "https://www.taiwanmobile.com/index.html";
		WebDriver driver = setupWebDriver(); // Initialize WebDriver
		ArrayList<ArrayList<String>> visited = initializeLists(); //Initialize nested ArrayList
		HashMap<Integer, String> map = initializeMap(); // Initialize HashMap
		crawl(url, visited, driver);
		// Quit the WebDriver
		driver.quit();
		// Display
		resultDisplay(visited, map);
		
	}
	
	// Selenium WebDriver setup
	private static WebDriver setupWebDriver() {
		System.setProperty("webdriver.chrome.driver", "/Users/kuan/Desktop/selenium/ChromeDriver/chromedriver");
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless"); // Run headless browser
		options.addArguments("--disable-gpu");
		options.addArguments("--no-sandbox");
		return new ChromeDriver(options);
	}

	private static void crawl(String url, ArrayList<ArrayList<String>> visited, WebDriver driver) {
		Document doc = fetchDocument(url, visited, driver);
		System.out.println("Finished!");
	}


	private static Document fetchDocument(String url, ArrayList<ArrayList<String>> visited, WebDriver driver) {
		System.out.println("Loading...");
		disableSSLVerification();
		try {

			long startTime = System.nanoTime();
			driver.get(url);

			// Define a custom expected condition to wait for DOMContentLoaded
			ExpectedCondition<Boolean> documentReady = new ExpectedCondition<Boolean>() {
				public Boolean apply(WebDriver driver) {
					return ((String)((JavascriptExecutor)driver).executeScript("return document.readyState")).equals("complete");
				}
			};
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(CONNECTION_TIMEOUT / 1000));
			wait.until(documentReady);

			long endTime = System.nanoTime();
			System.out.println("Page has loaded completely.");

			// Calculate the elapsed time in nanoseconds
			long duration = endTime - startTime;

			// Convert to milliseconds if needed
			double durationInMilliseconds = duration / 1_000_000.0;
			System.out.println("Elapsed time in milliseconds: " + durationInMilliseconds);

			String pageSource = driver.getPageSource();
			Document doc = Jsoup.parse(pageSource, url);

			if (doc != null) {
				// Process the document
				writeTheDocument(doc);

				// 0. Current URLurl
				visited.get(0).add(url);
				
				// 7. Check redirection
				checkForRedirect(url, visited);

				// 1. Website's title
				visited.get(1).add(doc.title());

				// 2. <meta name = "keywords"> contents
				Element keywordsMetaTag = doc.selectFirst("meta[name=keywords]");
				if (keywordsMetaTag != null) {
					String keywordsContent = keywordsMetaTag.attr("content");
					visited.get(2).add(keywordsContent);
				} else {
					visited.get(2).add("Keywords meta tag not found.");
				}

				// 3. <meta name = "descriptions"> contents
				Element descriptionMetaTag = doc.selectFirst("meta[name=description]");
				if (descriptionMetaTag != null) {
					String descriptionContent = descriptionMetaTag.attr("content");
					visited.get(3).add(descriptionContent);
				} else {
					visited.get(3).add("Description meta tag not found.");
				}

				// 4. <h1> tag contents (Jsoup)
				Elements h1Tags = doc.select("h1");
				if (!h1Tags.isEmpty()) {
					for (Element h1 : h1Tags) {
						visited.get(4).add(h1.text());
					}
				} else {
					visited.get(4).add("h1 tag not found.");
				}

				// 5. <h2> tag contents (Jsoup)
				Elements h2Tags = doc.select("h2");
				if (!h2Tags.isEmpty()) {
					for (Element h2 : h2Tags) {
						visited.get(5).add(h2.text());
					}
				} else {
					visited.get(5).add("h2 tag not found.");
				}

				// 6. <h3> tag contents (Jsoup)
				Elements h3Tags = doc.select("h3");
				if (!h3Tags.isEmpty()) {
					for (Element h3 : h3Tags) {
						visited.get(6).add(h3.text());
					}
				} else {
					visited.get(6).add("h3 tag not found.");
				}

				return doc;
			}
		} catch (Exception e) {
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
	
	private static HashMap<Integer, String> initializeMap(){
		HashMap<Integer, String> map = new HashMap<>();
		map.put(0, "1. Url: ");
		map.put(1, "2. Title: ");
		map.put(2, "3. Keyword: ");
		map.put(3, "4. Description: ");
		map.put(4, "5. H1: ");
		map.put(5, "6. H2: ");
		map.put(6, "7. H3: ");
		map.put(7, "8. Checking Redirected: ");
		return map;
	}
	
	// Initialize ArrayList of ArrayList
	private static ArrayList<ArrayList<String>> initializeLists(){
		ArrayList<ArrayList<String>> listOfLists = new ArrayList<>();
		ArrayList<String> urlList = new ArrayList<>();
		ArrayList<String> titleList = new ArrayList<>();
		ArrayList<String> keywordsList = new ArrayList<>();
		ArrayList<String> descriptionsList = new ArrayList<>();
		ArrayList<String> h1TagsList = new ArrayList<>();
		ArrayList<String> h2TagsList = new ArrayList<>();
		ArrayList<String> h3TagsList = new ArrayList<>();
		ArrayList<String> isRedirect = new ArrayList<>();
		Collections.addAll(listOfLists, urlList, titleList, keywordsList, descriptionsList, h1TagsList, h2TagsList, h3TagsList, isRedirect);
		return listOfLists;
	}

	private static void writeTheDocument(Document doc) {
		String content = doc.html();

		try(BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/kuan/Desktop/test4.txt"))){
			writer.write(content);
			writer.close();
		} catch (IOException e){

		}
	}
	
    public static void checkForRedirect(String urlString, ArrayList<ArrayList<String>> visited) throws IOException {
    	try {
	        URL url = new URL(urlString);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setInstanceFollowRedirects(false); // Don't automatically follow redirects
	        connection.setRequestMethod("GET");
	        connection.connect();
	
	        int responseCode = connection.getResponseCode();
	        if (responseCode >= 300 && responseCode < 400) {
	            // Handle different types of redirects
	            String newUrl = connection.getHeaderField("Location");
	            visited.get(7).add(newUrl);
	            //System.out.println("Redirected to: " + newUrl);
	        } else {
	        	visited.get(7).add("Url is not redirected.");
	        }
    	} catch(IOException e) {
    		System.out.println(e.getMessage());
    	}
    }
	
	private static void resultDisplay(ArrayList<ArrayList<String>> visited, HashMap<Integer, String> map) {
		System.out.println("");
		System.out.println("Result: ");
		System.out.println("--------------------------------------------------------------------------------------------------------");	
		int index = 0;
		for (ArrayList<String> row :visited) {
			System.out.println(map.get(index));
			for (String element : row) {
				System.out.println(element);
			}
			index ++;
			System.out.println("--------------------------------------------------------------------------------------------------------");	
		}
	}


}