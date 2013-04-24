import java.text.NumberFormat;
import java.text.ParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;

public class Planet {
	private static Logger logger = Logger.getLogger(Planet.class.getName());
	
	final static List<String> names = Arrays.asList(
			"Metal", "Crystal", "Deuterium",
			"Energy");
	
	final static int METAL = 0, CRYSTAL = 1, DEUTERIUM = 2, ENERGY = 3;

	int[] resources;

	final static List<String> statusNames = Arrays.asList(
			"Queue is full", "Not enough resources!", "Requirements are not met");

	HashMap<String,String> status;
	HashMap<String,String> refs;
	HashMap<String,HtmlAnchor> links;

	final static List<String> resourceNames = Arrays.asList(
			"Metal Mine", "Crystal Mine", "Deuterium Synthesizer",
			"Solar Plant", "Fusion Reactor", "Solar Satellite",
			"Metal Storage", "Crystal Storage", "Deuterium Tank",
			"Shielded Metal Den", "Underground Crystal Den", "Seabed Deuterium Den");
	final static List<String> stationNames = Arrays.asList(
			"Robotics Factory", "Shipyard", "Research Lab", "Alliance Depot",
			"Missile Silo", "Nanite Factory", "Terraformer");

	HashMap<String,Integer> building;

	final static List<String> defenseNames = Arrays.asList(
			"Rocket Launcher", "Light Laser", "Heavy Laser", "Gauss Cannon", "Ion Cannon", "Plasma Turret",
			"Small Shield Dome", "Large Shield Dome",
			"Anti-Ballistic Missiles", "Interplanetary Missiles");

	HashMap<String,Integer> shipyard;

	final static List<String> researchNames = Arrays.asList(
			"Energy Technology", "Laser Technology", "Ion Technology", "Hyperspace Technology", "Plasma Technology",
			"Combustion Drive", "Impulse Drive", "Hyperspace Drive",
			"Espionage Technology", "Computer Technology", "Astrophysics", "Intergalactic Research Network", "Graviton Technology",
			"Weapons Technology", "Shielding Technology", "Armour Technology");

	HashMap<String,Integer> research;

	void dump() {
		int i = 0;
		for(String name : names) {
			logger.info(name + ": " + resources[i++]);
	    }

		logger.info("-");

	    for (String key : this.building.keySet()) {
	        logger.info(key + ":" + this.refs.get(key) + ": lvl " + this.building.get(key) + ", " + this.status.get(key));
	    }

		logger.info("-");

	    for (String key : this.shipyard.keySet()) {
	        logger.info(key + ":" + this.refs.get(key) + ": lvl " + this.shipyard.get(key) + ", " + this.status.get(key));
	    }

		logger.info("-");

	    for (String key : this.research.keySet()) {
	        logger.info(key + ":" + this.refs.get(key) + ": lvl " + this.research.get(key) + ", " + this.status.get(key));
	    }

		logger.info("---");
	}
	
	private HashMap<String, HtmlAnchor> available(HashMap<String,Integer> hash) {
		HashMap<String,HtmlAnchor> ret = new HashMap<String,HtmlAnchor>();

	    for (String key : hash.keySet()) {
	    	if (this.status.get(key).equals(key)) {
	    		HtmlAnchor link = this.links.get(this.refs.get(key));
	    		if (link != null) {
    				ret.put(key, link);
	    		}
	    	}
	    }
		return ret;
	}
	
	private void updateResources(HtmlPage page) throws ElementNotFoundException {
		try {
			NumberFormat fmt = NumberFormat.getNumberInstance(Locale.GERMANY);

			final HtmlSpan htmlMetal = page.getHtmlElementById("resources_metal");
			resources[METAL] = fmt.parse(htmlMetal.asText()).intValue();
	
			final HtmlSpan htmlCrystal = page.getHtmlElementById("resources_crystal");
			resources[CRYSTAL] =  fmt.parse(htmlCrystal.asText()).intValue();
	
			final HtmlSpan htmlDeuterium = page.getHtmlElementById("resources_deuterium");
			resources[DEUTERIUM] = fmt.parse(htmlDeuterium.asText()).intValue();
	
			final HtmlSpan htmlEnergy = page.getHtmlElementById("resources_energy");
			resources[ENERGY] = fmt.parse(htmlEnergy.asText()).intValue();
		} catch (ParseException exc) {
		}
	}

