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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class FeatureExtractionOthers {
	
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
	public FeatureExtractionOthers() {
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
			} else if (words[0].matches("^-?\\d+$") && hundreds.containsKey(words[1])) {
				number = new Float(Float.parseFloat(words[0]) * hundreds.get(words[1])).longValue();
			}
		} else {
			String[] ords = numStr.split("-");
			if (ords.length > 1) {
				if (tens.contains(ords[0]) && ordinals.contains(ords[1])) 
					number = (tens.indexOf(ords[0]) * 10) + ordinals.indexOf(ords[1]);
				else if (tens.contains(ords[0]) && digits.contains(ords[1])) 
					number = (tens.indexOf(ords[0]) * 10) + digits.indexOf(ords[1]);
			} else {
				if (tens.contains(numStr)) number = tens.indexOf(numStr) * 10;
				else if (digits.contains(numStr)) number = digits.indexOf(numStr);
				else if (numStr.matches("^-?\\d+$")) number = new Float(Float.parseFloat(numStr)).longValue();
				else if (numStr.length() > 2 && ordinals.contains(numStr.substring(0, numStr.length()-2))) number = ordinals.indexOf(numStr);
				else if (numStr.length() > 2 && tenOrdinals.contains(numStr.substring(0, numStr.length()-2))) number = tenOrdinals.indexOf(numStr) * 10;
			}
		}
		
		return number;
	}
	
	public String getIntegerSimple(String numStr) {
		if (tens.contains(numStr)) return "_ten_";
		else if (digits.contains(numStr)) return "_num_";
		else if (numStr.matches("^-?\\d+$")) return "_num_";
		else if (hundreds.containsKey(numStr)) return "_hundred_";
		else if (numStr.replace("st", "").matches("^-?\\d+$") 
				|| numStr.replace("nd", "").matches("^-?\\d+$")
				|| numStr.replace("rd", "").matches("^-?\\d+$")
				|| numStr.replace("th", "").matches("^-?\\d+$"))
			return "_ord_";
		else if (ordinals.contains(numStr.replace("st", ""))
				|| ordinals.contains(numStr.replace("nd", ""))
				|| ordinals.contains(numStr.replace("rd", ""))
				|| ordinals.contains(numStr.replace("th", ""))) 
			return "_ord_";
		else if (numStr.split("-").length > 1) {
			if (digits.contains(numStr.split("-")[1])) return "_num_";
			else if (ordinals.contains(numStr.split("-")[1])) return "_ord_";
			else if (tenOrdinals.contains(numStr.split("-")[1])) return "_ord_";
		}
		
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
			} else if (words[0].matches("^-?\\d+$") && hundreds.containsKey(words[1])) {
				number = new Float(Float.parseFloat(words[0]) * hundreds.get(words[1])).longValue();
			}
		} else {
			if (tens.contains(words[0])) number = tens.indexOf(words[0]) * 10;
			else if (digits.contains(words[0])) number = digits.indexOf(words[0]);
			else if (words[0].matches("^-?\\d+$")) number = new Float(Float.parseFloat(words[0])).longValue();
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
	
	private boolean containProperNumberPerson(String sentence) throws IOException {
		Sentence sent = new Sentence(sentence);
		boolean personFound = false, numberFound = false;
		for (int i=0; i<sent.words().size(); i++) {
//			System.out.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
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
	
	private boolean containProperNumber(String sentence, boolean ordinal) throws IOException {
		Sentence sent = new Sentence(sentence);
		boolean numberFound = false;
//		if (containNumber(sent.text())) {			
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
				} else if (ordinal && sent.posTag(i).equals("JJ")
						&& sent.nerTag(i).equals("ORDINAL")) {
					numberFound = true;
					break;
				}
			}
//		}
		return numberFound;
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep, String label) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep + "\t" + label;
	}
	
	public void generateColumnsFile(String filepath, String randomFilepath, String topic, boolean ordinal, boolean composition) throws JSONException, IOException {
//		PrintWriter trainCount = new PrintWriter(new BufferedWriter(new FileWriter("./data/train_cardinality_count.txt", false)));
//		PrintWriter trainNum = new PrintWriter(new BufferedWriter(new FileWriter("./data/train_cardinality_num.txt", false)));
//		PrintWriter test = new PrintWriter(new BufferedWriter(new FileWriter("./data/random_test_cardinality.txt", false)));
		
		System.out.println("Read random filepath...");
		List<String> randomSeries = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(randomFilepath));
		String line = br.readLine();		
		while (line != null) {
			randomSeries.add(line.split(",")[0]);
			line = br.readLine();
		}
		br.close();
		System.out.println(randomSeries);
				
		String label, dep;
		long numInt;
		long numToAdd;
		List<Integer> idxToAdd;
		int numSent = 0;
		
		br = new BufferedReader(new FileReader(filepath));
		line = br.readLine();
		JSONObject obj;
		JSONArray lines;
		String wikidataId, numChild, countSeries;
		int numOfSeries = -99;
		PrintWriter outfile;
		
