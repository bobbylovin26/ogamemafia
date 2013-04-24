import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;

import org.w3c.dom.NamedNodeMap;
import org.yaml.snakeyaml.Yaml;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlMeta;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

public class Main {
	static Map<String,String> settings;
	
	static Properties game = new Properties();

	static HtmlPage page;
	
	public static void dump(HtmlPage page, String filename) throws Exception {
		final String xml = page.asXml();

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
		bw.write(xml);
		bw.close();
	}

	public static void click(HtmlAnchor anchor) throws Exception {
		page = anchor.click();
		final List<FrameWindow> window = page.getFrames();
		page = (HtmlPage)window.get(0).getEnclosingPage();
	}

	public static void updateGame(HtmlPage page) throws Exception {
		Iterator<HtmlElement> iter = page.getHead().getHtmlElementDescendants().iterator();
		while (iter.hasNext()) {
			try {
				HtmlMeta meta = (HtmlMeta) iter.next();

				NamedNodeMap map = meta.getAttributes();

				final String key = map.item(0).getNodeValue();
				final String value = map.item(1).getNodeValue();
				game.setProperty(key, value);
			} catch (ClassCastException exc) {
			}
		}

		System.out.println("ogame-version: "+game.getProperty("ogame-version"));

		System.out.println("ogame-universe: "+game.getProperty("ogame-universe"));
		System.out.println("ogame-universe-speed: "+game.getProperty("ogame-universe-speed"));

		System.out.println("ogame-player-id: "+game.getProperty("ogame-player-id"));
		System.out.println("ogame-player-name: "+game.getProperty("ogame-player-name"));
	}

	public static void home() throws Exception {
	    java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF); 
	    java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
	    
	    Yaml yaml = new Yaml();
	    
	    Reader reader;
	    
	    reader = new FileReader("settings.yaml");
	    Main.settings = (Map<String,String>) yaml.load(reader);

	    reader = new FileReader("queues.yaml");
	    Map<String,ArrayList<String>> queues = (Map<String,ArrayList<String>>)yaml.load(reader);
	    System.out.println(queues);

	    Queue<String> buildings = enqueue(queues, "build");
		Queue<String> researchs = enqueue(queues, "research");
		Queue<String> shipyards = enqueue(queues, "shipyard");

	    final WebClient client = new WebClient(BrowserVersion.FIREFOX_17);

		List<HtmlAnchor> menu;
		menu = login(client);

		Planet planet = new Planet();

		boolean run = true;
		
		while (run) {
			String building = buildings.peek();
			Integer buildingPage = Planet.buildingsPage(building);
			
			String shipyard = shipyards.peek();
			Integer shipyardPage = Planet.shipyardsPage(shipyard);

			String research = researchs.peek();
			Integer researchPage = Planet.researchsPage(research);

			try {
				planet.reset();
				
				if (buildingPage != null) {
					click(menu.get(Page.RESOURCES));
					//dump(page, "resources.html");
					planet.updateResource(page);

					click(menu.get(Page.STATION));
					//dump(page, "station.html");
					planet.updateStation(page);
				}
				
				if (shipyardPage != null) {
					click(menu.get(Page.DEFENSE));
					//dump(page, "defense.html");
					planet.updateDefense(page);
				}
				
				if (researchPage != null) {
					click(menu.get(Page.RESEARCH));
					//dump(page, "research.html");
					planet.updateResearch(page);
				}

				planet.dump();
			} catch (ElementNotFoundException exc) {
				sleep(60000.0);

				menu = login(client);
				continue;
			}
	
			SimpleEntry<String,Integer> buildit = planet.build(building, buildingPage);
			SimpleEntry<String,Integer> researchit = planet.research(research, researchPage);
			SimpleEntry<String,Integer> shipit = planet.ship(shipyard, shipyardPage);

			if (buildit != null) {
				execute(menu, buildings, buildingPage, buildit);
			} else if (researchit != null) {
				execute(menu, researchs, researchPage, researchit);
			} else if (shipit != null) {
			}

			sleep(30000.0);
		}
		
		client.closeAllWindows();
	}

	private static Queue<String> enqueue(Map<String, ArrayList<String>> queues, String key) {
		Queue<String> queue;
		if (queues.get(key) != null) {
	    	queue = new LinkedList<String>(queues.get(key));
	    } else {
	    	queue = new LinkedList<String>();
	    }
		return queue;
	}

	private static void execute(List<HtmlAnchor> menu, Queue<String> queue,
			Integer pageno, SimpleEntry<String, Integer> cando)
			throws Exception {
		final String title = cando.getKey();

		click(menu.get(pageno.intValue()));
		List<HtmlAnchor> link = (List<HtmlAnchor>) page.getByXPath("//a[contains(@title, '"+title+"')]");
		click(link.get(0));
		
		queue.remove();
	}

	private static void sleep(double time) {
		try {
			time = Math.random()*0.5*time+time;
		    Thread.sleep((long)time);							// milliseconds
		} catch (InterruptedException exc) {
		    Thread.currentThread().interrupt();
		}
	}

	private static List<HtmlAnchor> login(final WebClient client)
			throws IOException, MalformedURLException, Exception {
		final HtmlPage homepage = client.getPage("http://ogame.org");

		// we use a POST because filling out the form and clicking the login button doesn't work...
		WebRequest settings = new WebRequest(new URL(
				"http://ogame.org/main/login"), HttpMethod.POST);

		settings.setRequestParameters(new ArrayList<NameValuePair>());
		settings.getRequestParameters().add(new NameValuePair("kid", ""));
		settings.getRequestParameters().add(new NameValuePair("uni", Main.settings.get("universe")));
		settings.getRequestParameters().add(new NameValuePair("login", Main.settings.get("login")));
		settings.getRequestParameters().add(new NameValuePair("pass", Main.settings.get("password")));

		page = client.getPage(settings);
		updateGame(page);

		List<HtmlAnchor> menu = (List<HtmlAnchor>)page.getByXPath("//a[contains(@class, 'menubutton')]");
		//System.out.println(menu);

		//dump(page, "game.html");
		return menu;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("OGameMafia!");

		home();
	}
}
