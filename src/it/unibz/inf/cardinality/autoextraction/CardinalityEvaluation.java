package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.simple.Sentence;

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
	
	public void evaluate(JSONArray arr, String filepath) throws JSONException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		
		String line, label, prob;
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			int numChild = obj.getInt("num-child");
			String wikiId = obj.getString("wikidata-id");
			String wikiLabel = obj.getString("wikidata-label");
			JSONArray lines = obj.getJSONArray("article-num-only");
			
			for (int j=0; j<lines.length(); j++) {
				Sentence sent = new Sentence(lines.getString(j));
				line = br.readLine();
				
				String[] nums = new String[sent.words().size()];
				Double[] probs = new Double[sent.words().size()];
				for (int k=0; k<sent.words().size(); k++) {
					line = br.readLine();
					
					System.out.println(sent.word(k) + "-" + line);
					label = line.split("\t")[6].split("/")[0];
					prob = line.split("\t")[6].split("/")[1];
					if (label.equals("CHILD")) {
						nums[k] = sent.word(k);
						probs[k] = Double.parseDouble(prob);
					}
				}
				
				System.out.println(nums);
				System.out.println(probs);
				
				line = br.readLine();
			}
		}
	}

	public static void main(String[] args) throws JSONException, IOException {
		
		String resultPath = "./data/out-cardinality.txt";
		String jsonPath = "./data/test-cardinality-filtered-num.json";
		
		CardinalityEvaluation eval = new CardinalityEvaluation();
		eval.evaluate(eval.readJSONArray(jsonPath), resultPath);
	}

}
