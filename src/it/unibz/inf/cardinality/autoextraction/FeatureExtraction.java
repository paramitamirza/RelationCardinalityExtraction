package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class FeatureExtraction {

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
	
	public String generateColumns(JSONArray arr, 
			boolean dependency, boolean excludeOne) throws JSONException {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			int numChild = obj.getInt("num-child");
			JSONArray lines = obj.getJSONArray("article-num-only");
			for (int j=0; j<lines.length(); j++) {
				Sentence sent = new Sentence(lines.getString(j));
				
				String[] labels = new String[sent.words().size()];
				for (int k=0; k<sent.words().size(); k++) {
					labels[k] = "O";
					if (properNumber(sent.posTag(k), sent.nerTag(k))) {
						if (k+1 < sent.words().size()) {
							if (properNumber(sent.posTag(k+1), sent.nerTag(k+1))) {
								if (dependency
										&& sent.incomingDependencyLabel(k).get().equals("nummod")
										&& sent.incomingDependencyLabel(k+1).equals("nummod")) {
									if (matchNumbers (sent.lemma(k)+" "+sent.lemma(k+1), numChild)) {
										labels[k] = "CHILD";
										labels[k+1] = "CHILD";
									} 
								} else {
									if (matchNumbers (sent.lemma(k)+" "+sent.lemma(k+1), numChild)) {
										labels[k] = "CHILD";
										labels[k+1] = "CHILD";
									} 
								}
							} else {
								if (dependency && sent.incomingDependencyLabel(k).get().equals("nummod")) {
									if (matchNumbers (sent.lemma(k), numChild)) {
										labels[k] = "CHILD";
										if (excludeOne && numChild == 1) {
											labels[k] = "O";
										}
									} 
								} else {
									if (matchNumbers (sent.lemma(k), numChild)) {
										labels[k] = "CHILD";
										if (excludeOne && numChild == 1) {
											labels[k] = "O";
										}
									} 
								}
							}
						}
					}
					sb.append(sent.word(k) + "\t" + sent.posTag(k) + "\t" + sent.nerTag(k) + "\t" + labels[k] + "\n");
				}
				sb.append("\n");
				
			}
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		
		String trainFilepath = "./data/train-cardinality-filtered-num.json";
		String testFilepath = "./data/test-cardinality-filtered-num.json";
	
		FeatureExtraction feat = new FeatureExtraction();
		String trainColumns = feat.generateColumns(feat.readJSONArray(trainFilepath), false, false);
		FileWriter file = new FileWriter("./data/train-cardinality.txt");
		file.write(trainColumns);
		file.close();
		
		String testColumns = feat.generateColumns(feat.readJSONArray(testFilepath), false, false);
		file = new FileWriter("./data/test-cardinality.txt");
		file.write(testColumns);
		file.close();
		
		trainColumns = feat.generateColumns(feat.readJSONArray(trainFilepath), true, false);
		file = new FileWriter("./data/train-cardinality-nummod.txt");
		file.write(trainColumns);
		file.close();
		
		testColumns = feat.generateColumns(feat.readJSONArray(testFilepath), true, false);
		file = new FileWriter("./data/test-cardinality-nummod.txt");
		file.write(testColumns);
		file.close();
		
		trainColumns = feat.generateColumns(feat.readJSONArray(trainFilepath), true, true);
		file = new FileWriter("./data/train-cardinality-nummod-noone.txt");
		file.write(trainColumns);
		file.close();
	}
	
}
