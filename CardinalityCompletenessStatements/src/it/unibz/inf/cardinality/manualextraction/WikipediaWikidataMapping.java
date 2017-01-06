package it.unibz.inf.cardinality.manualextraction;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class WikipediaWikidataMapping {
	
	public String wikiArticleTitlesPath = "D:/Wikipedia/wiki_article_titles_line_num.txt";
	public String wikiArticleText = "D:/Wikipedia/wiki_articles_in_plain_text_only_contain_numbers_sample.txt";
	public TreeMap<Integer, String> articleTitles = new TreeMap<Integer, String>();
	
	public static final String PREFIXES = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
			+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX wikibase: <http://wikiba.se/ontology#>\n" + "PREFIX schema: <http://schema.org/>\n" + "\n";
	public static final String PROP_INSTANCE_OF = "wdt:P31";
	
	public static final String WD_SPARQL_ENDPOINT = "https://query.wikidata.org/bigdata/namespace/wdq/sparql";

	public static JSONObject executeSPARQLQuery(String q) throws JSONException {
		QueryExecution qExe = QueryExecutionFactory.sparqlService(WD_SPARQL_ENDPOINT, q);
		ResultSet results = qExe.execSelect();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ResultSetFormatter.output(baos, results, ResultsFormat.FMT_RS_JSON);
		JSONTokener jst = new JSONTokener(baos.toString());
		JSONObject initialRes = new JSONObject(jst);
		qExe.close();
		return initialRes;
	}
	
	public WikipediaWikidataMapping() {
		
	}
	
	public void extractArticleTitlesFromFile() throws IOException, ClassNotFoundException {
		BufferedReader br = new BufferedReader(new FileReader(wikiArticleTitlesPath));
		String line;
		while ((line = br.readLine()) != null) {
			String[] cols = line.split("\t");
			articleTitles.put(Integer.parseInt(cols[0]), cols[1]);
	    }
		br.close();
	}
	
	public String getWikidataIDFromTitle(String title) throws IOException {
		URL wiki = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&titles=" + title.replace(" ", "%20"));
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(wiki.openStream()));
		String output = in.readLine();
		String wikidata = "wikibase_item\":\"(.+?)\"";
		Pattern wikidataP = Pattern.compile(wikidata);
		Matcher m = wikidataP.matcher(output);
		if (m.find()) {
			return m.group(1);
		}
		in.close();
		
        return "null";
	}
	
	private String getInstanceOf(String wikidataID) throws JSONException {
		String sInstanceOf = PREFIXES + "SELECT * WHERE { wd:" + wikidataID + " " + PROP_INSTANCE_OF + " ?v }";
		JSONObject initialRes = executeSPARQLQuery(sInstanceOf);
		JSONArray results = initialRes.getJSONObject("results").getJSONArray("bindings");
		String resUri, resId;
		if (results.length() > 0) {
			resUri = results.getJSONObject(0).getJSONObject("v").getString("value");
			resId = resUri.substring(resUri.lastIndexOf("/")+1);
			return resId;
		}
		return "unknown";
	}
	
	public void run(String wikiArticleFilepath) throws NumberFormatException, IOException, JSONException {
		BufferedReader br = new BufferedReader(new FileReader(wikiArticleFilepath));
		String line;
		while ((line = br.readLine()) != null) {
			String num = line.substring(0, line.indexOf(": "));
			String text = line.substring(line.indexOf(": ")+2);
			
			String wikidataId = getWikidataIDFromTitle(articleTitles.floorEntry(Integer.parseInt(num)).getValue());
			String wikidataInstance = getInstanceOf(wikidataId);
			System.out.println(wikidataId + "\t" + wikidataInstance + "\t" + text);
	    }
		br.close();
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException, NumberFormatException, JSONException {
		
		WikipediaWikidataMapping map = new WikipediaWikidataMapping();
		
//		System.out.println("Load article titles and corresponding line numbers from file...");
		map.extractArticleTitlesFromFile();
//		System.out.println("Read paragraphs, map them to Wikidata Ids and Wikidata instanceOf Ids...");
		map.run(args[0]);
	
	}
}
