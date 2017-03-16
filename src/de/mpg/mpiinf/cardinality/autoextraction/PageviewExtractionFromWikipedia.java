package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PageviewExtractionFromWikipedia {
	
	public PageviewExtractionFromWikipedia() {
		
	}
	
	public static void main(String[] args) throws Exception {
		
		PageviewExtractionFromWikipedia pvExtraction = new PageviewExtractionFromWikipedia();
//		System.out.println(pvExtraction.getPageviewsFromTitle("John_Thomas_(photographer)"));
		
		BufferedReader br = new BufferedReader(new FileReader("./auto_extraction/data/wikidata_division.csv"));
		BufferedWriter bw = new BufferedWriter(new FileWriter("./auto_extraction/data/wikidata_division.csv".replace(".csv", "_pageviews.csv")));
		
		String line = br.readLine();
		String title;
		boolean cont = true;
		while (line != null) {
			title = line.split(",")[1];
//			if (line.split(",")[0].equals("Q294435")) cont = true;
			if (cont) {
				System.out.println(line);
				bw.write(line + "," + pvExtraction.getPageviewsFromTitle(title) + "\n");
			}
			line = br.readLine();
		}
		
		br.close();
		bw.close();
	}
	
	public int getPageviewsFromTitle(String title) throws IOException, JSONException {
		URL wiki = new URL("https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/en.wikipedia/all-access/all-agents/"+title+"/monthly/20150101/20170101");
		
		HttpURLConnection con = null;
		con = (HttpURLConnection) wiki.openConnection();
        con.connect();
        
        if( con.getResponseCode() < 400) {
		
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(wiki.openStream()));
			String output = in.readLine();
			JSONObject out = new JSONObject(output);
			JSONArray pageviews = out.getJSONArray("items");
			int views = 0;
			for (int i = 0; i < pageviews.length(); i++) {
			    JSONObject row = pageviews.getJSONObject(i);
				views += row.getInt("views");
			}
		
			return views;
		
        } else {
        	return 0;
        }
	}
	
	
}
