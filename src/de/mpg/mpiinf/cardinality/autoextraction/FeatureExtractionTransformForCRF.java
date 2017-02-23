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
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.json.*;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;

public class FeatureExtractionTransformForCRF {
	
	
	
	private String inputJsonFile = "./data/example/wikidata_sample.jsonl.gz";
	private String inputRandomCsvFile = "./data/example/wikidata_sample_random10.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	
	public FeatureExtractionTransformForCRF() {
		
	}
	
	public FeatureExtractionTransformForCRF(String inputJsonFilePath, String inputRandomCsvFilePath, String relationName, String dirOutput) {
		this();
		this.setInputJsonFile(inputJsonFilePath);
		this.setInputRandomCsvFile(inputRandomCsvFilePath);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public static void main(String[] args) throws JSONException, IOException {
				
		FeatureExtractionTransformForCRF featExtraction;
		if (args.length < 4) {
			featExtraction = new FeatureExtractionTransformForCRF();
		} else {
			featExtraction = new FeatureExtractionTransformForCRF(args[0], args[1], args[2], args[3]);
		}
		
		featExtraction.generateColumnsFile();
		
		////test!!!
//		featExtraction.transformNegative("John doesn't have any children.");
//		featExtraction.transformNegative("John didn't bring young friends yesterday.");
//		featExtraction.transformNegative("John had no children surviving adulthood.");
//		featExtraction.transformNegative("John has never been married.");
//		featExtraction.transformNegative("Their marriage is without children.");
//		featExtraction.transformNegative("John has never had his name revealed, although we can see it.");
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep, String label) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep + "\t" + label;
	}
	
	public Tree getLastVerbPhrase(Tree t) {
		boolean found = false;
		Tree more = null;
		for (Tree sub : t) {
			if (sub.nodeString().startsWith("VP ")) {
				found = true; 
				more = sub;
			} else if (sub.nodeString().startsWith(", ")) {
				break;
			}
		}
		if (found) return more;
		else return t;
	}
	
	public String transformNegative(String sentence) {
		String transformed = "";
		
		//// e.g., Their marriage is without children --> Their marriage is with 0 children
		if (sentence.contains("without")) return sentence.replaceAll("without", "with 0");
		
		int negFound = -999;
		Sentence sent = new Sentence(sentence);
		for (int i=0; i<sent.words().size(); i++) {
			if (sent.incomingDependencyLabel(i).isPresent()
					&& sent.incomingDependencyLabel(i).get().equals("neg")) {
				negFound = i;
				break;
			}
		}
		
		int nodeNum = -999, negNodeIdx = -999, negNextIdx = -999;
		if (negFound >= 0) {
			Tree root = sent.parse();
			nodeNum = root.getLeaves().get(negFound).nodeNumber(root);
		    
		    if (nodeNum >= 0) {
			    Tree negNode = root.getNodeNumber(nodeNum).parent(root);
			    
			    if (negNode.nodeString().startsWith("RB ")) { //not, n't or never
			    	
			    	Tree negParent = negNode.parent(root);
			    	if (negParent.nodeString().startsWith("VP ")) {
			    		//// e.g., John doesn't have any children --> John does have 0 children
			    		
				    	for (int k=0; k<negParent.children().length; k++) {
				    		if (negParent.children()[k] == negNode) {
				    			negNodeIdx = k;
				    			if (k+1 < negParent.children().length 
				    					&& negParent.children()[k+1].nodeString().startsWith("VP ")) 
				    				negNextIdx = k+1;
				    		}
				    	}
				    		
			    		if (negNodeIdx >= 0 && negNextIdx >= 0) {
			    			for (Tree t : negParent.children()[negNextIdx]) {
			    				if (t.nodeString().startsWith("NP ")) {
			    					if (t.children()[0].nodeString().startsWith("DT")) {
			    						////John doesn't have any children
			    						t.insertDtr(Tree.valueOf("(CD 0)"), 0);
			    						t.removeChild(1);
			    						
			    					} else {
			    						////John doesn't have stupid children
			    						t.insertDtr(Tree.valueOf("(CD 0)"), 0);
			    					}
			    					negParent.removeChild(negNodeIdx);
			    					break;
			    				}
			    			}
		    			}
				    	
			    	} else if (negParent.nodeString().startsWith("ADVP ")) {
			    		//// e.g., John has never been married --> John has been married 0 times
			    		negNode = negParent;
			    		negParent = negParent.parent(root);

				    	for (int k=0; k<negParent.children().length; k++) {
				    		if (negParent.children()[k] == negNode) {
				    			negNodeIdx = k;
				    			if (k+1 < negParent.children().length 
				    					&& negParent.children()[k+1].nodeString().startsWith("VP ")) 
				    				negNextIdx = k+1;
				    		}
				    	}
				    		
			    		if (negNodeIdx >= 0 && negNextIdx >= 0) {			    			
			    			Tree vp = getLastVerbPhrase(negParent.children()[negNextIdx]);
			    			negNextIdx = -999;
			    			for (int s=0; s<vp.children().length; s++) {
			    				if (vp.getChild(s).nodeString().startsWith("VB")) {
			    					negNextIdx = s;
			    					break;
			    				}
			    			}
			    			if (negNextIdx >= 0 && negNextIdx <= vp.children().length) {
			    				vp.insertDtr(Tree.valueOf("(NP-TMP (CD 0) (NNS times))"), negNextIdx+1);
			    				negParent.removeChild(negNodeIdx);
			    			}
		    			}
			    	}				    
				    
			    } else if (negNode.nodeString().startsWith("DT ")) {
			    	//// e.g., John had no children --> John had 0 children
			    	Tree negParent = negNode.parent(root);
			    	nodeNum = negParent.nodeNumber(root);
			    	if (negParent.nodeString().startsWith("NP ")) {
			    		
			    		for (int k=0; k<negParent.children().length; k++) {
				    		if (negParent.children()[k] == negNode) {
				    			negNodeIdx = k;
				    		}
			    		}
			    		if (negNodeIdx >= 0) {
				    		negParent.insertDtr(Tree.valueOf("(CD 0)"), negNodeIdx);
				    		negParent.removeChild(negNodeIdx+1);
			    		}
			    	}
			    }
		    }
		    
		    transformed = StringUtils.join(root.getLeaves(), " ");
		    System.err.println(sentence + " --> " + transformed);
		
		} else {
			transformed = sentence;
		}
		
		return transformed;
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
		
		System.out.println("Generate feature file (in column format) for CRF++...");
		while (line != null) {
			obj = new JSONObject(line);
			
			count = obj.getString("count");
			lines = obj.getJSONArray("article");
			wikidataId = obj.getString("wikidata-id");
			System.out.println(wikidataId + "\t" + count);
			
			numOfTriples = Integer.parseInt(count);
			
			if (testInstances.contains(wikidataId)) {
				outfile = new PrintWriter(new BufferedWriter(new FileWriter(dirFeature + relName + "_test_cardinality.data", true)));
			} else {
				outfile = new PrintWriter(new BufferedWriter(new FileWriter(dirFeature + relName + "_train_cardinality.data", true)));
			}
			
			for (int j=0; j<lines.length(); j++) {
				
				Sentence sent = new Sentence(transformNegative(lines.getString(j)));
				
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
					
					if (Numbers.properNumber(pos, ner)) {						
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
							if (numInt == numOfTriples
									&& deprel.startsWith("nummod")
									) {
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
	
	public String getDirFeature() {
		return dirFeature;
	}

	public void setDirFeature(String dirFeature) {
		this.dirFeature = dirFeature;
	}
	
}
