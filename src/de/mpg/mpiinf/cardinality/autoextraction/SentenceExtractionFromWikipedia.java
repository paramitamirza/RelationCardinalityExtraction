package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class SentenceExtractionFromWikipedia {
	
	private String inputCsvFile = "./data/auto_extraction/wikidata_sample_links.csv";
	private String outputJsonFile = "./data/auto_extraction/wikidata_sample.jsonl.gz";
	
	public SentenceExtractionFromWikipedia() {
		
	}
	
	public SentenceExtractionFromWikipedia(String inputCsvFilePath, String outputJsonFilePath) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setOutputJsonFile(outputJsonFilePath);
	}
	
	public static void main(String[] args) throws Exception {
		
		SentenceExtractionFromWikipedia sentExtraction;
		if (args.length < 2) {
			sentExtraction = new SentenceExtractionFromWikipedia();
		} else {
			sentExtraction = new SentenceExtractionFromWikipedia(args[0], args[1]);
		}
		
		sentExtraction.extractSentences();
	}
	
	
	public void extractSentences() throws JSONException, IOException, InterruptedException {
		BufferedReader br;
		BufferedWriter bw;
		String line;		
		String eid = "", label = "", count = "";
		
		br = new BufferedReader(new FileReader(this.getInputCsvFile()));
		line = br.readLine();		
//		PrintWriter json = new PrintWriter(this.getOutputJsonFile());
		bw = new BufferedWriter(
                new OutputStreamWriter(
                        new GZIPOutputStream(new FileOutputStream(this.getOutputJsonFile()))
                    ));
		
		System.out.println("Extract Wikipedia sentences (containing numbers) per Wikidata instance...");
		while (line != null) {
			
//	        System.out.println(line);
	        eid = line.split(",")[0];
	        label = line.split(",")[1];
	        count = line.split(",")[2];
	        System.out.println(eid + "\t" + label + "\t" + count);
				
			String wikipediaText = getWikipediaTextFromTitle(label);
			if (wikipediaText != "") {
				List<String> articleText = filterText(wikipediaText, false, false);	//ordinal=false, namedEntity=false --> only cardinal numbers!!
				
				if (articleText.size() > 0) {
//					System.out.println(eid + "\t" + name + "\t" + numChild + "\t" + StringUtils.join(articleText, "|"));
						
					JSONObject obj = new JSONObject();
					obj.put("wikidata-id", eid);
					obj.put("wikidata-label", label);
					obj.put("count", count);

					JSONArray list = new JSONArray();
					for (String s : articleText) {
						list.put(s);
					}
					obj.put("article", list);
//					json.write(obj.toString() + "\n");
					bw.write(obj.toString());
					bw.newLine();
				}
			}
				
       		line = br.readLine();
	    }
		
		br.close();
//		json.close();
		bw.close();
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
	
	public boolean containNumbers(String transformed, Sentence sent, 
			boolean ordinal, boolean namedEntity) {
		
		if (transformed.contains("LatinGreek_")) {
			return true;
			
		} else {
			boolean entityFound = false, numberFound = false, ordinalFound = false;
			for (int i=0; i<sent.words().size(); i++) {
//				System.err.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
				if (sent.posTag(i).equals("CD")
						&& !sent.word(i).contains("=")
						&& !sent.nerTag(i).equals("MONEY")
						&& !sent.nerTag(i).equals("PERCENT")
						&& !sent.nerTag(i).equals("DATE")
						&& !sent.nerTag(i).equals("TIME")
						&& !sent.nerTag(i).equals("DURATION")
						&& !sent.nerTag(i).equals("SET")) {
					numberFound = true;
					break;
				} else if (ordinal && sent.posTag(i).equals("JJ")
						&& sent.nerTag(i).equals("ORDINAL")) {
					ordinalFound = true;
					break;
				} else if (namedEntity && sent.posTag(i).equals("NNP")
						&& (sent.nerTag(i).equals("PERSON")
								|| sent.nerTag(i).equals("LOCATION")
								|| sent.nerTag(i).equals("ORGANIZATION"))) {
					entityFound = true;
					break;
				}
			}
			if (ordinal && (numberFound || ordinalFound)) {
				return true;
			} else if (namedEntity && (numberFound || entityFound)) {
				return true;
			} else if (numberFound) {
				return true;
			}
		}
		return false;
	}
	
	public List<String> filterText(String articleText, boolean ordinal, boolean namedEntity) throws IOException {
		List<String> filtered = new ArrayList<String>();
		String transformed;
		Sentence sent;
		
		Transform trans = new Transform();
		
		for (String line : articleText.split("\\r?\\n")) {
			Document doc = new Document(line);
			
			for (Sentence s : doc.sentences()) {
				
				transformed = trans.transform(s.text(), false, false, true, true);
				sent = new Sentence(transformed);
				if (containNumbers(transformed, sent, ordinal, namedEntity)) {
					filtered.add(sent.text());
				}
				
	        }
	    }
		return filtered;
	}

	public String getInputCsvFile() {
		return inputCsvFile;
	}

	public void setInputCsvFile(String inputCsvFile) {
		this.inputCsvFile = inputCsvFile;
	}

	public String getOutputJsonFile() {
		return outputJsonFile;
	}

	public void setOutputJsonFile(String outputJsonFile) {
		this.outputJsonFile = outputJsonFile;
	}
	
	
}
