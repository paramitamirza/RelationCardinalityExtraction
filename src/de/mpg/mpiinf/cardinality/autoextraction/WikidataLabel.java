package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

public class WikidataLabel {
	
	public String getLabel(String wdId) throws MalformedURLException, IOException {
		String label = "";
		
		String wdApiUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&props=labels&ids=" + wdId + "&format=json&languages=en";
		BufferedReader in = new BufferedReader(new InputStreamReader(new URL(wdApiUrl).openStream()));
		
        String input = "", inputLine;
        while ((inputLine = in.readLine()) != null)
            input += inputLine + "\n";
        in.close();
        
        label = new JSONObject(input).getJSONObject("entities")
        		.getJSONObject(wdId)
        		.getJSONObject("labels")
        		.getJSONObject("en").getString("value");
		
		return label;
	}
	
	public static void main(String[] args) throws MalformedURLException, IOException {
		WikidataLabel wl = new WikidataLabel();
		System.out.println(wl.getLabel("Q3052772"));
	}

}
