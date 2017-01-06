package it.unibz.inf.cardinality.manualextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;

public class WikidataMapping {
	
	private String inputFile;
	
	public WikidataMapping() {
		
	}
	
	public WikidataMapping(String filename) {
		setInputFile(filename);
	}
	
	public String getWikidataIDFromTitle(String title) throws IOException, JSONException {
		URL wiki = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&titles=" + URLEncoder.encode(title, "UTF-8"));
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
		
        return "";
	}
	
	public Integer getArticleLengthFromTitle(String title) throws IOException, JSONException {
		URL wiki = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=info&format=json&titles=" + URLEncoder.encode(title, "UTF-8"));
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(wiki.openStream()));
		String output = in.readLine();
		String wikidata = "length\":(\\d+)";
		Pattern wikidataP = Pattern.compile(wikidata);
		Matcher m = wikidataP.matcher(output);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}
		in.close();
		
        return 0;
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}
	
	public static void main(String[] args) throws IOException, JSONException {
		
		WikidataMapping map = new WikidataMapping();
		System.out.println(map.getWikidataIDFromTitle("Nejat Eczacıbaşı"));
		System.out.println(map.getArticleLengthFromTitle("Nejat Eczacıbaşı"));
//		
//		WikidataMapping map = new WikidataMapping(args[0]);
//		
//		BufferedReader br = new BufferedReader(new InputStreamReader (
//				new FileInputStream(map.getInputFile()), "UTF-16"));
//		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
//				new FileOutputStream(map.getInputFile().replace(".txt", "_wikidata.txt")), "UTF-16"));
//		PrintWriter writer = new PrintWriter(bw);
//		
//		String line;
//		while ((line = br.readLine()) != null) {
//			WikiArticle wa = new WikiArticle(line);
//			wa.setWikidataID(map.getWikidataIDFromTitle(wa.getTitle()));
//			wa.setLength(map.getArticleLengthFromTitle(wa.getTitle()));
//			writer.println(wa.toString());
//	    }
//		br.close();
//		bw.close();
		
//		System.out.println("https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&titles=" + URLEncoder.encode("Nejat Eczacıbaşı", "UTF-8"));
	}

}
