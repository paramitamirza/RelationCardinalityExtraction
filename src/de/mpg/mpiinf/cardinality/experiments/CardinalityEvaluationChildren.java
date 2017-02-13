package de.mpg.mpiinf.cardinality.experiments;

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

import static java.lang.Math.toIntExact;

public class CardinalityEvaluationChildren {
	
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
				&& !ner.equals("SET")
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
	
	public Map<Long, String> extractNumber(List<String> nums, List<Double> probs) {
		Map<Long, String> numChild = new HashMap<Long, String>();
		String number = "";
		Double prob = 0.0;
		int idx = 0;
		for (int i=0; i<nums.size(); i++) {
			if (!nums.get(i).equals("")) {
				number = nums.get(i);
				prob = probs.get(i);
				if (i+1<nums.size() && !nums.get(i+1).equals("")) {
					number += " " + nums.get(i+1);
					prob = (prob + probs.get(i))/2;
					idx = i;
					i ++;
				}
				if (getInteger(number) > 0) {
					numChild.put(getInteger(number), i + "#" + prob);
				}
			}
		}
		return numChild;
	}
	
	public String wordsToSentence(List<String> words, int idx) {
		String sent = "";
		for (int i=0; i<words.size(); i++) {
			if (i == idx) sent += "[" + words.get(i) + "]" + " ";
			else sent += words.get(i) + " ";
		}
		return sent.substring(0, sent.length()-1);
	}
	
	public String wordsToSentence(List<String> words, List<Integer> idx) {
		String sent = "";
		for (int i=0; i<words.size(); i++) {
			if (idx.contains(i)) sent += "[" + words.get(i) + "]" + " ";
			else sent += words.get(i) + " ";
		}
		return sent.substring(0, sent.length()-1);
	}
	
	public void evaluate(String arrpath, String filepath, String[] labels, boolean addSameSentence, boolean addDiffSentence) throws JSONException, IOException {
		BufferedReader br; String line;
		Map<String, Integer> peopleNumChild = new HashMap<String, Integer>();
		Map<String, String> peopleLabel = new HashMap<String, String>();
		br = new BufferedReader(new FileReader(arrpath));
		line = br.readLine();
		while (line != null) {
			peopleNumChild.put(line.split(",")[0], Integer.parseInt(line.split(",")[4]));
			peopleLabel.put(line.split(",")[0], line.split(",")[1]);
			line = br.readLine();
		}
		br.close();
		
		br = new BufferedReader(new FileReader(filepath));
		
		String label;
		Double prob = 0.0;
		List<String> nums = new ArrayList<String>();
		List<Double> probs = new ArrayList<Double>();
		
		int tp = 0;
		int fp = 0;
		int total = 0;
		
		double lower = 0.1;
		double upper = 1.0;
		
		String[] cols;
		List<String> sentence = new ArrayList<String>();
		String personId = null;
		line = br.readLine();
		
		long predictedNumChild = 0;
		double predictedProb = 0.0;
		int numPredicted = 0;
		String childLine = "";
		
		while (line != null) {
			
			if(!StringUtils.join(nums, "").equals("")) {
				Map<Long, String> numbers = extractNumber(nums, probs);
				long n = 0;
				double p = 0.0, pp;
				int m = 0, mm;
				List<Integer> mlist = new ArrayList<Integer>();
				
				if (addSameSentence) {	
					//When there are more than one in a sentence, add them up
					for (Long key : numbers.keySet()) {
						pp = Double.parseDouble(numbers.get(key).split("|")[1]);
						mm = Integer.parseInt(numbers.get(key).split("|")[0]);
						if (pp > p) {
							n += key;
							p += pp;
							mlist.add(mm);
						}
					}
					p = p/numbers.size();
				} else {	
					//When there are more than one in a sentence, choose the most probable
					for (Long key : numbers.keySet()) {
						pp = Double.parseDouble(numbers.get(key).split("#")[1]);
						mm = Integer.parseInt(numbers.get(key).split("#")[0]);
						if (pp > p) {
							n = key;
							p = pp;
							m = mm;
						}
					}
				}
				
				if (addDiffSentence) {	
					//When there are more than one sentences, add them up
					if (p > lower && p < upper) {
						predictedNumChild += n;
						predictedProb += p;
						childLine += wordsToSentence(sentence, mlist) + "|";
						numPredicted++;
					}
					predictedProb = predictedProb/numPredicted;
				} else {
					//When there are more than one sentences, choose the most probable
					if (p > predictedProb
							&& p > lower && p < upper) {
						predictedNumChild = n;
						predictedProb = p;
						childLine = wordsToSentence(sentence, m);
					}
				}
			}
			
			//Sentence starts			
			
			nums = new ArrayList<String>();
			probs = new ArrayList<Double>();
			sentence = new ArrayList<String>();
			
			line = br.readLine();
			line = br.readLine();
			
			while (line != null && !line.equals("")) {
				cols = line.split("\t");
				
				if (personId != null && !cols[0].equals(personId)) {	//Entity ends
					int numChild = peopleNumChild.get(personId);
					String wikiLabel = peopleLabel.get(personId);
					
					System.out.println(personId + ",https://en.wikipedia.org/wiki/" + wikiLabel + "," + numChild + "," + predictedNumChild + "," + predictedProb + ",\"" + childLine + "\"");
					if (numChild > 0) {
						if (numChild == predictedNumChild) tp ++;
						else if (numChild != predictedNumChild && predictedNumChild > 0) fp ++;
						total ++;
					}
					
					predictedNumChild = 0;
					predictedProb = 0.0;
					numPredicted = 0;
					childLine = "";
				}
				
				personId = cols[0];
				sentence.add(cols[3]);
				for (int l=0; l<labels.length; l++) {
					if (labels[l].equals("YES")) {
						prob = Double.valueOf(cols[cols.length-labels.length+l].split("/")[1]);
					}
				}
				if (prob > lower && prob < upper) {
					nums.add(cols[3]);
					probs.add(prob);
				} else {
					nums.add("");
					probs.add(0.0);
				}
				
//				System.out.println(line);
				
				line = br.readLine();
			}
		}
		
		double precision = (double)tp / (tp + fp);
		double recall = (double)tp / total;
		double fscore = (2 * precision * recall) / (precision + recall);
		System.out.println("Precision: " + precision);
		System.out.println("Recall: " + recall);
		System.out.println("F1-score: " + fscore);
	}

	public static void main(String[] args) throws JSONException, IOException {
		
		String csvPath = "./acl_auto_extraction/wikidata_children_random200.csv";
		
		String resultPath = "./acl_auto_extraction/output/children_cardinality_lemma_yes_num_nummod.out";
		String[] labels = {"O", "YES"};
		
		
		CardinalityEvaluationChildren eval = new CardinalityEvaluationChildren();
		eval.evaluate(csvPath, resultPath, labels, false, false);
	}

}
