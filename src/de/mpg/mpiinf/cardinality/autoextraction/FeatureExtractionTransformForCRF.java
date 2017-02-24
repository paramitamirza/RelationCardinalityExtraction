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
//		featExtraction.transformNegative("John doesn't have any ugly children.");
//		featExtraction.transformNegative("John didn't bring crazy young friends yesterday.");
//		featExtraction.transformNegative("John hasn't had children.");
//		featExtraction.transformNegative("John had no children surviving adulthood.");
//		featExtraction.transformNegative("John has never been married.");
//		featExtraction.transformNegative("John has never seen any children.");
//		featExtraction.transformNegative("John has never been married his partner.");
//		featExtraction.transformNegative("Their marriage is without children.");
//		featExtraction.transformNegative("John has never had young children revealed, although we know it.");
//		featExtraction.transformNegative("John has never had any children revealed, although we know it.");
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
	
	private int getDetAny(Sentence sent, int objIdx) {
		if (objIdx > 0) {
			for (int i=objIdx-1; i>=0; i--) {
				if (sent.governor(i).isPresent()
						&& sent.governor(i).get() == objIdx
						&& sent.incomingDependencyLabel(i).isPresent()
						&& sent.incomingDependencyLabel(i).get().equals("det")) {
					if (sent.word(i).toLowerCase().equals("any"))
						return i;
					else 
						return -99;
				}
			}
		}
		return -999;
	}
	
	private int getNounModifier(Sentence sent, int objIdx, int nounIdx) {
		if (objIdx > 0) {
			int verbAcl = getVerbAclRelcl(sent, objIdx);
			if (verbAcl > 0) {	//...have never had the children born
				return verbAcl;
			} else {			//...have never seen young clever children
				for (int i=objIdx-1; i>=0; i--) {
					if (sent.governor(i).isPresent()
							&& (sent.governor(i).get() == objIdx
								|| sent.governor(i).get() == nounIdx)
							&& sent.incomingDependencyLabel(i).isPresent()
							&& sent.incomingDependencyLabel(i).get().equals("amod")) {
						return getNounModifier(sent, i, nounIdx);
					} else {
						return objIdx;
					}
				}
				return objIdx;
			}
		} else {
			return -999;
		}
	}
	
	private int getVerbAclRelcl(Sentence sent, int objIdx) {
		if (objIdx > 0) {
			if (sent.governor(objIdx).isPresent()
					&& sent.posTag(sent.governor(objIdx).get()).startsWith("VB")
					&& sent.incomingDependencyLabel(objIdx).isPresent()
					&& sent.incomingDependencyLabel(objIdx).get().equals("nsubj")) {
				return sent.governor(objIdx).get();
			} else {
				for (int i=objIdx+1; i<sent.words().size(); i++) {
					if (sent.governor(i).isPresent()
							&& sent.governor(i).get() == objIdx
							&& sent.incomingDependencyLabel(i).isPresent()
							&& sent.incomingDependencyLabel(i).get().startsWith("acl")) {
						return i;
					} else {
						return -999;
					}
				}
				return -999;
			}
		} else {
			return -999;
		}
	}
	
	private int getNounSubj(Sentence sent, int verbIdx) {
		if (verbIdx > 0) {
			for (int i=verbIdx-1; i>=0; i--) {
				if (sent.governor(i).isPresent()
						&& sent.governor(i).get() == verbIdx
						&& sent.incomingDependencyLabel(i).isPresent()
						&& sent.incomingDependencyLabel(i).get().startsWith("nsubj")) {
					return i;
				} else {
					return -999;
				}
			}
			return -999;
		} else {
			return -999;
		}
	}
	
	public String transformNegative(String sentence) {
		String transformed = "", original = "";
		
		//// e.g., Their marriage is without children --> Their marriage is with 0 children
		if (sentence.contains("without")) return sentence.replaceAll("without", "with 0");
		
		List<Integer> negFoundList = new ArrayList<Integer>();
		Sentence orig = new Sentence(sentence);
		original = StringUtils.join(orig.words(), " ");
		for (int i=0; i<orig.words().size(); i++) {
			if (orig.incomingDependencyLabel(i).isPresent()
					&& orig.incomingDependencyLabel(i).get().equals("neg")) {
				negFoundList.add(i);
				break;
			}
		}
		
		transformed = original;
		Sentence sent;
		List<String> wordList = new ArrayList<String>();
		
		int gov = -999, det = -999, noun = -999, verbAcl = -999;
		boolean objExist = false, compExist = false;
		
		for (int negFound : negFoundList) {
			
			sent = new Sentence(transformed);
			gov = sent.governor(negFound).get();
			wordList.clear();
			wordList.addAll(sent.words());
			
			if (sent.posTag(gov).startsWith("V")) {	//if gov is a verb, let's look for the object!
				if (sent.word(negFound).equals("never")) {
					for (int k=gov+1; k<sent.words().size(); k++) {
						
						if (sent.governor(k).isPresent()
								&& sent.governor(k).get() == gov
								&& sent.incomingDependencyLabel(k).isPresent()) {
							
							if (sent.incomingDependencyLabel(k).get().equals("dobj")
									&& (sent.posTag(k).equals("NN") || sent.posTag(k).equals("NNS"))) {
								objExist = true;
								det = getDetAny(sent, k);
								noun = getNounModifier(sent, k, k);
								
								if (det != -999) {			//...have never seen any children
									if (det > 0) {
										wordList.set(det, "0");
										wordList.remove(negFound);
									}
									break;
								} else if (noun > 0) {		//...never saw crazy children
									wordList.add(noun+1, "0 times");
									wordList.remove(negFound);
									break;
								} else if (verbAcl > 0) {	//...have never had any children born
									wordList.add(verbAcl+1, "0 times");
									wordList.remove(negFound);
									break;
								}				
								
							} else if (sent.incomingDependencyLabel(k).get().equals("ccomp")
									&& sent.posTag(k).startsWith("VB")) {	
								compExist = true;
								noun = getNounSubj(sent, k);
								det = getDetAny(sent, noun);
								
								if (det != -999) {	//...have never had any children revealed
									if (det > 0) {
										wordList.set(det, "0");
										wordList.remove(negFound);
									}
									break;
								} else {			//...have never had children revealed
									wordList.add(k+1, "0 times");
									wordList.remove(negFound);
									break;
								}
								
								
							}
							
						}
					}
					
					if (!objExist && !compExist) {	//...have never been married
						wordList.add(gov+1, "0 times");
						wordList.remove(negFound);
					}
					
				} else {	//not!
					for (int k=gov+1; k<sent.words().size(); k++) {
						if (sent.governor(k).isPresent()
								&& sent.governor(k).get() == gov
								&& sent.incomingDependencyLabel(k).isPresent()
								&& sent.incomingDependencyLabel(k).get().equals("dobj")
								&& (sent.posTag(k).equals("NN") || sent.posTag(k).equals("NNS"))) {
							objExist = true;
							det = getDetAny(sent, k);
							noun = getNounModifier(sent, k, k);
							if (det != -999) {				//...not have any children
								if (det > 0) {
									wordList.set(det, "0");
									wordList.remove(negFound);
								}
								break;
							} else if (noun > 0) {			//...have young bright children
								wordList.add(noun, "0");
								wordList.remove(negFound);
								break;
							}
						}
					}
				}
			} else if (sent.posTag(gov).equals("NNS")
					|| sent.posTag(gov).equals("NN")) {	//if gov is a noun, let's look for the governing verb! e.g., ...have no child
				for (int k=gov; k<sent.words().size(); k++) {
					if (sent.incomingDependencyLabel(k).isPresent()
							&& sent.incomingDependencyLabel(k).get().equals("dobj")) {
						wordList.add(negFound, "0");
						wordList.remove(negFound+1);
					}
				}
			}
			
			transformed = StringUtils.join(wordList, " ");	
		} 
		
		if (!transformed.equals(original))
			System.err.println("### " + original + " --> " + transformed);
		
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
