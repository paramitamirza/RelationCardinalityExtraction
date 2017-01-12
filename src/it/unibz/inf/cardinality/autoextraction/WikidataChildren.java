package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class WikidataChildren {
	
	public static final String PREFIXES = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
			+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX wikibase: <http://wikiba.se/ontology#>\n" 
			+ "PREFIX schema: <http://schema.org/>\n" 
			+ "PREFIX bd: <http://www.bigdata.com/rdf#>\n"
			+ "\n";
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
	
	public JSONArray readJSONArray(String filepath) throws IOException, JSONException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String everything;
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    everything = sb.toString();
		} finally {
		    br.close();
		}
		return new JSONArray(everything);
	}
	
	public String getInteger(String numStr) {
		String[] digitsArr = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
				"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
		String[] tensArr = {"", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
		List<String> digits = Arrays.asList(digitsArr);
		List<String> tens = Arrays.asList(tensArr);
		Map<String, Integer> hundreds = new HashMap<String, Integer>();
		hundreds.put("hundred", 100);
		hundreds.put("thousand", 1000);
		hundreds.put("million", 1000000);
		
		long number = -999; 
		if (tens.contains(numStr)) number = tens.indexOf(numStr) * 10;
		else if (digits.contains(numStr)) number = digits.indexOf(numStr);
		else if (NumberUtils.isNumber(numStr)) number = new Float(Float.parseFloat(numStr)).longValue();
		else if (hundreds.containsKey(numStr)) number = hundreds.get(numStr);
		
		if (number != -999) {
			return number+"";
		} else {
			return numStr;
		}
	}
	
	public String getIntegerSimple(String numStr) {
		String[] digitsArr = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
				"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
		String[] tensArr = {"", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
		List<String> digits = Arrays.asList(digitsArr);
		List<String> tens = Arrays.asList(tensArr);
		Map<String, Integer> hundreds = new HashMap<String, Integer>();
		hundreds.put("hundred", 100);
		hundreds.put("thousand", 1000);
		hundreds.put("million", 1000000);
		
		if (tens.contains(numStr)) return "ten";
		else if (digits.contains(numStr)) return "num";
		else if (NumberUtils.isNumber(numStr)) return "num";
		else if (hundreds.containsKey(numStr)) return "hundred";
		
		return numStr;
	}
	
	public boolean matchNumbers(String numStr, int num) {
		String[] digitsArr = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
				"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
		String[] tensArr = {"ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
		List<String> digits = Arrays.asList(digitsArr);
		List<String> tens = Arrays.asList(tensArr);
		Map<String, Integer> hundreds = new HashMap<String, Integer>();
		hundreds.put("hundred", 100);
		hundreds.put("thousand", 1000);
		hundreds.put("million", 1000000);
		
		long number = -999; 
		String[] words = numStr.split(" ");
		if (words.length > 1) {
			if (tens.contains(words[0]) && digits.contains(words[1])) {
				number = (tens.indexOf(words[0]) * 10) + digits.indexOf(words[1]);
			} else if (tens.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = (tens.indexOf(words[0]) * 10) * hundreds.get(words[1]);
			} else if (digits.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = digits.indexOf(words[0]) * hundreds.get(words[1]);
			} else if (NumberUtils.isNumber(words[0]) && hundreds.containsKey(words[1])) {
				number = new Float(Float.parseFloat(words[0]) * hundreds.get(words[1])).longValue();
			}
		} else {
			if (tens.contains(words[0])) number = tens.indexOf(words[0]) * 10;
			else if (digits.contains(words[0])) number = digits.indexOf(words[0]);
			else if (NumberUtils.isNumber(words[0])) number = new Float(Float.parseFloat(words[0])).longValue();
		}
		
		if (number == -999) {
			return false;
		} else {
			if (number == num) return true;
			else return false;
		}
	}
	
	public boolean properNumber(String pos, String ner) {
		if (pos.equals("CD")
				&& !ner.equals("MONEY")
				&& !ner.equals("PERCENT")
				&& !ner.equals("DATE")
				&& !ner.equals("TIME")
				&& !ner.equals("DURATION")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	public void generateChildrenFile(JSONArray arr, String filePath) throws JSONException, IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, false)));
		
		String children;
		int numSent = 0;
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			String wikidataId = obj.getString("wikidata-id");
			System.out.println(wikidataId);
			children = "";
			
			String sChildrenOf = PREFIXES + "SELECT ?child ?childLabel"
					+ " WHERE"
					+ " {"
					+ " BIND(wd:" + wikidataId + " as ?parent) ."
					+ " ?parent wdt:P40 ?child ."
					+ " SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }"
					+ " }";
			JSONObject initialRes = executeSPARQLQuery(sChildrenOf);
			JSONArray results = initialRes.getJSONObject("results").getJSONArray("bindings");
			String resUri, resId;
			for (int j=0; j<results.length(); j++) {
				resUri = results.getJSONObject(j).getJSONObject("childLabel").getString("value");
				resId = resUri.substring(resUri.lastIndexOf("/")+1);
				children += resId + " | ";
			}
			out.println(wikidataId + "\t" + children);
		}
		out.close();
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		
		String trainFilepath = "./data/train-cardinality-filtered-num.json";
		String testFilepath = "./data/test-cardinality-filtered-num.json";
		
		WikidataChildren feat = new WikidataChildren();
		
		feat.generateChildrenFile(feat.readJSONArray(trainFilepath), "./data/train-children.tsv");
		feat.generateChildrenFile(feat.readJSONArray(testFilepath), "./data/test-children.tsv");
	}
	
}
