package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.json.*;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;

public class Transform {
	
	private String numberRelatedTermsPath = "./data/number_related_terms.tsv";
	private String prefixLatinGreekPath = "./data/prefix_latin_greek.tsv";
	private String postPrefixLatinGreekPath = "./data/post_prefix_latin_greek.tsv";
	
	public Map<String, Integer> prefixLatinGreek;
	private List<String> postPrefixLetinGreek;
	
	public Transform() throws IOException {
		
		prefixLatinGreek = new HashMap<String, Integer>();
		postPrefixLetinGreek = new ArrayList<String>();
		
		String line;
		
		BufferedReader br = new BufferedReader(new FileReader(prefixLatinGreekPath));
		line = br.readLine();
		while(line != null) {
			prefixLatinGreek.put(line.split("\t")[0], Integer.parseInt(line.split("\t")[1]));
			line = br.readLine();
		}
		br.close();
		
		br = new BufferedReader(new FileReader(postPrefixLatinGreekPath));
		line = br.readLine();
		while(line != null) {
			postPrefixLetinGreek.add(line);
			line = br.readLine();
		}
		br.close();
		
	}
	
	public static void main(String[] args) throws JSONException, IOException {
				
		Transform transform = new Transform();
		
		String transformed;
		String sentence;
		
		
		///Problematic
//		sentence = "";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
		
		///test!!!
//		sentence = "John has an ugly son and a beautiful daughter.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//		
//		sentence = "John has a child.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
		
		sentence = "John doesn't have any ugly children.";
		transformed = transform.transform(sentence, true, true, true, true);
		System.out.println(sentence + " --> " + transformed);
		
//		sentence = "John didn't bring crazy young friends yesterday.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//
//		sentence = "John hasn't had children.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//		
//		sentence = "John had no children surviving adulthood.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//		
		sentence = "John has never been married and never had any children.";
		transformed = transform.transform(sentence, true, true, true, true);
		System.out.println(sentence + " --> " + transformed);
		
		sentence = "John has never seen any children.";
		transformed = transform.transform(sentence, true, true, true, true);
		System.out.println(sentence + " --> " + transformed);
		
		sentence = "John has never been married his partner.";
		transformed = transform.transform(sentence, true, true, true, true);
		System.out.println(sentence + " --> " + transformed);
		
		sentence = "Their marriage is without children.";
		transformed = transform.transform(sentence, true, true, true, true);
		System.out.println(sentence + " --> " + transformed);
		
		sentence = "John has never had young children revealed, although we know it.";
		transformed = transform.transform(sentence, true, true, true, true);
		System.out.println(sentence + " --> " + transformed);
		
		sentence = "John has never had any children revealed, although we know it.";
		transformed = transform.transform(sentence, true, true, true, true);
		System.out.println(sentence + " --> " + transformed);
		
//		sentence = "Her marriage produced three children : one son , Axel and twin daughters Elin and Josefin.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//		
//		sentence = "She married twice.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//		
//		sentence = "A dozen eggs are enough.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//		
//		sentence = "The triplets play with a hexagon.";
//		transformed = transform.transform(sentence, true, true, true, true);
//		System.out.println(sentence + " --> " + transformed);
//		
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
	
	public String latinGreek(String word, String lemma) {
		String post;
		for (String key : prefixLatinGreek.keySet()) {
			if (lemma.startsWith(key)) {
				post = lemma.replace(key, "");
				if (postPrefixLetinGreek.contains(post)) {
					if (post.equals("uplet")){
						return "LatinGreek_" + word + "_" + prefixLatinGreek.get(key) + "_" + "plet";
					} else {
						return "LatinGreek_" + word + "_" + prefixLatinGreek.get(key) + "_" + post;
					}
				} else if (post.isEmpty()) {
					if (key.equalsIgnoreCase("duo") || key.equalsIgnoreCase("trio")) {
						return "LatinGreek_" + word + "_" + prefixLatinGreek.get(key) + "_" + "o";
					}
				}
			}
		}
		return "";
	}
	
	public String transform(String sentence, boolean articles, boolean negative, boolean otherConcepts, boolean latinGreek) throws IOException {
		String line, transformed = sentence, term;
		
//		System.out.println(sentence);
		
		if (otherConcepts) {
			BufferedReader br = new BufferedReader(new FileReader(numberRelatedTermsPath));
			line = br.readLine();
			while (line != null) {
				term = line.split("\t")[0];
				transformed = transformed.replaceAll("\\b["+Character.toUpperCase(term.charAt(0))+term.charAt(0)+"]"+term.substring(1)+"\\b", line.split("\t")[1]);
				line = br.readLine();
			}
			br.close();
		}
		
		if (negative) {
			transformed = transformNegative(transformed);
		} 
		
		Sentence sent = new Sentence(transformed);
		List<String> wordList = new ArrayList<String>();
		for (int i=0; i<sent.words().size(); i++) {
			
			if (articles
					&& sent.lemma(i).equals("a")
					&& sent.incomingDependencyLabel(i).isPresent()
					&& sent.incomingDependencyLabel(i).get().equals("det")) {
				wordList.add("1");
				
			} else if (latinGreek) {
				term = latinGreek(sent.word(i), sent.lemma(i));
				if (!term.equals("")) wordList.add(term);
				else wordList.add(sent.word(i));
			
			} else {
				wordList.add(sent.word(i));
			}
		}
		
		return StringUtils.join(wordList, " ");
	}
	
	public String transformCountableQuantifiers(String sentence) {
		String transformed = sentence;
		transformed = transformed.replaceAll(" a few ", " few ");
		transformed = transformed.replaceAll(" lots of ", "many");
		transformed = transformed.replaceAll(" a lot of ", "many");
		transformed = transformed.replaceAll(" plenty of ", "many");
		transformed = transformed.replaceAll(" a number of ", "several");
		transformed = transformed.replaceAll(" a couple of ", "several");		
		return transformed;
	}
	
	public String transformOthers(String sentence) throws IOException {
		String line, transformed = sentence, term;
		
		BufferedReader br = new BufferedReader(new FileReader(numberRelatedTermsPath));
		line = br.readLine();
		while (line != null) {
			term = line.split("\t")[0];
			transformed = transformed.replaceAll("\\b["+Character.toUpperCase(term.charAt(0))+term.charAt(0)+"]"+term.substring(1)+"\\b", line.split("\t")[1]);
			line = br.readLine();
		}
		br.close();
		
		return transformed;
	}
	
	public String transformLatinGreek(String sentence) {
		String line, transformed = sentence, term;
		
		Sentence orig = new Sentence(transformed);
		List<String> trans = new ArrayList<String>();
		for (int i=0; i<orig.words().size(); i++) {
			term = latinGreek(orig.word(i), orig.lemma(i));
			if (!term.equals("")) trans.add(term);
			else trans.add(orig.word(i));
		}
		
		return StringUtils.join(trans, " ");
	}
	
	public String transformArticles(String sentence) {
		Sentence orig = new Sentence(sentence);
		List<String> transformed = new ArrayList<String>();
		for (int i=0; i<orig.words().size(); i++) {
			if (orig.lemma(i).equals("a")
					&& orig.incomingDependencyLabel(i).isPresent()
					&& orig.incomingDependencyLabel(i).get().equals("det")) {
				transformed.add("1");
			} else {
				transformed.add(orig.word(i));
			}
		}
		return StringUtils.join(transformed, " ");
	}
	
	private int findNegative(Sentence sent, List<Integer> skipped) {
		List<Integer> negFoundList = new ArrayList<Integer>();		
		for (int i=0; i<sent.words().size(); i++) {
			if (sent.incomingDependencyLabel(i).isPresent()
					&& sent.incomingDependencyLabel(i).get().equals("neg")
					&& !skipped.contains(i)) {
				return i;
			}
		}
		return -999;
	}
	
	public String transformNegative(String sentence) {
		sentence = sentence.replaceAll("without", "with no");
		Sentence sent = new Sentence(sentence);
		List<String> wordList = new ArrayList<String>();
		wordList.addAll(sent.words());
		
		for (int i=0; i<sent.length(); i++) {
			if (sent.lemma(i).equals("any")
					&& sent.posTag(i).equals("DT")
					&& sent.incomingDependencyLabel(i).isPresent()
					) {
				int noun = sent.governor(i).get();
				int verb = sent.governor(noun).get();
				for (int j=verb-1; j>=0; j--) {
					if (sent.incomingDependencyLabel(j).isPresent()
							&& sent.incomingDependencyLabel(j).get().equals("neg")
							) {
						wordList.set(i, "no");
						if (sent.lemma(j).equals("not") || sent.lemma(j).equals("never")) {
							wordList.set(j, "");
						}
						if (sent.lemma(j-1).equals("do")) {
							wordList.set(j-1, "");
						}
						break;
					}
				}
			}
		}
		
		String transformed = "";
		for (String word : wordList) {
			if (!word.equals("")) transformed += " " + word;
		}
		wordList.clear();
		sent = new Sentence(transformed);
		wordList.addAll(sent.words());
		
		for (int i=0; i<sent.length(); i++) {
			if (sent.lemma(i).equals("never")
					&& sent.posTag(i).equals("RB")
					&& sent.incomingDependencyLabel(i).isPresent()
					) {
				int verb = sent.governor(i).get();
				wordList.set(i, "");
				wordList.set(verb, sent.word(verb) + " 0 time");
			}
		}
		
		transformed = "";
		for (String word : wordList) {
			if (!word.equals("")) transformed += " " + word;
		}
		return transformed.substring(1);
	}
	
	public String transformNegativeOld(String sentence) {
		String transformed = "", original = "";
		
		//// e.g., Their marriage is without children --> Their marriage is with 0 children
		if (sentence.contains("without")) transformed = sentence.replaceAll("without", "with 0");
		else transformed = sentence;
		
		Sentence sent = new Sentence(transformed);
		List<Integer> skipped = new ArrayList<Integer>();
		int negFound = findNegative(sent, skipped);
		List<String> wordList = new ArrayList<String>();
		
		int gov = -999, det = -999, noun = -999, verbAcl = -999;
		boolean objExist = false, compExist = false, negRemoved = false;
		
		while (negFound > 0) {
			
			gov = sent.governor(negFound).get();
			wordList.clear();
			wordList.addAll(sent.words());
			negRemoved = false;
			
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
										negRemoved = true;
									}
									break;
								} else if (noun > 0) {		//...never saw crazy children
									wordList.add(noun+1, "0 times");
									wordList.remove(negFound);
									negRemoved = true;
									break;
								} else if (verbAcl > 0) {	//...have never had any children born
									wordList.add(verbAcl+1, "0 times");
									wordList.remove(negFound);
									negRemoved = true;
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
										negRemoved = true;
									}
									break;
								} else {			//...have never had children revealed
									wordList.add(k+1, "0 times");
									wordList.remove(negFound);
									negRemoved = true;
									break;
								}
								
								
							}
							
						}
					}
					
					if (!objExist && !compExist) {	//...have never been married
						wordList.add(gov+1, "0 times");
						wordList.remove(negFound);
						negRemoved = true;
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
									negRemoved = true;
								}
								break;
							} else if (noun > 0) {			//...have young bright children
								wordList.add(noun, "0");
								wordList.remove(negFound);
								negRemoved = true;
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
						negRemoved = true;
					}
				}
			}
			
			if (!negRemoved) {
				skipped.add(negFound);
			}
			
			
			transformed = StringUtils.join(wordList, " ");	
			sent = new Sentence(transformed);
			negFound = findNegative(sent, skipped);
		} 
		
//		if (!transformed.equals(original))
//			System.err.println("### " + original + " --> " + transformed);
		
		return transformed;
	}
	
}
