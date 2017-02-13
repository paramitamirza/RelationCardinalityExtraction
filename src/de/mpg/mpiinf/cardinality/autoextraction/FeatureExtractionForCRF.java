package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.json.*;

import edu.stanford.nlp.simple.Sentence;

public class FeatureExtractionForCRF {
	
	public String[] digitsArr = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
	public String[] tensArr = {"", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
	public String[] ordinalsArr = {"", "fir", "seco", "thi", "four", "fif", "six", "seven", "eigh", "nin", "ten", 
			"eleven", "twelf", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twentie"};
	public String[] tenOrdinalsArr = {"", "ten", "twentie", "thirtie", "fortie", "fiftie", "sixtie", "seventie", "eightie", "ninetie"};
	
	public List<String> digits = Arrays.asList(digitsArr);
	public List<String> tens = Arrays.asList(tensArr);
	public List<String> ordinals = Arrays.asList(ordinalsArr);
	public List<String> tenOrdinals = Arrays.asList(tenOrdinalsArr);
	
	public Map<String, Integer> hundreds = new HashMap<String, Integer>();
	
	private String inputJsonFile = "./data/auto_extraction/wikidata_sample.jsonl.gz";
	private String inputRandomCsvFile = "./data/auto_extraction/wikidata_sample_random.csv";
	private String relName = "sample";
	private String dirFeature = "./data/auto_extraction/";
	
	public FeatureExtractionForCRF() {
		hundreds.put("hundred", 100);
		hundreds.put("thousand", 1000);
		hundreds.put("million", 1000000);
	}
	
	public FeatureExtractionForCRF(String inputJsonFilePath, String inputRandomCsvFilePath, String relationName) {
		this();
		this.setInputJsonFile(inputJsonFilePath);
		this.setInputRandomCsvFile(inputRandomCsvFilePath);
		this.setRelName(relationName);
	}
	
	public static void main(String[] args) throws JSONException, IOException {
				
		FeatureExtractionForCRF featExtraction;
		if (args.length < 3) {
			featExtraction = new FeatureExtractionForCRF();
		} else {
			featExtraction = new FeatureExtractionForCRF(args[0], args[1], args[2]);
		}
		
		featExtraction.generateColumnsFile();
		
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep, String label) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep + "\t" + label;
	}
	
	public void generateColumnsFile() throws JSONException, IOException {
		
		List<String> testInstances = readRandomInstances();
				
		String line, label;
		long numInt;
		int numSent = 0;
		
		BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(this.getInputJsonFile()))
                    ));
		line = br.readLine();
		JSONObject obj;
		JSONArray lines;
		String wikidataId, count;
		int numOfTriples = -99;
		PrintWriter outfile;
		
		ensureDirectory(new File(dirFeature));
		File train = new File(dirFeature + relName + "_train_cardinality.data");
		File test = new File(dirFeature + relName + "_test_cardinality.data");
		Files.deleteIfExists(train.toPath());
		Files.deleteIfExists(test.toPath());
		
		while (line != null) {
			obj = new JSONObject(line);
			
			count = obj.getString("count");
			lines = obj.getJSONArray("article");
			wikidataId = obj.getString("wikidata-id");
			System.out.println(wikidataId + "\t" + count);
			
			if (testInstances.contains(wikidataId)) {
				outfile = new PrintWriter(new BufferedWriter(new FileWriter(dirFeature + relName + "_test_cardinality.data", true)));
			} else {
				numOfTriples = Integer.parseInt(count);
				outfile = new PrintWriter(new BufferedWriter(new FileWriter(dirFeature + relName + "_train_cardinality.data", true)));
			}
			
			for (int j=0; j<lines.length(); j++) {
					
				Sentence sent = new Sentence(lines.getString(j));
				
				String word = "", lemma = "", pos = "", ner = "", deprel = "";
				String labelJoinStr = "";
				StringBuilder sb = new StringBuilder();
				int k;
				boolean lrb = false;
				for (k=0; k<sent.words().size(); k++) {
					pos = sent.posTag(k);
					ner = sent.nerTag(k);
					deprel = "O";
					if (sent.incomingDependencyLabel(k).isPresent()) {
						deprel = sent.incomingDependencyLabel(k).get();
					}
					label = "O";
					
					if (properNumber(pos, ner)) {						
						word = ""; lemma = ""; deprel = "";
						
						while (k<sent.words().size()) {
							if (properNumber(sent.posTag(k), sent.nerTag(k))) {
								word += sent.word(k) + "_";
								lemma += sent.lemma(k) + "_";
								if (sent.incomingDependencyLabel(k).isPresent()) deprel = sent.incomingDependencyLabel(k).get();
								else deprel = "O";
								if (sent.governor(k).isPresent() && !deprel.equals("root")) {
									deprel += "_" + sent.lemma(sent.governor(k).get());
								}
								k++;
								
							} else {
								break;
							}
						}
						word = word.substring(0, word.length()-1);
						lemma = lemma.substring(0, lemma.length()-1);
						
						numInt = getInteger(word.toLowerCase());
						if (numInt > 0) {
							lemma = "_num_";
							if (numInt == numOfTriples) {
								label = "_YES_";
//							} else if (numInt < numOfTriples) {
//								label = "_NO_";
//							} else if (numInt > numOfTriples) {
//								label = "_MAYBE_";
							} else {
								label = "O";
							}
						}
						
						labelJoinStr += label;
						sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
						sb.append(System.getProperty("line.separator"));
						
						word = ""; lemma = ""; deprel = "";
						k--;
						
					} else if (properName(pos, ner)) {
						word = ""; lemma = ""; deprel = "";
						
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
						labelJoinStr += label;
						sb.append(generateLine(wikidataId, j+"", k+"", word.substring(0, word.length()-1), lemma, pos, ner, deprel.substring(0, deprel.length()-1), label));
						sb.append(System.getProperty("line.separator"));
						
						word = ""; lemma = ""; deprel = "";
						k--;
						
					} else {							
						word = sent.word(k);
						lemma = sent.lemma(k);
						sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
						sb.append(System.getProperty("line.separator"));							
					}
				}
				
				if (testInstances.contains(wikidataId)) {
					sb.append(System.getProperty("line.separator"));
					outfile.print(sb.toString());
					numSent ++;
					
				} else {
//					if (labelJoinStr.contains("_YES_") || labelJoinStr.contains("_NO_")
//							|| labelJoinStr.contains("_MAYBE_")
//							) {
						sb.append(System.getProperty("line.separator"));
						outfile.print(sb.toString());
						numSent ++;
//					}
				}		
			}
			
			outfile.close();
			line = br.readLine();
		}
		
		br.close();
		System.out.println(numSent);
		
	}
	
	public void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public List<String> readRandomInstances() throws IOException {
		System.out.println("Read random instances...");
		List<String> randomInstances = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(this.getInputRandomCsvFile()));
		String line = br.readLine();		
		while (line != null) {
			randomInstances.add(line.split(",")[0]);
			line = br.readLine();
		}
		br.close();
		return randomInstances;
	}

	public JSONArray readJSONArray(String filepath) throws IOException, JSONException {
		JSONArray arr = new JSONArray();
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		JSONObject obj;
		try {
		    String line = br.readLine();

		    while (line != null) {
		    	obj = new JSONObject(line);
		    	arr.put(obj);
		        line = br.readLine();
		    }
		} finally {
		    br.close();
		}
		return arr;
	}
	
	public Long getInteger(String numStr) {
		long number = -999; 
		if (numStr.contains(",")) numStr = numStr.replace(",", "");
		if (numStr.contains("-")) numStr = numStr.replace("-", "_");
		String[] words = numStr.split("_");
		
		if (words.length == 4) {
			if (digits.contains(words[0]) && hundreds.containsKey(words[1])
					&& tens.contains(words[2]) && digits.contains(words[3])) {
				number = (digits.indexOf(words[0]) * hundreds.get(words[1])) + (tens.indexOf(words[2]) * 10) + digits.indexOf(words[3]);
			}
		} else if (words.length == 3) {
			if (hundreds.containsKey(words[0])
					&& tens.contains(words[1]) && digits.contains(words[2])) {
				number = (1 * hundreds.get(words[0])) + (tens.indexOf(words[1]) * 10) + digits.indexOf(words[2]);
			}
		} else if (words.length == 2) {
			if (tens.contains(words[0]) && digits.contains(words[1])) {
				number = (tens.indexOf(words[0]) * 10) + digits.indexOf(words[1]);
			} else if (tens.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = (tens.indexOf(words[0]) * 10) * hundreds.get(words[1]);
			} else if (digits.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = digits.indexOf(words[0]) * hundreds.get(words[1]);
			} else if (words[0].matches("^-?\\d+\\.?\\d*$") && hundreds.containsKey(words[1])) {
				number = new Float(Float.parseFloat(words[0]) * hundreds.get(words[1])).longValue();
			} else if (tens.contains(words[0]) && ordinals.contains(words[1])) {
				number = (tens.indexOf(words[0]) * 10) + ordinals.indexOf(words[1]);
			}
		} else {
			if (tens.contains(numStr)) number = tens.indexOf(numStr) * 10;
			else if (digits.contains(numStr)) number = digits.indexOf(numStr);
			else if (numStr.matches("^-?\\d+\\.?\\d*$")) number = new Float(Float.parseFloat(numStr)).longValue();
			else if (numStr.length() > 2 && ordinals.contains(numStr.substring(0, numStr.length()-2))) number = ordinals.indexOf(numStr);
			else if (numStr.length() > 2 && tenOrdinals.contains(numStr.substring(0, numStr.length()-2))) number = tenOrdinals.indexOf(numStr) * 10;
		}
		
		return number;
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
	
	public boolean properOrdinal(String pos, String ner) {
		if (pos.equals("JJ")
				&& ner.equals("ORDINAL")) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean properNumberAndOrdinal(String pos, String ner) {
		if (pos.equals("CD")
				&& !ner.equals("MONEY")
				&& !ner.equals("PERCENT")
				&& !ner.equals("DATE")
				&& !ner.equals("TIME")
				&& !ner.equals("DURATION")
				&& !ner.equals("SET")
				) {
			return true;
		} else if (pos.equals("JJ")
				&& ner.equals("ORDINAL")) {
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

	public String getInputJsonFile() {
		return inputJsonFile;
	}

	public void setInputJsonFile(String inputJsonFile) {
		this.inputJsonFile = inputJsonFile;
	}

	public String getInputRandomCsvFile() {
		return inputRandomCsvFile;
	}

	public void setInputRandomCsvFile(String inputRandomCsvFile) {
		this.inputRandomCsvFile = inputRandomCsvFile;
	}
	
	public String getRelName() {
		return relName;
	}

	public void setRelName(String relationName) {
		this.relName = relationName;
	}
	
}