//		File test = new File("./data/random_test_cardinality.txt");
//		File trainNum = new File("./data/train_cardinality_num.txt");
//		File trainCount = new File("./data/train_cardinality_count.txt");
//		Files.deleteIfExists(trainCount.toPath());
//		Files.deleteIfExists(trainNum.toPath());
//		Files.deleteIfExists(test.toPath());
		
		boolean start = false;
		start = true;
		
		while (line != null) {
			randomSeries.add(line.split(",")[0]);
			obj = new JSONObject(line);
			
			countSeries = obj.getString("count-"+topic);
			lines = obj.getJSONArray("article-num-only");
			wikidataId = obj.getString("wikidata-id");
			System.out.println(wikidataId + "\t" + countSeries);
			
//			if (wikidataId.equals("Q3576629")) start = true;
			
			if (randomSeries.contains(wikidataId)) {
				outfile = new PrintWriter(new BufferedWriter(new FileWriter("./data/"+topic+"_test_cardinality.txt", true)));
			} else {
				numOfSeries = Integer.parseInt(countSeries);
				outfile = new PrintWriter(new BufferedWriter(new FileWriter("./data/"+topic+"_train_cardinality.txt", true)));
			}
			
			if (start) {
				for (int j=0; j<lines.length(); j++) {
					if (containProperNumber(lines.getString(j), ordinal)) {
						
//						System.out.println(lines.getString(j));
						Sentence sent = new Sentence(lines.getString(j));
						
						String[] labels = new String[sent.words().size()];
						
						idxToAdd = new ArrayList<Integer>();
						numToAdd = 0;
						
						for (int k=0; k<sent.words().size(); k++) {
							labels[k] = "O";
							
							if (composition) {
								if (ordinal && properOrdinal(sent.posTag(k), sent.nerTag(k))) {
									numInt = getInteger(sent.lemma(k));
									if (numInt > 0 && numInt == numOfSeries) {
										labels[k] = "YES-ORD";
									} else if (numInt > 0 && numInt < numOfSeries) {
										labels[k] = "NO-ORD";
									} else if (numInt > 0 && numInt > numOfSeries) {
										labels[k] = "MAYBE-ORD";
									} 
									
								} else if (properNumber(sent.posTag(k), sent.nerTag(k))) {
									numInt = getInteger(sent.lemma(k));
									
									if (k+1 < sent.words().size()) {
										if (properNumber(sent.posTag(k+1), sent.nerTag(k+1))) {
											numInt = getInteger(sent.lemma(k)+" "+sent.lemma(k+1));
											
											if (numToAdd > 0) {
												if (numInt > 0 && (numToAdd+numInt) == numOfSeries) {
													labels[k] = "YES";
													labels[k+1] = "YES";
													k++;
													for (Integer nnn : idxToAdd) labels[nnn] = "YES";
													numToAdd = 0;
													idxToAdd.clear();
													
												} else if (numInt > 0 && (numToAdd+numInt) < numOfSeries) {
													labels[k] = "NO";
													labels[k+1] = "NO";
													k++;
													numToAdd += numInt;
													idxToAdd.add(k); idxToAdd.add(k+1);
													
												}
											} else {
												if (numInt > 0 && numInt == numOfSeries) {
													labels[k] = "YES";
													labels[k+1] = "YES";
													k++;			
													
												} else if (numInt > 0 && numInt < numOfSeries) {
													labels[k] = "NO";
													labels[k+1] = "NO";
													k++;
													numToAdd += numInt;
													idxToAdd.add(k); idxToAdd.add(k+1);
													
												} else if (numInt > 0 && numInt > numOfSeries) {
													labels[k] = "MAYBE";
													labels[k+1] = "MAYBE";
													k++;
												}
											}
											
										} else {								
											if (numToAdd > 0) {
												if (numInt > 0 && (numToAdd+numInt) == numOfSeries) {
													labels[k] = "YES";
													for (Integer nnn : idxToAdd) labels[nnn] = "YES";
													numToAdd = 0;
													idxToAdd.clear();
													
												} else if (numInt > 0 && (numToAdd+numInt) < numOfSeries) {
													labels[k] = "NO";
													numToAdd += numInt;
													idxToAdd.add(k);
													
												} 
											} else {
												if (numInt > 0 && numInt == numOfSeries) {
													labels[k] = "YES";
													
												} else if (numInt > 0 && numInt < numOfSeries) {
													labels[k] = "NO";
													numToAdd += numInt;
													idxToAdd.add(k);
													
												} else if (numInt > 0 && numInt > numOfSeries) {
													labels[k] = "MAYBE";
												}
											}
										}
									}
								}
							} else {
								if (ordinal && properOrdinal(sent.posTag(k), sent.nerTag(k))) {
									numInt = getInteger(sent.lemma(k));
									
									if (k+1 < sent.words().size()) {
										if (!properNumber(sent.posTag(k+1), sent.nerTag(k+1))) {
											if (numInt > 0 && numInt == numOfSeries) {
												labels[k] = "YES-ORD";
											} else if (numInt > 0 && numInt < numOfSeries) {
												labels[k] = "NO-ORD";
											} else if (numInt > 0 && numInt > numOfSeries) {
												labels[k] = "MAYBE-ORD";
											} 
										}
									}
									
								} else if (properNumber(sent.posTag(k), sent.nerTag(k))) {
									numInt = getInteger(sent.lemma(k));
									
									if (k+1 < sent.words().size()) {
										if (properNumber(sent.posTag(k+1), sent.nerTag(k+1))) {
											numInt = getInteger(sent.lemma(k)+" "+sent.lemma(k+1));
											
											if (numInt > 0 && numInt == numOfSeries) {
												labels[k] = "YES";
												labels[k+1] = "YES";
												k++;
											} else if (numInt > 0 && numInt < numOfSeries) {
												labels[k] = "NO";
												labels[k+1] = "NO";
												k++;
											} else if (numInt > 0 && numInt > numOfSeries) {
												labels[k] = "MAYBE";
												labels[k+1] = "MAYBE";
												k++;
											} 
											
										} else {
											if (numInt > 0 && numInt == numOfSeries) {
												labels[k] = "YES";
											} else if (numInt > 0 && numInt < numOfSeries) {
												labels[k] = "NO";
											} else if (numInt > 0 && numInt > numOfSeries) {
												labels[k] = "MAYBE";
											} 
										}
									}
								}
							}
						}
						
						String word = "", lemma = "", pos = "", ner = "", deprel = "";
						boolean lrb = false;
						int k;
						StringBuilder sb = new StringBuilder();
						String labelJoinStr = "";
						for (k=0; k<sent.words().size(); k++) {
							pos = sent.posTag(k);
							ner = sent.nerTag(k);
							label = labels[k];
							dep = "O";
							if (sent.incomingDependencyLabel(k).isPresent()) {
								dep = sent.incomingDependencyLabel(k).get();
							}
							
							if (properName(pos, ner)) {
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
								
							} else if (properNumberAndOrdinal(pos, ner)) {							
								word = sent.word(k);
								lemma = getIntegerSimple(sent.lemma(k));
//								System.out.println(word + "\t" + lemma + "\t" + label);
								if (sent.governor(k).isPresent() && !dep.equals("root")) {
									dep += "_" + sent.lemma(sent.governor(k).get());
								}
								
//								if ((label.startsWith("YES") || label.startsWith("MAYBE")) 
//										&& !dep.equals("nummod") && !dep.equals("compound") && !dep.equals("amod")) label = "O";
//								if (!numChild.equals("null") && label.equals("MAYBE")) label = "NO";
//								if (!numChild.equals("null") && label.equals("MAYBE-ORD")) label = "NO-ORD";
//								if (!countChild.equals("null") && label.startsWith("MAYBE")) label = "O";
								
								labelJoinStr += label;
								sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dep, label));
								sb.append(System.getProperty("line.separator"));
							
							} else {							
								word = sent.word(k);
								lemma = sent.lemma(k);
								sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dep, label));
								sb.append(System.getProperty("line.separator"));							
							}
						}
						
						if (randomSeries.contains(wikidataId)) {
							sb.append(System.getProperty("line.separator"));
							outfile.print(sb.toString());
							numSent ++;
							
						} else {
							if (labelJoinStr.contains("YES") || labelJoinStr.contains("NO")
									|| labelJoinStr.contains("MAYBE")
									) {
								sb.append(System.getProperty("line.separator"));
								outfile.print(sb.toString());
								numSent ++;
							}
						}
					}				
				}
			}
			
			outfile.close();
			line = br.readLine();
		}
		
		br.close();
		System.out.println(numSent);
		
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		
//		String filepath = "./data/wikidata_series_cardinality.json";
//		String randomFilepath = "./data/wikidata_series_random20.csv";
//		String topic = "series";
		
		String filepath = args[0];
		String randomFilepath = args[1];
		String topic = args[2];
				
		FeatureExtractionOthers feat = new FeatureExtractionOthers();
		
		feat.generateColumnsFile(filepath, randomFilepath, topic, false, false);
		
	}
	
}
