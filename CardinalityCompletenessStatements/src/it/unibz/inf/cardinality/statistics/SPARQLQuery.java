package it.unibz.inf.cardinality.statistics;

import java.io.ByteArrayOutputStream;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SPARQLQuery {
	
	public static final String PREFIXES = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
			+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX wikibase: <http://wikiba.se/ontology#>\n" + "PREFIX schema: <http://schema.org/>\n" + "\n";
	public static final String DBP_PREFIXES = "PREFIX dbo:<http://dbpedia.org/ontology/>\n"
			+ "PREFIX : <http://dbpedia.org/resource/>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#/>\n" + "\n";
	
	public static final String PROP_CHILD = "wdt:P40";
	public static final String PROP_NUM_OF_CHILDREN = "wdt:P1971";
	public static final String PROP_OCCUPATION = "wdt:P106";
	public static final String PROP_POSITION_HELD = "wdt:P39";
	public static final String PROP_INSTANCE_OF = "wdt:P31";
	public static final String PROP_DATE_OF_DEATH = "wdt:P570";
	public static final String OBJ_HUMAN = "wd:Q5";
	public static final String OBJ_POLITICIAN = "wd:Q82955";
	public static final String OBJ_BBPLAYER = "wd:Q3665646";
	public static final String OBJ_PROFESSOR = "wd:Q121594";
	public static final String OBJ_US_PRESIDENT = "wd:Q11696";
	
	public static final String WD_SPARQL_ENDPOINT = "https://query.wikidata.org/bigdata/namespace/wdq/sparql";
	public static final String DBP_SPARQL_ENDPOINT = "http://dbpedia.org/sparql";

	
	public SPARQLQuery() {
		
	}

	public static JSONObject execute(String q) throws JSONException {
		QueryExecution qExe = QueryExecutionFactory.sparqlService(WD_SPARQL_ENDPOINT, PREFIXES + q);
		ResultSet results = qExe.execSelect();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ResultSetFormatter.output(baos, results, ResultsFormat.FMT_RS_JSON);
		JSONTokener jst = new JSONTokener(baos.toString());
		JSONObject initialRes = new JSONObject(jst);
		qExe.close();
		return initialRes;
	}
	
	public static JSONObject executeDBPedia(String q) throws JSONException {
		QueryExecution qExe = QueryExecutionFactory.sparqlService(DBP_SPARQL_ENDPOINT, DBP_PREFIXES + q);
		ResultSet results = qExe.execSelect();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ResultSetFormatter.output(baos, results, ResultsFormat.FMT_RS_JSON);
		JSONTokener jst = new JSONTokener(baos.toString());
		JSONObject initialRes = new JSONObject(jst);
		qExe.close();
		return initialRes;
	}
	
}
