package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import it.unibz.inf.cardinality.statistics.SPARQLQuery;

public class WikidataNumberOfChildrenDataNew {
	
	public String wikiArticlesDirPath = "./data/wiki-articles/";
	
	public WikidataNumberOfChildrenDataNew() {
		
	}
	
	public WikidataNumberOfChildrenDataNew(String wikiArticleTitlesPath, String wikiArticlesPath, String wikiArticlesDirPath) {
		this.wikiArticlesDirPath = wikiArticlesDirPath;
	}

	public static void main(String[] args) throws Exception {
		WikidataNumberOfChildrenDataNew numOfChildren = new WikidataNumberOfChildrenDataNew();
		
		numOfChildren.matchNumberOfChildrenTrain(numOfChildren.wikiArticlesDirPath);
	}
	
	public String getWikipediaTextFromTitle(String title) throws IOException, JSONException {
		URL wiki = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&explaintext&redirects=true&titles=" + URLEncoder.encode(title, "UTF-8"));
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(wiki.openStream()));
		String output = in.readLine();
		JSONObject pages = new JSONObject(output).getJSONObject("query").getJSONObject("pages");
		Iterator key = pages.keys();
		
		JSONObject page = pages.getJSONObject(key.next().toString());
		if (page.has("extract")) {
			return page.getString("extract");
		}
		return "";
	}
	
	private Boolean containNumber(String str) {
		String hasNumRegex = "\\b(\\d+)\\b";
		Pattern hasNum = Pattern.compile(hasNumRegex);
		String hasNumStrRegex = "\\b((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety))\\b";
		Pattern hasNumStr = Pattern.compile(hasNumStrRegex);
		
		Matcher mNum = hasNum.matcher(str);
		Matcher mNumStr = hasNumStr.matcher(str);
        if (mNum.find() || mNumStr.find()) {
        	return true;
        }
        
        return false;
		
	}
	
	private List<String> filterText(String articleText) throws IOException {
		List<String> filtered = new ArrayList<String>();
		
		for (String line : articleText.split("\\r?\\n")) {
			if (containNumber(line)) {
				Document doc = new Document(line);
				
				for (Sentence sent : doc.sentences()) {
					if (containNumber(sent.text())) {
						boolean numberFound = false;
						for (int i=0; i<sent.words().size(); i++) {
//							System.out.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
							if (sent.posTag(i).equals("CD")
									&& !sent.nerTag(i).equals("MONEY")
									&& !sent.nerTag(i).equals("PERCENT")
									&& !sent.nerTag(i).equals("DATE")
									&& !sent.nerTag(i).equals("TIME")
									&& !sent.nerTag(i).equals("DURATION")
									&& !sent.nerTag(i).equals("SET")) {
								numberFound = true;
							}
						}
						if (numberFound) {
//							System.out.println(sent.text());
							filtered.add(sent.text());
						}
					}
		        }
				
				
			}
	    }
		return filtered;
	}
	
	public void matchNumberOfChildrenTrain(String articlePath) throws JSONException, IOException, InterruptedException {
		//Get people with number of chidren property
		String q = "SELECT ?parent ?name ?numOfChildren ?countChildren WHERE { "
		 + "{ "
		 + "  ?parent wdt:P1971 ?numOfChildren . "
		 + "  ?parent wdt:P31 wd:Q5 . "
		 + "  OPTIONAL { "
		 + "    ?parent rdfs:label ?name filter (lang(?name) = \"en\") . "
		 + "  } "
		 + "  { " 
		 + "    SELECT ?parent (COUNT(?child) AS ?countChildren) WHERE { "
		 + "      ?parent wdt:P40 ?child . "
		 + "    } GROUP BY ?parent "
		 + "  } "
		 + "} "
		 + "UNION "
		 + "{ " 
		 + "  { " 
		 + "    ?parent wdt:P1971 ?numOfChildren . " 
		 + "    ?parent wdt:P31 wd:Q5 . "
		 + "    OPTIONAL { "
		 + "      ?parent rdfs:label ?name filter (lang(?name) = \"en\") . "
		 + "    } "
		 + "  } " 
		 + "  MINUS " 
		 + "  { "
		 + "    SELECT ?parent WHERE { "
		 + "      ?parent wdt:P40 ?child . "
		 + "    } GROUP BY ?parent "
		 + "  } " 
		 + "} "
		 + "}";
		
		JSONObject initialRes = SPARQLQuery.execute(q);
		JSONArray res = initialRes.getJSONObject ("results").getJSONArray("bindings");
		
		String entity = "", eid = "", name = "", start = "", end = "";
		int numChild, countChild, match = 0;
		JSONArray result = new JSONArray();
		for (int i=0; i<res.length(); i++) {
			if (res.getJSONObject(i).has("name")) {
				entity = res.getJSONObject(i).getJSONObject("parent").getString("value");
				eid = entity.replace("http://www.wikidata.org/entity/", "");
				name = res.getJSONObject(i).getJSONObject("name").getString("value");
				numChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("numOfChildren").getString("value"));
				if (res.getJSONObject(i).isNull("countChildren")) {
					countChild = 0;
				} else {
					countChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("countChildren").getString("value"));
				}
				
				if (
						numChild > 0
						&& numChild > countChild) {
					
					String wikipediaText = getWikipediaTextFromTitle(name);
					if (wikipediaText != "") {
						List<String> articleText = filterText(wikipediaText);
						
						if (articleText.size() > 0) {
							System.out.println(eid + "\t" + name + "\t" + numChild + "\t" + countChild + "\t" + StringUtils.join(articleText, "|"));
							
							JSONObject obj = new JSONObject();
							obj.put("wikidata-id", eid);
							obj.put("wikidata-label", name);
							obj.put("num-child", numChild);
		
							JSONArray list = new JSONArray();
							for (String s : articleText) {
								list.put(s);
							}
							obj.put("article-num-only", list);
							result.put(obj);
							
							match ++;
						}
					}
				}
			}
		}
		try {

			FileWriter file = new FileWriter("train-cardinality.json");
			file.write(result.toString(4));
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(match);
	}
}
