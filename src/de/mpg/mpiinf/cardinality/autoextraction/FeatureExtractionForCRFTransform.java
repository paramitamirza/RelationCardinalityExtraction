package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class FeatureExtractionForCRFTransform {
	
	
	
	private String inputCsvFile = "./data/example/wikidata_sample.csv";
	private String inputRandomCsvFile = "./data/example/wikidata_sample_random10.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	
	public FeatureExtractionForCRFTransform() {
		
	}
	
	public FeatureExtractionForCRFTransform(String inputCsvFilePath, String inputRandomCsvFilePath, String relationName, String dirOutput) {
		this();
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile(inputRandomCsvFilePath);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public static void main(String[] args) throws JSONException, IOException {
				
		FeatureExtractionForCRFTransform featExtraction;
		if (args.length < 4) {
			featExtraction = new FeatureExtractionForCRFTransform();
		} else {
			featExtraction = new FeatureExtractionForCRFTransform(args[0], args[1], args[2], args[3]);
		}
		
		featExtraction.generateColumnsFile(true, false, 0);
		
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep;
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep, String label) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep + "\t" + label;
	}
	
	public void generateColumnsFile(boolean nummod, boolean compositional, int threshold) throws JSONException, IOException {
		
		List<String> testInstances = readRandomInstances();
				
		String line;
		String wikidataId = "", label = "", count = "";
		long numInt;
		int numSent = 0;
		
		List<Integer> idxToAdd;
		long numToAdd;
		
		List<String> labels;
		List<String> tokenFeatures;
		int tokenIdx;
		
		BufferedReader br = new BufferedReader(new FileReader(this.getInputCsvFile()));
		line = br.readLine();
		int numOfTriples = -99;
		PrintWriter outfile;
		
		ensureDirectory(new File(dirFeature));
		File train = new File(dirFeature + relName + "_train_cardinality.data");
		File test = new File(dirFeature + relName + "_test_cardinality.data");
		Files.deleteIfExists(train.toPath());
		Files.deleteIfExists(test.toPath());
		
		Transform trans = new Transform();
		
		BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new GZIPOutputStream(new FileOutputStream(this.getInputCsvFile().replace(".csv", ".jsonl.gz")))
                    ));
		
		System.out.println("Generate feature file (in column format) for CRF++...");
		while (line != null) {
			
//	        System.out.println(line);
			wikidataId = line.split(",")[0];
	        label = line.split(",")[1];
	        count = line.split(",")[2];
	        System.out.println(wikidataId + "\t" + label + "\t" + count);
	        
	        numOfTriples = Integer.parseInt(count);
	        
	        SentenceExtractionFromWikipedia sentExtraction = new SentenceExtractionFromWikipedia(inputCsvFile, inputCsvFile.replace(".csv", ".jsonl.gz"));
	        String wikipediaText = sentExtraction.getWikipediaTextFromTitle(label);
	        
	        boolean training = true;
			if (testInstances.contains(wikidataId)) {
				outfile = new PrintWriter(new BufferedWriter(new FileWriter(dirFeature + relName + "_test_cardinality.data", true)));
				training = false;
			} else {
				outfile = new PrintWriter(new BufferedWriter(new FileWriter(dirFeature + relName + "_train_cardinality.data", true)));
			}
	        
	        if (wikipediaText != "") {
	        	
	        	String transformed;
	    		Sentence sent;
	    		
	    		JSONObject obj = new JSONObject();
				obj.put("id", wikidataId);
				obj.put("label", label);
				JSONArray list = new JSONArray();
				
				int j=0;
	    		
	    		for (String l : wikipediaText.split("\\r?\\n")) {	//Split the paragraphs
	    			Document doc = new Document(l);
	    			
	    			for (Sentence s : doc.sentences()) {	//Split the sentences
	    				
	    				transformed = trans.transform(s.text(), false, false, true, true);
	    				sent = new Sentence(transformed);
	    				
//	    				System.out.println(transformed);
	    				
	    				if (sentExtraction.containNumbers(transformed, sent, false, false)) {
	    					//filtered.add(sent.text());
	    					
	    					list.put(s.text());
	    					
	    					if (!training) 
	    						sent = new Sentence(trans.transform(transformed, true, true, false, false));
	    					
	    					String word = "", lemma = "", pos = "", ner = "", deprel = "";
							StringBuilder sb = new StringBuilder();
							int k;
							boolean lrb = false;
							
							idxToAdd = new ArrayList<Integer>();
							numToAdd = 0;
							
							labels = new ArrayList<String>();
							tokenFeatures = new ArrayList<String>();
							tokenIdx = 0;
							
							for (k=0; k<sent.words().size(); k++) {
								pos = sent.posTag(k);
								ner = sent.nerTag(k);
								deprel = "O";
								if (sent.incomingDependencyLabel(k).isPresent()) {
									deprel = sent.incomingDependencyLabel(k).get();
								}
								label = "O";
								
								if (sent.word(k).startsWith("LatinGreek_")) {
									System.err.println(sent.word(k));
									word = sent.word(k).split("_")[0] + "_" + sent.word(k).split("_")[1] + "_" + sent.word(k).split("_")[2];
									lemma = "_" + sent.word(k).split("_")[3] + "_";
									
									numInt = Long.parseLong(sent.word(k).split("_")[2]);
									
									if (compositional) {
										if (numToAdd > 0) {
											if (numInt == numOfTriples) {
												label = "_YES_";
												numToAdd = 0;
												idxToAdd.clear();
												
											} else {
												if ((numToAdd+numInt) == numOfTriples) {
													label = "_YES_";
													for (Integer nnn : idxToAdd) labels.set(nnn, "_YES_");
													numToAdd = 0;
													idxToAdd.clear();
												} else if ((numToAdd+numInt) < numOfTriples) {
													label = "O";
													numToAdd += numInt;
													idxToAdd.add(tokenIdx);
												} else {	//(numToAdd+numInt) > numOfTriples
													label = "O";
													numToAdd = 0;
													idxToAdd.clear();
												}
											}
											
										} else {
											if (numInt == numOfTriples) {
												label = "_YES_";
											} else if (numInt < numOfTriples) {
												label = "O";
												numToAdd += numInt;
												idxToAdd.add(tokenIdx);
											} else {	//numInt > numOfTriples
												label = "O";
											}
										}
										
									} else {
										if (numInt == numOfTriples) {
											label = "_YES_";
//											} else if (numInt < numOfTriples) {
//												label = "_NO_";
//											} else if (numInt > numOfTriples) {
//												label = "_MAYBE_";
										} else {
											label = "O";
										}
									}
									
//									sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
//									sb.append(System.getProperty("line.separator"));
									tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel));
									labels.add(label);
									tokenIdx ++;								
									
								} else if (Numbers.properNumber(pos, ner)) {						
									word = ""; lemma = ""; deprel = "";
									
									while (k<sent.words().size()) {
										if (Numbers.properNumber(sent.posTag(k), sent.nerTag(k))) {
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
									
									numInt = Numbers.getInteger(word.toLowerCase());
									if (numInt > 0) {
										lemma = "_num_";
										
										if (compositional) {
											if (numToAdd > 0) {
												if (numInt == numOfTriples
														&& ((nummod && deprel.startsWith("nummod"))
																|| !nummod)
														&& numOfTriples > threshold
														) {
													label = "_YES_";
													numToAdd = 0;
													idxToAdd.clear();
													
												} else {
													if ((numToAdd+numInt) == numOfTriples
															&& ((nummod && deprel.startsWith("nummod"))
																	|| !nummod)
															&& numOfTriples > threshold
															) {
														label = "_YES_";
														for (Integer nnn : idxToAdd) labels.set(nnn, "_YES_");
														numToAdd = 0;
														idxToAdd.clear();
													} else if ((numToAdd+numInt) < numOfTriples
															&& ((nummod && deprel.startsWith("nummod"))
																	|| !nummod)
															&& numOfTriples > threshold
															) {
														label = "O";
														numToAdd += numInt;
														idxToAdd.add(tokenIdx);
													} else {	//(numToAdd+numInt) > numOfTriples
														label = "O";
														numToAdd = 0;
														idxToAdd.clear();
													}
												}
												
											} else {
												if (numInt == numOfTriples
														&& ((nummod && deprel.startsWith("nummod"))
																|| !nummod)
														&& numOfTriples > threshold
														) {
													label = "_YES_";
												} else if (numInt < numOfTriples
														&& ((nummod && deprel.startsWith("nummod"))
																|| !nummod)
														&& numOfTriples > threshold
														) {
													label = "O";
													numToAdd += numInt;
													idxToAdd.add(tokenIdx);
												} else {	//numInt > numOfTriples
													label = "O";
												}
											}
											
										} else {
											if (numInt == numOfTriples
													&& ((nummod && deprel.startsWith("nummod"))
															|| !nummod)
													&& numOfTriples > threshold
													) {
												label = "_YES_";
//												} else if (numInt < numOfTriples) {
//													label = "_NO_";
//												} else if (numInt > numOfTriples) {
//													label = "_MAYBE_";
											} else {
												label = "O";
											}
										}
									}
									
//									sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
//									sb.append(System.getProperty("line.separator"));
									k--;
									tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel));
									labels.add(label);
									tokenIdx ++;
									
									word = ""; lemma = ""; deprel = "";
									
								} else if (Numbers.properName(pos, ner)) {
									word = ""; lemma = ""; deprel = "";
									
									while (k<sent.words().size()) {
										if (Numbers.properName(sent.posTag(k), sent.nerTag(k))) {
											word += sent.word(k) + "_";
											lemma = "_name_";
											if (sent.incomingDependencyLabel(k).isPresent()) deprel += sent.incomingDependencyLabel(k).get() + "_";
											else deprel += "O_";
											k++;
											
										} else if ((sent.posTag(k).equals("-LRB-") || sent.posTag(k).equals("``")) 
												&& ( (k+1<sent.words().size() && Numbers.properName(sent.posTag(k+1), sent.nerTag(k+1))) 
														|| ((k+2<sent.words().size() && Numbers.properName(sent.posTag(k+2), sent.nerTag(k+2))))
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
									
//										sb.append(generateLine(wikidataId, j+"", k+"", word.substring(0, word.length()-1), lemma, pos, ner, deprel.substring(0, deprel.length()-1), label));
//										sb.append(System.getProperty("line.separator"));
									k--;
									tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word.substring(0, word.length()-1), lemma, pos, ner, deprel.substring(0, deprel.length()-1)));
									labels.add(label);
									tokenIdx ++;
									
									word = ""; lemma = ""; deprel = "";
									
								} else {							
									word = sent.word(k);
									lemma = sent.lemma(k);
//										sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
//										sb.append(System.getProperty("line.separator"));
									tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel));
									labels.add(label);
									tokenIdx ++;
								}
							}
							
							for (int t=0; t<tokenFeatures.size(); t++) {
								sb.append(tokenFeatures.get(t) + "\t" + labels.get(t));
								sb.append(System.getProperty("line.separator"));
							}
							
							sb.append(System.getProperty("line.separator"));
							outfile.print(sb.toString());
							numSent ++;
	    				}
	    				
	    				j ++;
	    	        }
	    	    }
	    		
	    		obj.put("sentences", list);
//				json.write(obj.toString() + "\n");
				bw.write(obj.toString());
				bw.newLine();
	    		
	        }
			
			outfile.close();
			line = br.readLine();
		}
		
		br.close();
		bw.close();
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

	public String getInputCsvFile() {
		return inputCsvFile;
	}

	public void setInputCsvFile(String inputCsvFile) {
		this.inputCsvFile = inputCsvFile;
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
	
	public String getDirFeature() {
		return dirFeature;
	}

	public void setDirFeature(String dirFeature) {
		this.dirFeature = dirFeature;
	}
	
}
