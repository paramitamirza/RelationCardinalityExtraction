package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
	
	public void generateColumnsFile(JSONArray arr, String filePath) throws JSONException, IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
		PrintWriter outnummod = new PrintWriter(new BufferedWriter(new FileWriter(filePath.replace(".txt", "-nummod.txt"), true)));
		PrintWriter outnoone = new PrintWriter(new BufferedWriter(new FileWriter(filePath.replace(".txt", "-nummod-noone.txt"), true)));
		
		String label, dep, word, lemma;
		int numSent = 0;
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			float percent = (i/arr.length())*100;
			if (percent % 1.0 == 0 && percent > 0) System.err.print(percent+"%..");
			
			int numChild = obj.getInt("num-child");
			JSONArray lines = obj.getJSONArray("article-num-only");
			for (int j=0; j<lines.length(); j++) {
				Sentence sent = new Sentence(lines.getString(j));
				
				for (int k=0; k<sent.words().size(); k++) {
					label = "O";
					dep = "O";
					if (sent.incomingDependencyLabel(k).isPresent()) {
						dep = sent.incomingDependencyLabel(k).get();
					}
					word = sent.word(k);
					lemma = sent.lemma(k);
					if (properNumber(sent.posTag(k), sent.nerTag(k))) {
						lemma = getIntegerSimple(sent.word(k));
						if (k+1 < sent.words().size()) {
							if (properNumber(sent.posTag(k+1), sent.nerTag(k+1))) {
								if (matchNumbers (sent.lemma(k)+" "+sent.lemma(k+1), numChild)) {
									label = "CHILD";
									label = "CHILD";
								} 
							} else {
								if (matchNumbers (sent.lemma(k), numChild)) {
									label = "CHILD";
								}
							}
						}
					}
					out.println(word + "\t" + lemma + "\t" + sent.posTag(k) + "\t" + sent.nerTag(k) + "\t" + dep + "\t" + label);
					if (label.equals("CHILD") && !dep.equals("nummod")) label = "O";
					outnummod.println(word + "\t" + lemma + "\t" + sent.posTag(k) + "\t" + sent.nerTag(k) + "\t" + dep + "\t" + label);
					if (label.equals("CHILD") && word.equals("1")) label = "O";
					outnoone.println(word + "\t" + lemma + "\t" + sent.posTag(k) + "\t" + sent.nerTag(k) + "\t" + dep + "\t" + label);
					
				}
				numSent ++;
				out.println();
				outnummod.println();
				outnoone.println();
			}
		}
		System.out.println(numSent);
		out.close();
		outnummod.close();
		outnoone.close();
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		
		String trainFilepath = "./data/train-cardinality-filtered-num.json";
		String testFilepath = "./data/test-cardinality-filtered-num.json";
		
		FeatureExtraction feat = new FeatureExtraction();
		
		feat.generateColumnsFile(feat.readJSONArray(trainFilepath), "./data/train-cardinality.txt");
		feat.generateColumnsFile(feat.readJSONArray(testFilepath), "./data/test-cardinality.txt");
	}
	
}
