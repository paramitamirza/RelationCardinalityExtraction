package de.mpg.mpiinf.cardinality.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DBPediaChildren {
	
	/**
	SELECT rdf_schema_label, child_label
	FROM [fh-bigquery:dbpedia.person]
	WHERE rdf_schema_label <> "NULL" AND child_label <> "NULL"
	 * @throws IOException 
	*/
	
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
	
	public void mapDbpediaWikidata(JSONArray arr, String filePath) throws IOException, JSONException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, false)));
		
		WikidataMapping map = new WikidataMapping();
		
		String rdfSchemaLabel, wikidataId, children;
		int numSent = 0;
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			rdfSchemaLabel = obj.getString("rdf_schema_label");
			wikidataId = map.getWikidataIDFromTitle(rdfSchemaLabel);
			children = " " + java.net.URLDecoder.decode(obj.getString("child_label"), "UTF-8") + " ";
			children = children.replace("_", " ");
			children = children.replace("{", "");
			children = children.replace("},", "");
			children = children.replace("|", " | ");
			
			if (wikidataId != "") {
				out.println(wikidataId + "\t" + rdfSchemaLabel + "\t" + children);
			}
		}
		out.close();
	}
	
	public static void main(String[] args) throws IOException, JSONException {
		String dbpediaResult = "./data/dbpedia-child-20170112.json";
		
		DBPediaChildren feat = new DBPediaChildren();
		
		feat.mapDbpediaWikidata(feat.readJSONArray(dbpediaResult), "./data/dbpedia_children.tsv");
	}

}
