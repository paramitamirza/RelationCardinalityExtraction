package de.mpg.mpiinf.cardinality.experiments;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class WikidataNumberOfDivision {
	
	public String divFilePath = "./data/wikidata_divisions.csv";
	
	public WikidataNumberOfDivision() {
		
	}
	
	public WikidataNumberOfDivision(String divFilePath) {
		this.divFilePath = divFilePath;
	}

	public static void main(String[] args) throws Exception {
		WikidataNumberOfDivision numOfDiv = new WikidataNumberOfDivision();
		
		numOfDiv.matchNumberOfChildrenTrain(numOfDiv.divFilePath, "./data/wikidata-divisions-cardinality.json");
	}
	
	public String getWikipediaTextFromTitle(String title) throws IOException, JSONException {
//		URL wiki = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&explaintext&redirects=true&titles=" + URLEncoder.encode(title, "UTF-8"));
		URL wiki = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&explaintext&redirects=true&titles=" + title);
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
	
	private List<String> filterTextNumberPerson(String articleText) throws IOException {
		List<String> filtered = new ArrayList<String>();
		
		for (String line : articleText.split("\\r?\\n")) {
			Document doc = new Document(line);
			
			for (Sentence sent : doc.sentences()) {
				boolean personFound = false, numberFound = false;
				for (int i=0; i<sent.words().size(); i++) {
//					System.out.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
					if (sent.posTag(i).equals("NNP")
							&& sent.nerTag(i).equals("PERSON")) {
						personFound = true;
						break;
					} else if (sent.posTag(i).equals("CD")
							&& !sent.word(i).contains("=")
							&& !sent.nerTag(i).equals("MONEY")
							&& !sent.nerTag(i).equals("PERCENT")
							&& !sent.nerTag(i).equals("DATE")
							&& !sent.nerTag(i).equals("TIME")
							&& !sent.nerTag(i).equals("DURATION")
							&& !sent.nerTag(i).equals("SET")) {
						numberFound = true;
						break;
					} else if (sent.posTag(i).equals("JJ")
							&& sent.nerTag(i).equals("ORDINAL")) {
						numberFound = true;
						break;
					}
				}
				if (personFound || numberFound) {
//							System.out.println(sent.text());
					filtered.add(sent.text());
				}
	        }
	    }
		return filtered;
	}
	
	private List<String> filterTextNumber(String articleText) throws IOException {
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
									&& !sent.word(i).contains("=")
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
	
	public void matchNumberOfChildrenTrain(String filepath, String jsonPath) throws JSONException, IOException, InterruptedException {
		BufferedReader br;
		String line;
		
		String eid = "", name = "", countSeries = ""; 
		int match = 0;		
		
		//Get people with number of chidren
		System.out.println("Read number of division...");
		br = new BufferedReader(new FileReader(filepath));
		line = br.readLine();
		
		PrintWriter json = new PrintWriter(jsonPath);
		
		while (line != null) {
			
//	        System.out.println(line);
	        eid = line.split(",")[0];
	        name = line.split(",")[1];
	        if (line.split(",")[2].equals("NULL")) countSeries = "null";
	        else countSeries = line.split(",")[2];
		        
	        System.out.println(eid + "\t" + name + "\t" + countSeries);
				
			String wikipediaText = getWikipediaTextFromTitle(name);
			if (wikipediaText != "") {
				List<String> articleText = filterTextNumberPerson(wikipediaText);
				
				if (articleText.size() > 0) {
//					System.out.println(eid + "\t" + name + "\t" + numChild + "\t" + StringUtils.join(articleText, "|"));
						
					JSONObject obj = new JSONObject();
					obj.put("wikidata-id", eid);
					obj.put("wikidata-label", name);
					obj.put("count-division", countSeries);

					JSONArray list = new JSONArray();
					for (String s : articleText) {
						list.put(s);
					}
					obj.put("article-num-only", list);
					json.write(obj.toString() + "\n");
					
					match ++;
				}
			}
				
       		line = br.readLine();
	    }
		System.out.println(match);
		
		br.close();
		json.close();
		
//		try {
//
//			FileWriter file = new FileWriter("./data/wikidata-count-cardinality.json");
//			file.write(result.toString(4));
//			file.flush();
//			file.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
}
