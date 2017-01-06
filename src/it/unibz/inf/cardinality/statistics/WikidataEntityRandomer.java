package it.unibz.inf.cardinality.statistics;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class WikidataEntityRandomer {

	private static final String PREFIXES = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
			+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX wikibase: <http://wikiba.se/ontology#>\n" + "PREFIX schema: <http://schema.org/>\n" + "\n";
	private static final String PROP_OCCUPATION = "wdt:P106";
	private static final String PROP_POSITION_HELD = "wdt:P39";
	private static final String PROP_INSTANCE_OF = "wdt:P31";
	private static final String OBJ_HUMAN = "wd:Q5";
	private static final String OBJ_POLITICIAN = "wd:Q82955";
	private static final String OBJ_BBPLAYER = "wd:Q3665646";
	private static final String OBJ_PROFESSOR = "wd:Q121594";
	private static final String OBJ_US_PRESIDENT = "wd:Q11696";
	private static final HashMap<Pair<String, String>, Set<String>> objs = new HashMap<Pair<String, String>, Set<String>>();
	public static final String WD_SPARQL_ENDPOINT = "https://query.wikidata.org/bigdata/namespace/wdq/sparql";

	public static void main(String[] args) {
		try {
//			getRandomEntities(PROP_OCCUPATION, OBJ_POLITICIAN, 10);
//			getRandomEntities(PROP_OCCUPATION, OBJ_BBPLAYER, 10);
			getRandomEntities(PROP_INSTANCE_OF, OBJ_HUMAN, 100);
//			getRandomEntities(PROP_OCCUPATION, OBJ_PROFESSOR, 10);
//			getRandomEntities(PROP_POSITION_HELD, OBJ_US_PRESIDENT, 10);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static int WDCount(String prop, String obj) throws JSONException {
		String sCount = PREFIXES + "SELECT (COUNT(DISTINCT ?v) AS ?count) WHERE { ?v " + prop + " " + obj + "}";
		JSONObject initialRes = executeSPARQLQuery(sCount);
		return initialRes.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("count")
				.getInt("value");
	}

	private static void getRandomEntities(String prop, String obj, int randomCount) throws JSONException {
		int allCount = WDCount(prop, obj);

		Stack<Integer> randomPool = new Stack<Integer>();
		for (int i = 0; i < allCount; i++) {
			randomPool.add(i);
		}
		int i = 0;
		while (i < randomCount) {
			Collections.shuffle(randomPool);
			int offset = randomPool.pop();

			String q = PREFIXES + "SELECT ?v WHERE { ?v " + prop + " " + obj + "} LIMIT 1 OFFSET " + offset;
			JSONObject initialRes = executeSPARQLQuery(q);
			String uri = initialRes.getJSONObject("results").getJSONArray("bindings").getJSONObject(0)
					.getJSONObject("v").getString("value");
			String eid = uri.substring(uri.lastIndexOf("/")+1);
			//System.out.println(eid);
			
			String q2 = PREFIXES + "SELECT ?v WHERE { ?v ?p wd:" + eid + "}";
			JSONObject initialRes2 = executeSPARQLQuery(q2);
			JSONArray res2 = initialRes2.getJSONObject ("results").getJSONArray("bindings");
			boolean found = false;
			for (int j=0; j<res2.length(); j++) {
				if (res2.getJSONObject(j).getJSONObject("v").getString("value").contains("https://en.wikipedia.org/wiki/")) {
					found = true;
					break;
				}
			}
			if (found) {
				System.out.println(uri);
				i++;
			}
		}
	}

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

}
