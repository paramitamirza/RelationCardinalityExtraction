package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.StringUtils;

public class CardinalityEvaluation {
	
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
	
	public long getInteger(String numStr) {
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
		
		return number;
	}
	
	public Map<Long, Double> extractNumber(List<String> nums, List<Double> probs) {
		Map<Long, Double> numChild = new HashMap<Long, Double>();
		String number = "";
		Double prob = 0.0;
		for (int i=0; i<nums.size(); i++) {
			if (!nums.get(i).equals("")) {
				number = nums.get(i);
				prob = probs.get(i);
				if (!nums.get(i+1).equals("")) {
					number += " " + nums.get(i+1);
					prob = (prob + probs.get(i))/2;
					i ++;
				}
				if (getInteger(number) > 0) {
					numChild.put(getInteger(number), prob);
				}
			}
		}
		return numChild;
	}
	
	public void evaluate(JSONArray arr, String filepath) throws JSONException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		
		String line, label;
		Double prob;
		List<String> nums;
		List<Double> probs;
		
		int tp = 0;
		int fp = 0;
		
		double threshold = 0.75;
		
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			int numChild = obj.getInt("num-child");
			String wikiId = obj.getString("wikidata-id");
			String wikiLabel = obj.getString("wikidata-label");
			JSONArray lines = obj.getJSONArray("article-num-only");
			
			long predictedNumChild = 0;
			double predictedProb = 0.0;
			int numPredicted = 0;
			String childLine = "";
			
			for (int j=0; j<lines.length(); j++) {
				line = br.readLine();
				line = br.readLine();
				nums = new ArrayList<String>();
				probs = new ArrayList<Double>();
				while (!line.equals("")) {
//					System.out.println(line);
					label = line.split("\t")[6].split("/")[0];
					prob = Double.parseDouble(line.split("\t")[6].split("/")[1]);
					if (label.equals("NCHILD")) {
						nums.add(line.split("\t")[0]);
						probs.add(prob);
					} else {
						nums.add("");
						probs.add(0.0);
					}
					line = br.readLine();
				}
				
				if(!StringUtils.join(nums, "").equals("")) {
					int n = 0;
					double p = 0.0;
					Map<Long, Double> numbers = extractNumber(nums, probs);
					for (Long key : numbers.keySet()) {
						n += key;
						p += numbers.get(key);
					}
					p = p/numbers.size();
					
					//When there are more than one sentences, choose the most probable
//					if (p > predictedProb
//							&& p > threshold) {
//						predictedNumChild = n;
//						predictedProb = p;
//						childLine = lines.getString(j);
//					}
					
					//When there are more than one sentences, add them up
					if (p > threshold) {
						predictedNumChild += n;
						predictedProb += p;
						childLine += lines.getString(j) + "|";
						numPredicted++;
					}
					predictedProb = predictedProb/numPredicted;
				}
				
				line = br.readLine();
			}
			
			System.out.println(wikiLabel + " (" + wikiId + ")\t" + numChild + "\t" + predictedNumChild + "\t" + predictedProb + "\t" + childLine);
			if (numChild == predictedNumChild) tp ++;
			else if (numChild != predictedNumChild && predictedNumChild > 0) fp ++;
		}
		
		double precision = (double)tp / (tp + fp);
		double recall = (double)tp / arr.length();
		double fscore = (2 * precision * recall) / (precision + recall);
		System.out.println("Precision: " + precision);
		System.out.println("Recall: " + recall);
		System.out.println("F1-score: " + fscore);
	}

	public static void main(String[] args) throws JSONException, IOException {
		
		String resultPath = "./data/crf_output/out_cardinality_lemma_ner.txt";
		String jsonPath = "./data/auto_extraction/20170116-test-cardinality.json";
		
		CardinalityEvaluation eval = new CardinalityEvaluation();
		eval.evaluate(eval.readJSONArray(jsonPath), resultPath);
	}

}