	private void update(HtmlPage page, HashMap<String,Integer> hash, List<String> names) throws ElementNotFoundException {
		List<HtmlSpan> spans = (List<HtmlSpan>) page.getByXPath("//ul//span[@class='level']");

		int i;
		
		i = 0;
		for (HtmlSpan span : spans) {
			Scanner sc = new Scanner(span.asText());
			while (sc.hasNext()) {
				if (sc.hasNextInt()) {
					int level = sc.nextInt();
					hash.put(names.get(i), new Integer(level));
				} else {
					sc.next();
				}
			}
			sc.close();
			i++;
		}

		List<HtmlAnchor> links;
		
		links = (List<HtmlAnchor>) page.getByXPath("//a[contains(@id, 'details')]");
		
		i = 0;
		for (HtmlAnchor link : links) {
			String status = link.getAttribute("title");
			String ref = link.getAttribute("ref");
			
			if (status.contains(">")) {
				String[] tokens = status.split(">");
				status = tokens[tokens.length-1];
			}
			
			this.status.put(names.get(i), status);
			this.refs.put(names.get(i), ref);
			i++;
		}

		links = (List<HtmlAnchor>) page.getByXPath("//a[contains(@class, 'fastBuild')]");
		for (HtmlAnchor link : links) {
			String onclick = link.getAttribute("onclick");

			final Pattern pattern = Pattern.compile("(.+)(type=)(\\d+)(.+)", Pattern.DOTALL);
			String ref = pattern.matcher(onclick).replaceAll("$3");

			this.links.put(ref, link);
		}
	}

	void updateResource(HtmlPage page) throws ElementNotFoundException {
		updateResources(page);
		
		update(page, this.building, resourceNames);
	}

	void updateStation(HtmlPage page) throws ElementNotFoundException {
		updateResources(page);

		update(page, this.building, stationNames);
	}
	
	void updateDefense(HtmlPage page) throws ElementNotFoundException {
		updateResources(page);

		update(page, this.shipyard, defenseNames);
	}
	
	void updateResearch(HtmlPage page) throws ElementNotFoundException {
		updateResources(page);

		update(page, this.research, researchNames);
	}

	static Integer buildingsPage(String wish) {
		Integer ret = null;
		if (resourceNames.contains(wish)) {
			ret = new Integer(Page.RESOURCES);
		} else if (stationNames.contains(wish)) {
			ret = new Integer(Page.STATION);
		}
		return ret;
	}

	static Integer shipyardsPage(String wish) {
		Integer ret = null;
		if (defenseNames.contains(wish)) {
			ret = new Integer(Page.DEFENSE);
		}
		return ret;
	}

	static Integer researchsPage(String wish) {
		Integer ret = null;
		if (researchNames.contains(wish)) {
			ret = new Integer(Page.RESEARCH);
		}
		return ret;
	}

	SimpleEntry<String,Integer> build(String wish, Integer pageno) {
		return cando(wish, pageno, this.building, "building");
	}

	SimpleEntry<String,Integer> research(String wish, Integer pageno) {
		return cando(wish, pageno, this.research, "researching");
	}

	SimpleEntry<String,Integer> ship(String wish, Integer pageno) {
		return cando(wish, pageno, this.shipyard, "shipyarding");
	}

	private SimpleEntry<String, Integer> cando(String wish, Integer pageno, HashMap<String,Integer> hash, String descr) {
		HashMap<String,HtmlAnchor> availables = available(hash);

		for (String key : availables.keySet()) {
			logger.info(key);
		}
		logger.info("---");

		for (String key : availables.keySet()) {
			if (key.equals(wish)) {
				HtmlAnchor link = availables.get(key);
				final String title = link.getAttribute("title");

				logger.info(descr + " " + key + "...");
				return new SimpleEntry<String,Integer>(title, pageno);
			}
		}
		return null;
	}

	void reset() {
		this.links.clear();
	}

	public Planet() {
		this.resources = new int[names.size()];

		this.status = new HashMap<String,String>();
		this.refs = new HashMap<String,String>();
		this.links = new HashMap<String,HtmlAnchor>();

		this.building = new HashMap<String,Integer>();
		this.shipyard = new HashMap<String,Integer>();
		this.research = new HashMap<String,Integer>();
	}
}
