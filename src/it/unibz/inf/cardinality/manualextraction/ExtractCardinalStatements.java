package it.unibz.inf.cardinality.manualextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class ExtractCardinalStatements {
	
	private String wikiTitlesPath = "D:/Wikipedia/wiki_article_titles.txt";
	private String wikiArticlesPath = "D:/Wikipedia/wiki_articles_in_plain_text.txt";
	
//	private String wikiArticleTitlesPath = "D:/Wikipedia/wiki_article_titles_line_num.ser";
	private String wikiArticleTitlesPath = "D:/Wikipedia/wiki_article_titles_line_num.txt";
	private TreeMap<Integer, String> articleTitles = new TreeMap<Integer, String>();
	
	private String cardinalStatementsPath = "data/cardinal_statements.txt";
	
	private HashMap<String, String> filePatterns = new HashMap<String, String>();
	private HashMap<String, WikiArticle> wikiArticles = new HashMap<String, WikiArticle>();
	private HashSet<String> lines = new HashSet<String>();
	private HashSet<Integer> filteredOut = new HashSet<Integer>();
	private String cardinalPath = "data/cardinal_statements.txt";
	private int err = 0;
	
	private HashMap<String, Integer> numbers = new HashMap<String, Integer>();
	
	private static final String PREFIXES = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
			+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX wikibase: <http://wikiba.se/ontology#>\n" + "PREFIX schema: <http://schema.org/>\n" + "\n";
	private static final String PROP_INSTANCE_OF = "wdt:P31";
	private static final String OBJ_HUMAN = "wd:Q5";
	
	public static final String WD_SPARQL_ENDPOINT = "https://query.wikidata.org/bigdata/namespace/wdq/sparql";

	private static JSONObject executeSPARQLQuery(String q) throws JSONException {
		QueryExecution qExe = QueryExecutionFactory.sparqlService(WD_SPARQL_ENDPOINT, q);
		ResultSet results = qExe.execSelect();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ResultSetFormatter.output(baos, results, ResultsFormat.FMT_RS_JSON);
		JSONTokener jst = new JSONTokener(baos.toString());
		JSONObject initialRes = new JSONObject(jst);
		qExe.close();
		return initialRes;
	}
	
	public ExtractCardinalStatements() {
		
		filePatterns.put("data/has_num_child_cardinal_statements.txt", "have-child");
		filePatterns.put("data/has_child_cardinal_statements.txt", "have-child");
		filePatterns.put("data/has_num_daughter_cardinal_statements.txt", "have-daughter");
		filePatterns.put("data/has_daughter_cardinal_statements.txt", "have-daughter");
		filePatterns.put("data/has_num_son_cardinal_statements.txt", "have-son");
		filePatterns.put("data/has_son_cardinal_statements.txt", "have-son");
		filePatterns.put("data/has_num_daughter_son_cardinal_statements.txt", "have-daughter-son");
		filePatterns.put("data/has_daughter_son_cardinal_statements.txt", "have-daughter-son");
		filePatterns.put("data/has_num_son_daughter_cardinal_statements.txt", "have-son-daughter");
		filePatterns.put("data/has_son_daughter_cardinal_statements.txt", "have-son-daughter");
		
		filePatterns.put("data/parent_of_num_child_cardinal_statements.txt", "parent-of-child");
		filePatterns.put("data/parent_of_child_cardinal_statements.txt", "parent-of-child");
		filePatterns.put("data/parent_of_num_daughter_cardinal_statements.txt", "parent-of-daughter");
		filePatterns.put("data/parent_of_daughter_cardinal_statements.txt", "parent-of-daughter");
		filePatterns.put("data/parent_of_num_son_cardinal_statements.txt", "parent-of-son");
		filePatterns.put("data/parent_of_son_cardinal_statements.txt", "parent-of-son");
		filePatterns.put("data/parent_of_num_daughter_son_cardinal_statements.txt", "parent-of-daughter-son");
		filePatterns.put("data/parent_of_daughter_son_cardinal_statements.txt", "parent-of-daughter-son");
		filePatterns.put("data/parent_of_num_son_daughter_cardinal_statements.txt", "parent-of-son-daughter");
		filePatterns.put("data/parent_of_son_daughter_cardinal_statements.txt", "parent-of-son-daughter");
		
		filePatterns.put("data/father_of_num_child_cardinal_statements.txt", "father-of-child");
		filePatterns.put("data/father_of_child_cardinal_statements.txt", "father-of-child");
		filePatterns.put("data/father_of_num_daughter_cardinal_statements.txt", "father-of-daughter");
		filePatterns.put("data/father_of_daughter_cardinal_statements.txt", "father-of-daughter");
		filePatterns.put("data/father_of_num_son_cardinal_statements.txt", "father-of-son");
		filePatterns.put("data/father_of_son_cardinal_statements.txt", "father-of-son");
		filePatterns.put("data/father_of_num_daughter_son_cardinal_statements.txt", "father-of-daughter-son");
		filePatterns.put("data/father_of_daughter_son_cardinal_statements.txt", "father-of-daughter-son");
		filePatterns.put("data/father_of_num_son_daughter_cardinal_statements.txt", "father-of-son-daughter");
		filePatterns.put("data/father_of_son_daughter_cardinal_statements.txt", "father-of-son-daughter");
		
		filePatterns.put("data/mother_of_num_child_cardinal_statements.txt", "mother-of-child");
		filePatterns.put("data/mother_of_child_cardinal_statements.txt", "mother-of-child");
		filePatterns.put("data/mother_of_num_daughter_cardinal_statements.txt", "mother-of-daughter");
		filePatterns.put("data/mother_of_daughter_cardinal_statements.txt", "mother-of-daughter");
		filePatterns.put("data/mother_of_num_son_cardinal_statements.txt", "mother-of-son");
		filePatterns.put("data/mother_of_son_cardinal_statements.txt", "mother-of-son");
		filePatterns.put("data/mother_of_num_daughter_son_cardinal_statements.txt", "mother-of-daughter-son");
		filePatterns.put("data/mother_of_daughter_son_cardinal_statements.txt", "mother-of-daughter-son");
		filePatterns.put("data/mother_of_num_son_daughter_cardinal_statements.txt", "mother-of-son-daughter");
		filePatterns.put("data/mother_of_son_daughter_cardinal_statements.txt", "mother-of-son-daughter");
		
		numbers.put("no", 0);
		numbers.put("any", 0);
		numbers.put("a", 1);
		numbers.put("an", 1);
		numbers.put("one", 1);
		numbers.put("two", 2);
		numbers.put("three", 3);
		numbers.put("four", 4);
		numbers.put("five", 5);
		numbers.put("six", 6);
		numbers.put("seven", 7);
		numbers.put("eight", 8);
		numbers.put("nine", 9);
		numbers.put("ten", 10);
		numbers.put("eleven", 11);
		numbers.put("twelve", 12);
		numbers.put("thirteen", 13);
		numbers.put("fourteen", 14);
		numbers.put("fifteen", 15);
		numbers.put("sixteen", 16);
		numbers.put("seventeen", 17);
		numbers.put("eighteen", 18);
		numbers.put("nineteen", 19);
		numbers.put("twenty", 20);
		numbers.put("thirty", 30);
		numbers.put("forty", 40);
		numbers.put("fifty", 50);
		numbers.put("sixty", 60);
		numbers.put("seventy", 70);
		numbers.put("eighty", 80);
		numbers.put("ninety", 90);
	}
	
	public String getWikidataIDFromTitle(String title) throws IOException, JSONException {
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
		
//		try (InputStream is = wiki.openStream();
//				JsonReader rdr = Json.createReader(is)) {
//		
//			JsonObject obj = rdr.readObject();
//			JsonObject pages = obj.getJsonObject("query").getJsonObject("pages");
//			Set<String> pageKeys = pages.keySet();
//			
//			JsonObject page = null;
//			for(String key : pageKeys){
//				page = (JsonObject) pages.getJsonObject(key).get("pageprops");
////	            System.out.println(title + " : " + page.getString("wikibase_item"));
//	            if (page != null)
//	            	return page.getString("wikibase_item");
//	        }
//		}
		
        return "";
	}
	
	public void writeArticleTitlesFromWikiArticles() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(wikiArticlesPath));
		BufferedWriter bw = new BufferedWriter(new FileWriter(wikiArticleTitlesPath));
		
		String line;
		Integer lineNum = 0;
		while ((line = br.readLine()) != null) {
			if (line.contains("#Article: ")) {
				String title = line.replace("#Article: ", "").trim();
				if (!title.isEmpty()) {
					bw.write(lineNum + "\t" + title + "\n");					
				}
	    	}
	    	lineNum ++;
	    }
		br.close();
		bw.close();
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
	
	private boolean isInteger(String s) {  
	    return s.matches("\\d+");  
	}  
	
	private Integer toInteger(String n) {
		String num = n.trim();
		if (isInteger(n)) return Integer.parseInt(num);
		else {
			int nn = 0;
//			System.out.println(num);
			if (num.contains(" ")) {
				for (String s : num.split(" ")) {
					nn += numbers.get(s.trim());
				}
			} else if (num.contains("-")) {
				for (String s : num.split("-")) {
					nn += numbers.get(s.trim());
				}
			} else {
				nn += numbers.get(num);
			}
			return nn;
		}
	}
	
	public void writeHasChild(String path, String pattern, Pattern p) throws IOException {
		BufferedReader br;
		String line;
		
		br = new BufferedReader(new FileReader(path));
		boolean found = false;
		while ((line = br.readLine()) != null) {
			Integer numLine = Integer.parseInt(line.substring(0, line.indexOf(':')));
			String paragraph = line.substring(line.indexOf(':')+1);
			
			Document doc = new Document(paragraph);
			Sentence sent;
			found = false;
			String title, wikidataID;
			Integer numChild = 0;
	        for (int i=0; i<doc.sentences().size(); i++) {
	        	sent = doc.sentences().get(i);
	            Matcher m = p.matcher(sent.toString());
	            if (m.find()) {
	            	if (!lines.contains(numLine + "-" + i)) {
	            		title = articleTitles.floorEntry(numLine).getValue();
	            		
	        			if (!wikiArticles.containsKey(title)) {
	        				wikiArticles.put(title, new WikiArticle());
	        			}
	        			wikiArticles.get(title).setTitle(title);
	        			
	        			if (m.groupCount() == 1 || m.groupCount() == 3) {
		            		numChild = toInteger(m.group(1));
	        			} else if (m.groupCount() == 2) {
	        				numChild = toInteger(m.group(1)) + toInteger(m.group(2));
	        			} else if (m.groupCount() == 6 || m.groupCount() == 4) {
	        				numChild = toInteger(m.group(1)) + toInteger(m.group(4));
	        			}
	        			wikiArticles.get(title).getSentences().add(pattern + "#" + numChild + "#" + sent.toString());
	            		wikiArticles.get(title).setNumChild(wikiArticles.get(title).getNumChild() + numChild);
	        			
	        			lines.add(numLine + "-" + i);
	            	}
	            	found = true;
	            } 
	        }
	        if (!found) {
	        	if (!filteredOut.contains(numLine)) {
//	        		System.err.println(found + "\t" + numLine + "\t" + pattern + "\t" + paragraph);
	        		filteredOut.add(numLine);
	        		err ++;
	        	}
	        }
	    }
		br.close();
	}
	
	public void mergeCardinalStatements() throws IOException, JSONException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(cardinalPath),"UTF-16"));
		PrintWriter writer = new PrintWriter(bw);
		
		String hasNumChildPath = "data/has_num_child_cardinal_statements.txt";
		String hasChildPath = "data/has_child_cardinal_statements.txt";
		
		String hasNumDaughterSonPath = "data/has_num_daughter_son_cardinal_statements.txt";
		String hasDaughterSonPath = "data/has_daughter_son_cardinal_statements.txt";
		
		String hasNumSonDaughterPath = "data/has_num_son_daughter_cardinal_statements.txt";
		String hasSonDaughterPath = "data/has_son_daughter_cardinal_statements.txt";
		
		String hasNumDaughterPath = "data/has_num_daughter_cardinal_statements.txt";
		String hasDaughterPath = "data/has_daughter_cardinal_statements.txt";
		
		String hasNumSonPath = "data/has_num_son_cardinal_statements.txt";
		String hasSonPath = "data/has_son_cardinal_statements.txt";
		String hasNumChildRegex = "\\bha[sdv]e* (\\d+) (?:\\w+ )*child[ren]{0,3}\\b";
		Pattern hasNumChildP = Pattern.compile(hasNumChildRegex);
		String hasChildRegex = "\\bha[sdv]e* ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}child[ren]{0,3}\\b";
		Pattern hasChildP = Pattern.compile(hasChildRegex);
		
		String hasNoChildRegex = "\\bha[sdv]e* (any|no|zero) child[ren]{0,3}\\b";
		Pattern hasNoChildP = Pattern.compile(hasNoChildRegex);
		
		String hasNumDaughterNumSonRegex = "\\bha[sdv]e* (\\d+) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}sons*\\b";
		Pattern hasNumDaughterNumSonP = Pattern.compile(hasNumDaughterNumSonRegex);
		String hasNumDaughterSonRegex = "\\bha[sdv]e* (\\d+) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*\\b";
		Pattern hasNumDaughterSonP = Pattern.compile(hasNumDaughterSonRegex);
		
		String hasDaughterNumSonRegex = "\\bha[sdv]e* ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}sons*\\b";
		Pattern hasDaughterNumSonP = Pattern.compile(hasDaughterNumSonRegex);
        String hasDaughterSonRegex = "\\bha[sdv]e* ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*\\b";
		Pattern hasDaughterSonP = Pattern.compile(hasDaughterSonRegex);
        
        String hasNumSonNumDaughterRegex = "\\bha[sdv]e* (\\d+) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}daughters*\\b";
		Pattern hasNumSonNumDaughterP = Pattern.compile(hasNumSonNumDaughterRegex);
		String hasNumSonDaughterRegex = "\\bha[sdv]e* (\\d+) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*\\b";
		Pattern hasNumSonDaughterP = Pattern.compile(hasNumSonDaughterRegex);
		
		String hasSonNumDaughterRegex = "\\bha[sdv]e* ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}daughters*\\b";
		Pattern hasSonNumDaughterP = Pattern.compile(hasSonNumDaughterRegex);
        String hasSonDaughterRegex = "\\bha[sdv]e* ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*\\b";
		Pattern hasSonDaughterP = Pattern.compile(hasSonDaughterRegex);
		
		String hasNumDaughterRegex = "\\bha[sdv]e* (\\d+) (?:\\w+ ){0,2}daughters*\\b";
		Pattern hasNumDaughterP = Pattern.compile(hasNumDaughterRegex);
		String hasDaughterRegex = "\\bha[sdv]e* ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*\\b";
		Pattern hasDaughterP = Pattern.compile(hasDaughterRegex);
		
		String hasNumSonRegex = "\\bha[sdv]e* (\\d+) (?:\\w+ ){0,2}sons*\\b";
		Pattern hasNumSonP = Pattern.compile(hasNumSonRegex);
		String hasSonRegex = "\\bha[sdv]e* ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*\\b";
		Pattern hasSonP = Pattern.compile(hasSonRegex);
		
		System.out.println("   has-child pattern...");
		writeHasChild(hasNumChildPath, filePatterns.get(hasNumChildPath), hasNumChildP);
		writeHasChild(hasChildPath, filePatterns.get(hasChildPath), hasChildP);
		writeHasChild(hasChildPath, "has-no-child", hasNoChildP);
		
		System.out.println("   has-daughter-son pattern...");
		writeHasChild(hasNumDaughterSonPath, filePatterns.get(hasNumDaughterSonPath), hasNumDaughterNumSonP);
		writeHasChild(hasNumDaughterSonPath, filePatterns.get(hasNumDaughterSonPath), hasNumDaughterSonP);
		writeHasChild(hasDaughterSonPath, filePatterns.get(hasDaughterSonPath), hasDaughterNumSonP);
		writeHasChild(hasDaughterSonPath, filePatterns.get(hasDaughterSonPath), hasDaughterSonP);
		
		writeHasChild(hasNumDaughterPath, filePatterns.get(hasNumDaughterSonPath), hasNumDaughterNumSonP);
		writeHasChild(hasNumDaughterPath, filePatterns.get(hasNumDaughterSonPath), hasNumDaughterSonP);
		writeHasChild(hasDaughterPath, filePatterns.get(hasDaughterSonPath), hasDaughterNumSonP);
		writeHasChild(hasDaughterPath, filePatterns.get(hasDaughterSonPath), hasDaughterSonP);
		
		System.out.println("   has-son-daughter pattern...");
		writeHasChild(hasNumSonDaughterPath, filePatterns.get(hasNumSonDaughterPath), hasNumSonNumDaughterP);
		writeHasChild(hasNumSonDaughterPath, filePatterns.get(hasNumSonDaughterPath), hasNumSonDaughterP);
		writeHasChild(hasSonDaughterPath, filePatterns.get(hasSonDaughterPath), hasSonNumDaughterP);
		writeHasChild(hasSonDaughterPath, filePatterns.get(hasSonDaughterPath), hasSonDaughterP);
		
		writeHasChild(hasNumSonPath, filePatterns.get(hasNumSonDaughterPath), hasNumSonNumDaughterP);
		writeHasChild(hasNumSonPath, filePatterns.get(hasNumSonDaughterPath), hasNumSonDaughterP);
		writeHasChild(hasSonPath, filePatterns.get(hasSonDaughterPath), hasSonNumDaughterP);
		writeHasChild(hasSonPath, filePatterns.get(hasSonDaughterPath), hasSonDaughterP);
		
		System.out.println("   has-daughter pattern...");
		writeHasChild(hasNumDaughterPath, filePatterns.get(hasNumDaughterPath), hasNumDaughterP);
		writeHasChild(hasDaughterPath, filePatterns.get(hasDaughterPath), hasDaughterP);
		
		System.out.println("   has-son pattern...");
		writeHasChild(hasNumSonPath, filePatterns.get(hasNumSonPath), hasNumSonP);
		writeHasChild(hasSonPath, filePatterns.get(hasSonPath), hasSonP);
		
		String[] parent = {"parent", "father", "mother"};
		
		for (String p : parent) {
			
			String parentOfNumChildPath = "data/" + p + "_of_num_child_cardinal_statements.txt";
			String parentOfChildPath = "data/" + p + "_of_child_cardinal_statements.txt";
			
			String parentOfNumDaughterSonPath = "data/" + p + "_of_num_daughter_son_cardinal_statements.txt";
			String parentOfDaughterSonPath = "data/" + p + "_of_daughter_son_cardinal_statements.txt";
			
			String parentOfNumSonDaughterPath = "data/" + p + "_of_num_son_daughter_cardinal_statements.txt";
			String parentOfSonDaughterPath = "data/" + p + "_of_son_daughter_cardinal_statements.txt";
			
			String parentOfNumDaughterPath = "data/" + p + "_of_num_daughter_cardinal_statements.txt";
			String parentOfDaughterPath = "data/" + p + "_of_daughter_cardinal_statements.txt";
			
			String parentOfNumSonPath = "data/" + p + "_of_num_son_cardinal_statements.txt";
			String parentOfSonPath = "data/" + p + "_of_son_cardinal_statements.txt";
			
			String parentOfNumChildRegex = "\\bw*[aei][rs]e* the " + p + "s* of (\\d+) (?:\\w+ )*child[ren]{0,3}\\b";
			Pattern parentOfNumChildP = Pattern.compile(parentOfNumChildRegex);
			String parentOfChildRegex = "\\bw*[aei][rs]e* the " + p + "s* of ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}child[ren]{0,3}\\b";
			Pattern parentOfChildP = Pattern.compile(parentOfChildRegex);
			
			String parentOfNoChildRegex = "\\bw*[aei][rs]e* the " + p + "s* of (any|no|zero) child[ren]{0,3}\\b";
			Pattern parentOfNoChildP = Pattern.compile(parentOfNoChildRegex);
			
			String parentOfNumDaughterNumSonRegex = "\\bw*[aei][rs]e* the " + p + "s* of (\\d+) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}sons*\\b";
			Pattern parentOfNumDaughterNumSonP = Pattern.compile(parentOfNumDaughterNumSonRegex);
			String parentOfNumDaughterSonRegex = "\\bw*[aei][rs]e* the " + p + "s* of (\\d+) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*\\b";
			Pattern parentOfNumDaughterSonP = Pattern.compile(parentOfNumDaughterSonRegex);
            
            String parentOfDaughterNumSonRegex = "\\bw*[aei][rs]e* the " + p + "s* of ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}sons*\\b";
			Pattern parentOfDaughterNumSonP = Pattern.compile(parentOfDaughterNumSonRegex);
			String parentOfDaughterSonRegex = "\\bw*[aei][rs]e* the " + p + "s* of ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*\\b";
			Pattern parentOfDaughterSonP = Pattern.compile(parentOfDaughterSonRegex);
			
			String parentOfNumSonNumDaughterRegex = "\\bw*[aei][rs]e* the " + p + "s* of (\\d+) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}daughters*\\b";
			Pattern parentOfNumSonNumDaughterP = Pattern.compile(parentOfNumSonNumDaughterRegex);
			String parentOfNumSonDaughterRegex = "\\bw*[aei][rs]e* the " + p + "s* of (\\d+) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*\\b";
			Pattern parentOfNumSonDaughterP = Pattern.compile(parentOfNumSonDaughterRegex);
            
            String parentOfSonNumDaughterRegex = "\\bw*[aei][rs]e* the " + p + "s* of ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )(\\d+) (?:\\w+ ){0,2}daughters*\\b";
			Pattern parentOfSonNumDaughterP = Pattern.compile(parentOfSonNumDaughterRegex);
			String parentOfSonDaughterRegex = "\\bw*[aei][rs]e* the " + p + "s* of ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*(?:.* )and(?:.* )((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*\\b";
			Pattern parentOfSonDaughterP = Pattern.compile(parentOfSonDaughterRegex);
			
			String parentOfNumDaughterRegex = "\\bw*[aei][rs]e* the " + p + "s* of (\\d+) (?:\\w+ ){0,2}daughters*\\b";
			Pattern parentOfNumDaughterP = Pattern.compile(parentOfNumDaughterRegex);
			String parentOfDaughterRegex = "\\bw*[aei][rs]e* the " + p + "s* of ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}daughters*\\b";
			Pattern parentOfDaughterP = Pattern.compile(parentOfDaughterRegex);
			
			String parentOfNumSonRegex = "\\bw*[aei][rs]e* the " + p + "s* of (\\d+) (?:\\w+ ){0,2}sons*\\b";
			Pattern parentOfNumSonP = Pattern.compile(parentOfNumSonRegex);
			String parentOfSonRegex = "\\bw*[aei][rs]e* the " + p + "s* of ((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)) (?:\\w+ ){0,2}sons*\\b";
			Pattern parentOfSonP = Pattern.compile(parentOfSonRegex);
			
			System.out.println("   "+p+"-of-child pattern...");
			writeHasChild(parentOfNumChildPath, filePatterns.get(parentOfNumChildPath), parentOfNumChildP);
			writeHasChild(parentOfChildPath, filePatterns.get(parentOfChildPath), parentOfChildP);
			writeHasChild(parentOfChildPath, "parent-of-no-child", parentOfNoChildP);
			
			System.out.println("   "+p+"-of-daughter-son pattern...");
			writeHasChild(parentOfNumDaughterSonPath, filePatterns.get(parentOfNumDaughterSonPath), parentOfNumDaughterNumSonP);
			writeHasChild(parentOfNumDaughterSonPath, filePatterns.get(parentOfNumDaughterSonPath), parentOfNumDaughterSonP);
			writeHasChild(parentOfDaughterSonPath, filePatterns.get(parentOfDaughterSonPath), parentOfDaughterNumSonP);
			writeHasChild(parentOfDaughterSonPath, filePatterns.get(parentOfDaughterSonPath), parentOfDaughterSonP);
			
			writeHasChild(parentOfNumDaughterPath, filePatterns.get(parentOfNumDaughterSonPath), parentOfNumDaughterNumSonP);
			writeHasChild(parentOfNumDaughterPath, filePatterns.get(parentOfNumDaughterSonPath), parentOfNumDaughterSonP);
			writeHasChild(parentOfDaughterPath, filePatterns.get(parentOfDaughterSonPath), parentOfDaughterNumSonP);
			writeHasChild(parentOfDaughterPath, filePatterns.get(parentOfDaughterSonPath), parentOfDaughterSonP);
			
			System.out.println("   "+p+"-of-son-daughter pattern...");
			writeHasChild(parentOfNumSonDaughterPath, filePatterns.get(parentOfNumSonDaughterPath), parentOfNumSonNumDaughterP);
			writeHasChild(parentOfNumSonDaughterPath, filePatterns.get(parentOfNumSonDaughterPath), parentOfNumSonDaughterP);
			writeHasChild(parentOfSonDaughterPath, filePatterns.get(parentOfSonDaughterPath), parentOfSonNumDaughterP);
			writeHasChild(parentOfSonDaughterPath, filePatterns.get(parentOfSonDaughterPath), parentOfSonDaughterP);
			
			writeHasChild(parentOfNumSonPath, filePatterns.get(parentOfNumSonDaughterPath), parentOfNumSonNumDaughterP);
			writeHasChild(parentOfNumSonPath, filePatterns.get(parentOfNumSonDaughterPath), parentOfNumSonDaughterP);
			writeHasChild(parentOfSonPath, filePatterns.get(parentOfSonDaughterPath), parentOfSonNumDaughterP);
			writeHasChild(parentOfSonPath, filePatterns.get(parentOfSonDaughterPath), parentOfSonDaughterP);
			
			System.out.println("   "+p+"-of-daughter pattern...");
			writeHasChild(parentOfNumDaughterPath, filePatterns.get(parentOfNumDaughterPath), parentOfNumDaughterP);
			writeHasChild(parentOfDaughterPath, filePatterns.get(parentOfDaughterPath), parentOfDaughterP);
			
			System.out.println("   "+p+"-of-son pattern...");
			writeHasChild(parentOfNumSonPath, filePatterns.get(parentOfNumSonPath), parentOfNumSonP);
			writeHasChild(parentOfSonPath, filePatterns.get(parentOfSonPath), parentOfSonP);
			
		}
		
		System.out.println("Mapping title to Wikidata ID...");
		System.out.println("Total articles: " + wikiArticles.keySet().size());
		int i = 0;
		for (String key : wikiArticles.keySet()) {
//			wikiArticles.get(key).setWikidataID(getWikidataIDFromTitle(key));
			wikiArticles.get(key).setWikidataID("0");
			wikiArticles.get(key).setLength(0);
			writer.println(wikiArticles.get(key).toString());
			if (i % 100 == 0) System.out.print(i + " ");
			i ++;
		}
		
		bw.close();
		writer.close();
	}
	
	private static String instanceOf(String wikidataID) throws JSONException {
		String instanceOf = "non-human";
		String sInstanceOf = PREFIXES + "SELECT * WHERE { wd:" + wikidataID + " " + PROP_INSTANCE_OF + " ?v }";
		JSONObject initialRes = executeSPARQLQuery(sInstanceOf);
		JSONArray results = initialRes.getJSONObject("results").getJSONArray("bindings");
		String resUri, resId;
		if (results.length() > 0) {
			resUri = results.getJSONObject(0).getJSONObject("v").getString("value");
			resId = resUri.substring(resUri.lastIndexOf("/")+1);
			if (resId.equals("Q5")) {
				instanceOf = "human";
			}
		} else {
			instanceOf = "unknown";
		}
		return instanceOf;
	}
	
	public void extractCardinalStatements() throws IOException, JSONException {
		BufferedReader br = new BufferedReader(new InputStreamReader (
				new FileInputStream(cardinalStatementsPath), "UTF-16"));
		
		List<WikiArticle> statements = new ArrayList<WikiArticle>();		
		String line;
		while ((line = br.readLine()) != null) {
			WikiArticle wa = new WikiArticle(line);
			statements.add(wa);
	    }
		br.close();
		
		// Get 50 WikiArticle in a random order
		WikidataMapping map = new WikidataMapping();
		Collections.shuffle(statements);
		for (int i=0; i<50; i++) {
		    WikiArticle wk = statements.get(i);
		    wk.setWikidataID(map.getWikidataIDFromTitle(wk.getTitle()));
			wk.setLength(map.getArticleLengthFromTitle(wk.getTitle()));
			System.out.println(wk.toString() + "\t" + instanceOf(wk.getWikidataID()));
		}
	}
	
	private void splitCardinalStatements() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader (
				new FileInputStream(cardinalStatementsPath), "UTF-16"));
		
		String line; Integer numLine = 0, numFile = 0;
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(cardinalStatementsPath.replace(".txt", "") + "_0.txt"), "UTF-16"));
		PrintWriter writer = new PrintWriter(bw);
		while ((line = br.readLine()) != null) {
			if (numLine == 10000) {
				bw.close();
				numFile ++;
				bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(cardinalStatementsPath.replace(".txt", "") + "_" + (numFile) + ".txt"), "UTF-16"));
				writer = new PrintWriter(bw);
				numLine = 0;
			}
			writer.println(line.trim());
			numLine ++;
	    }
		br.close();
		bw.close();
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, JSONException {
		
//		PrintStream out = new PrintStream(new FileOutputStream("cardinality_output.txt"));
//		System.setOut(out);
//		PrintStream log = new PrintStream(new FileOutputStream("cardinality_log.txt"));
//		System.setErr(log);
		
		ExtractCardinalStatements extract = new ExtractCardinalStatements();
		
//		System.out.println("Save article titles and corresponding line numbers into file...");
//		extract.writeArticleTitlesFromWikiArticles();
		
		System.out.println("Load article titles and corresponding line numbers from file...");
		extract.extractArticleTitlesFromFile();
		//System.out.println(extract.articleTitles.floorEntry(491102).getValue());
		
		System.out.println("Pattern extraction, filter and merge cardinality statements...");
		extract.mergeCardinalStatements();
		System.out.println(extract.err);
		
		System.out.println("Wikidata mapping... run WikidataMapping.jar independently for each file split, then merge.");		
		extract.splitCardinalStatements();
		
//		System.out.println("Randomize 50 Wikipedia article and Wikidata mapping...");
//		extract.extractCardinalStatements();
	}
	
}
