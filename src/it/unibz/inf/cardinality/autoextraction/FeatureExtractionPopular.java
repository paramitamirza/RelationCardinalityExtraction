package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class FeatureExtractionPopular {
	
	public String[] digitsArr = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
	public String[] tensArr = {"ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
	public List<String> digits = Arrays.asList(digitsArr);
	public List<String> tens = Arrays.asList(tensArr);
	public Map<String, Integer> hundreds = new HashMap<String, Integer>();
	
	public FeatureExtractionPopular() {
		hundreds.put("hundred", 100);
		hundreds.put("thousand", 1000);
		hundreds.put("million", 1000000);
	}

	public JSONArray readJSONArray(String filepath) throws IOException, JSONException {
		JSONArray arr = new JSONArray();
		BufferedReader br = new BufferedReader(new FileReader(filepath));
//		String everything;
		JSONObject obj;
		try {
//		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		    	obj = new JSONObject(line);
		    	arr.put(obj);
//		        sb.append(line);
//		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
//		    everything = sb.toString();
		} finally {
		    br.close();
		}
//		return new JSONArray(everything);
		return arr;
	}
	
	public Long getInteger(String numStr) {
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
	
	public String getIntegerSimple(String numStr) {
		if (tens.contains(numStr)) return "_ten_";
		else if (digits.contains(numStr)) return "_num_";
		else if (NumberUtils.isNumber(numStr)) return "_num_";
		else if (hundreds.containsKey(numStr)) return "_hundred_";
		
		return numStr;
	}
	
	public boolean matchNumbers(String numStr, int num) {
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
				&& !ner.equals("SET")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean properName(String pos, String ner) {
		if (pos.equals("NNP")
				&& ner.equals("PERSON")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean containNumberPerson(String sentence) throws IOException {
		Sentence sent = new Sentence(sentence);
		boolean personFound = false, numberFound = false;
		for (int i=0; i<sent.words().size(); i++) {
//			System.out.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
			if (sent.posTag(i).equals("NNP")
					&& sent.nerTag(i).equals("PERSON")) {
				personFound = true;
				break;
			}
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
			}
		}
		return (personFound || numberFound);
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
	
	private boolean containProperNumber(String sentence) throws IOException {
		Sentence sent = new Sentence(sentence);
		boolean numberFound = false;
		if (containNumber(sent.text())) {			
			for (int i=0; i<sent.words().size(); i++) {
//				System.out.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
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
				}
			}
		}
		return numberFound;
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep, String label) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep + "\t" + label;
	}
	
	public void generateColumnsFile(String filepath, String randomFilepath) throws JSONException, IOException {
//		PrintWriter trainCount = new PrintWriter(new BufferedWriter(new FileWriter("./data/train_cardinality_count.txt", false)));
//		PrintWriter trainNum = new PrintWriter(new BufferedWriter(new FileWriter("./data/train_cardinality_num.txt", false)));
//		PrintWriter test = new PrintWriter(new BufferedWriter(new FileWriter("./data/random_test_cardinality.txt", false)));
		
		System.out.println("Read random filepath...");
		List<String> randomPersons = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(randomFilepath));
		String line = br.readLine();		
		while (line != null) {
			randomPersons.add(line.split(",")[0]);
			line = br.readLine();
		}
		br.close();
		System.out.println(randomPersons);
				
		String label, dep;
		long numInt;
		long numToAdd;
		List<Integer> idxToAdd;
		int numSent = 0;
		
		br = new BufferedReader(new FileReader(filepath));
		line = br.readLine();
		JSONObject obj;
		JSONArray lines;
		String wikidataId, numChild, countChild;
		int numOfChild = -99;
		PrintWriter outfile;
		
//		File test = new File("./data/random_test_cardinality.txt");
//		File trainNum = new File("./data/train_cardinality_num.txt");
//		File trainCount = new File("./data/train_cardinality_count.txt");
//		Files.deleteIfExists(trainCount.toPath());
//		Files.deleteIfExists(trainNum.toPath());
//		Files.deleteIfExists(test.toPath());
		
		int xnum=-1, xcount=-1;
		while (line != null) {
			randomPersons.add(line.split(",")[0]);
			obj = new JSONObject(line);
			
			numChild = obj.getString("num-child");
			countChild = obj.getString("count-child");
			lines = obj.getJSONArray("article-num-only");
			wikidataId = obj.getString("wikidata-id");
//			System.out.println(wikidataId + "\t" + countChild + "\t" + numChild + "\t" + lines);
			
			if (randomPersons.contains(wikidataId)) {
				outfile = new PrintWriter(new BufferedWriter(new FileWriter("./data/random_test_cardinality.txt", true)));
			} else {
				if (!numChild.equals("null")) {
					numOfChild = Integer.parseInt(numChild);
					xnum++;
					outfile = new PrintWriter(new BufferedWriter(new FileWriter("./data/train_cardinality_num_"+(xnum/1000)+".txt", true)));
				}
				else {
					numOfChild = Integer.parseInt(countChild);
					xcount++;
					outfile = new PrintWriter(new BufferedWriter(new FileWriter("./data/train_cardinality_count_"+(xcount/1000)+".txt", true)));
				}
			}
			
			for (int j=0; j<lines.length(); j++) {
				if (containProperNumber(lines.getString(j))) {
					
	//				System.out.println(lines.getString(j));
					Sentence sent = new Sentence(lines.getString(j));
					
					String[] labels = new String[sent.words().size()];
					idxToAdd = new ArrayList<Integer>();
					numToAdd = 0;
					
					for (int k=0; k<sent.words().size(); k++) {
						labels[k] = "O";
						
						if (properNumber(sent.posTag(k), sent.nerTag(k))) {
							numInt = getInteger(sent.lemma(k));
							
							if (k+1 < sent.words().size()) {
								if (properNumber(sent.posTag(k+1), sent.nerTag(k+1))) {
									numInt = getInteger(sent.lemma(k)+" "+sent.lemma(k+1));
									
									if (numToAdd > 0) {
										if ((numToAdd+numInt) == numOfChild) {
											labels[k] = "NCHILD";
											labels[k+1] = "NCHILD";
											for (Integer nnn : idxToAdd) labels[nnn] = "NCHILD";
											numToAdd = 0;
											idxToAdd.clear();
											
										} else if ((numToAdd+numInt) < numOfChild) {
											numToAdd += numInt;
											idxToAdd.add(k); idxToAdd.add(k+1);
										}
									} else {
										if (numInt == numOfChild) {
											labels[k] = "NCHILD";
											labels[k+1] = "NCHILD";
											
										} else if (numInt < numOfChild) {
											numToAdd += numInt;
											idxToAdd.add(k); idxToAdd.add(k+1);
											
										}
									}
									
								} else {								
									if (numToAdd > 0) {
										if ((numToAdd+numInt) == numOfChild) {
											labels[k] = "NCHILD";
											for (Integer nnn : idxToAdd) labels[nnn] = "NCHILD";
											numToAdd = 0;
											idxToAdd.clear();
											
										} else if ((numToAdd+numInt) < numOfChild) {
											numToAdd += numInt;
											idxToAdd.add(k);
										}
									} else {
										if (numInt == numOfChild) {
											labels[k] = "NCHILD";
											
										} else if (numInt < numOfChild) {
											numToAdd += numInt;
											idxToAdd.add(k);
											
										}
									}
								}
							}
						}
					}
					
					String word = "", lemma = "", pos = "", ner = "", deprel = "";
					boolean lrb = false;
					int k;
					for (k=0; k<sent.words().size(); k++) {
						if (properName(sent.posTag(k), sent.nerTag(k))) {
							pos = sent.posTag(k);
							ner = sent.nerTag(k);
							label = labels[k];
							
							while (k<sent.words().size()) {
								if (properName(sent.posTag(k), sent.nerTag(k))) {
									word += sent.word(k) + "_";
									lemma = "_name_";
									if (sent.incomingDependencyLabel(k).isPresent()) deprel += sent.incomingDependencyLabel(k).get() + "_";
									else deprel += "O_";
									k++;
									
								} else if ((sent.posTag(k).equals("-LRB-") || sent.posTag(k).equals("``")) 
										&& ( (k+1<sent.words().size() && properName(sent.posTag(k+1), sent.nerTag(k+1))) 
												|| ((k+2<sent.words().size() && properName(sent.posTag(k+2), sent.nerTag(k+2))))
										   )) {
									word += sent.word(k) + "_";
									lemma = "_name_";
									if (sent.incomingDependencyLabel(k).isPresent()) deprel += sent.incomingDependencyLabel(k).get() + "_";
									else deprel += "O_";
									k++;
									lrb = true;
									
								} else if (lrb && (sent.posTag(k).equals("-RRB-") || sent.posTag(k).equals("''"))) {
									word += sent.word(k) + "_";
									lemma = "_name_";
									if (sent.incomingDependencyLabel(k).isPresent()) deprel += sent.incomingDependencyLabel(k).get() + "_";
									else deprel += "O_";
									k++;
									lrb = false;
									
								} else {
									break;
								}
							}
							outfile.println(generateLine(wikidataId, j+"", k+"", word.substring(0, word.length()-1), lemma, pos, ner, deprel.substring(0, deprel.length()-1), label));
							word = ""; lemma = ""; pos = ""; ner = ""; deprel = "";
							k--;
							
						} else {
							label = labels[k];
							dep = "O";
							if (sent.incomingDependencyLabel(k).isPresent()) {
								dep = sent.incomingDependencyLabel(k).get();
							}
							if (label.equals("NCHILD") && !dep.equals("nummod")) label = "O";
							if (label.equals("NCHILD") && getInteger(sent.lemma(k)) == 1) label = "O";
							outfile.println(generateLine(wikidataId, j+"", k+"", sent.word(k), getIntegerSimple(sent.lemma(k)), sent.posTag(k), sent.nerTag(k), dep, label));
						}
					}
					numSent ++;
					outfile.println();
				}
			}			
			
			outfile.close();
			line = br.readLine();
		}
		
		br.close();
		System.out.println(numSent);
		
	}
	
	public void generateSentencesFile(JSONArray arr, String filePath) throws JSONException, IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, false)));
		
		int numSent = 0;
		
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			int numChild = obj.getInt("num-child");
			JSONArray lines = obj.getJSONArray("article-num-only");
			String wikidataId = obj.getString("wikidata-id");
			String wikidataLbl = obj.getString("wikidata-label");
			
			for (int j=0; j<lines.length(); j++) {
				System.out.println(lines.getString(j));
				out.println(wikidataId + "\t" + j + "\t" + lines.getString(j));
				numSent ++;	
			}
		}
		System.out.println(numSent);
		out.close();
	}
	
	public void generatePersonsFile(JSONArray arr, String filePath) throws JSONException, IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, false)));
		
		int numSent = 0;
		
		for (int i=0; i<arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			
			int numChild = obj.getInt("num-child");
			String wikidataId = obj.getString("wikidata-id");
			String wikidataLbl = obj.getString("wikidata-label");
			out.println(wikidataId + "\t" + wikidataLbl + "\t" + numChild);
		}
		System.out.println(numSent);
		out.close();
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		
		String filepath = "./data/wikidata-count-cardinality.json";
		String randomFilepath = "./data/wikidata_persons_random1000.csv";
		
		FeatureExtractionPopular feat = new FeatureExtractionPopular();
		
		feat.generateColumnsFile(filepath, randomFilepath);
		
	}
	
}
