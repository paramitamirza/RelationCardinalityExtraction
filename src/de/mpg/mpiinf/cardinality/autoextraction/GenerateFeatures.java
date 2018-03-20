package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class GenerateFeatures implements Runnable {
	
	private String dirFeature;
	private String relName;
	
	private WikipediaArticle wiki;
	
	private String wikidataId;
	private String count;
	private Integer curId;
	
	private boolean training;
	
	private boolean nummod;
	private boolean ordinal;
	private boolean numterms;
	
	private boolean articles;
	private boolean quantifiers;
	private boolean pronouns;
	
	private boolean compositional;
	private boolean negation;
	private boolean negTrain;
	
	private double countInfThreshold;
	private double countDist;
	
	private boolean ignoreHigher;
	private boolean ignoreFreq;
	private int ignoreHigherLess;
	private int maxTripleCount;
	
	private List<Long> frequentNumbers;
	
	public GenerateFeatures(String dirFeature, String relName,
			WikipediaArticle wiki, String wikidataId, String count, Integer curId, String freqNum,
			boolean training,
			boolean ignoreHigher, int ignoreHigherLess,
			float countInfThreshold, String countDist,
			boolean ignoreFreq, int maxCount,
			boolean nummod, boolean ordinal, boolean numterms,
			boolean articles, boolean quantifiers, boolean pronouns,
			boolean compositional, 
			boolean negation, boolean negTrain
			) {
		this.setDirFeature(dirFeature);
		this.setRelName(relName);
		
		this.setWiki(wiki);
		
		this.setWikidataId(wikidataId);
		this.setCount(count);
		this.setCurId(curId);
		
		this.setTraining(training);
		
		this.setNummod(nummod);
		this.setOrdinal(ordinal);
		this.setNumterms(numterms);
		
		this.setArticles(articles);
		this.setQuantifiers(quantifiers);
		this.setPronouns(pronouns);
		
		this.setCompositional(compositional);
		
		this.setNegation(negation);
		this.setNegTrain(negTrain);
		
		this.setCountInfThreshold(countInfThreshold);
		this.setCountDist(Double.parseDouble(countDist));
		
		this.setIgnoreHigher(ignoreHigher);
		this.setIgnoreFreq(ignoreFreq);
		this.setIgnoreHigherLess(ignoreHigherLess);
		this.setMaxTripleCount(maxCount);
		
		List<Long> freqNums = new ArrayList<Long>();
		String freqs = freqNum.substring(1, freqNum.length()-1);
		if (!freqs.isEmpty()) {
			for (String f : freqs.split(";")) freqNums.add(Long.parseLong(f));
		}
		this.setFrequentNumbers(freqNums);
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		
//		featExtraction.generateColumnsFile(true, false, 0);
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub		
        try {
        	int numOfTriples = Integer.parseInt(this.getCount());
    		String wikipediaText = this.getWiki().fetchArticle(this.getCurId());
			
			if (wikipediaText != "") {
				
				String original;
	    		Sentence sent;
	    		StringBuilder toPrint = new StringBuilder();
	    		
	    		int j=0;
	    		Transform trans = new Transform();
	    		
	    		for (String l : wikipediaText.split("\\r?\\n")) {	//Split the paragraphs
	    			
	    			if (!l.trim().isEmpty()) {
		    			Document doc = new Document(l);
		    			
//		    			System.out.println("-----");
//		    			System.out.println(l);
//		    			System.out.println(doc.coref());
		    				    			
		    			for (Sentence s : doc.sentences()) {	//Split the sentences
		    				
		    				original = s.text();	    				
		    				sent = filter(original, trans);
		    				
		    				if (sent != null) {
		    					
		    					toPrint.append(generateFeatures(sent, j, numOfTriples, 
		    							this.isOrdinal(), this.isNummod(), this.isCompositional(), 
		    							this.getCountInfThreshold(), this.getCountDist(),
		    							this.isIgnoreHigher(), this.getIgnoreHigherLess(),
		    							this.isIgnoreFreq(), this.getMaxTripleCount(), this.isNegTrain()).toString());
		    				}
		    				
		    				j ++;
		    	        }
	    			}
	    	    }
	    		
//	    		synchronized (this) {
//	    			PrintWriter outfile;
//	    			if (!this.isTraining()) {
//						outfile = new PrintWriter(new BufferedWriter(new FileWriter(this.getDirFeature() + "/" + this.getRelName() + "_test_cardinality.data", true)));
//					} else {
//						outfile = new PrintWriter(new BufferedWriter(new FileWriter(this.getDirFeature() + "/" + this.getRelName() + "_train_cardinality.data", true)));
//					}
//	    			outfile.print(toPrint.toString());
//	    			outfile.close();
//	    		}
	    		
	    		String outFilePath;
	    		if (!this.isTraining()) {
	    			outFilePath = this.getDirFeature() + "/" + this.getRelName() + "_test_cardinality.data";
	    		} else {
	    			outFilePath = this.getDirFeature() + "/" + this.getRelName() + "_train_cardinality.data";
	    		}
	    		WriteToFile.getInstance().appendContents(outFilePath, toPrint.toString());
			}			
			
		} catch (JSONException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Sentence filter(String sentence, Transform trans) throws IOException {
		
		Sentence sent;
		String sentStr;
		
		if (this.isTraining()) {
			sentStr = sentence;
			if (this.isNumterms()) {
				sentStr = trans.transform(sentStr, false, false, this.isNumterms(), this.isNumterms());
			}
			sent = new Sentence(sentStr);
			
			if (Numbers.containsNumerals(sent, this.isOrdinal()))
				return sent;
			else
				return null;
			
		} else {
			sentStr = sentence;
			if (this.isNumterms()) {
				sentStr = trans.transform(sentStr, false, this.isNegation(), this.isNumterms(), this.isNumterms());
			}
			sent = new Sentence(sentStr);
			
			if (Numbers.containsNumerals(sent, this.isOrdinal())
					|| (this.isArticles() && Numbers.containsArticles(sent))
					|| (this.isQuantifiers() && Numbers.containsCountableQuantifiers(sentStr))
					|| (this.isPronouns() && Numbers.containsPersonalPronouns(sent))
					|| (this.isNegation() && Numbers.containsNo(sent))
					|| (this.isNegation() && Numbers.containsNo(sent))
					|| (this.isNegTrain() && Numbers.containsNegation(sent))
					)
				return sent;
			else
				return null;
		}
	}
	
	public static <K,V extends Comparable<? super V>> 
    	List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

		List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());
		
		Collections.sort(sortedEntries, 
		    new Comparator<Entry<K,V>>() {
		        @Override
		        public int compare(Entry<K,V> e1, Entry<K,V> e2) {
		            return e2.getValue().compareTo(e1.getValue());
		        }
		    }
		);
		
		return sortedEntries;
	}
	
	private String decideOnLabelEqual(long numInt, double countInfThreshold, double countDist) {
		String label = "O";
		
		if (-(Math.log(countDist) / Math.log(2)) >= countInfThreshold) { //numOfTriples > threshold
			if (!this.getFrequentNumbers().contains(numInt)) {
				label = "_YES_";
			} else {
				label = "_MAYBE_";
			}
		} else {				
			if (!this.getFrequentNumbers().contains(numInt)) {
				label = "_MAYBE_";
			} else {
				label = "_NO_";	
			}
		}
		
		return label;
	}
	
	private String decideOnLabelHigher(int numOfTriples, long numInt, int ignoreHigherLess,
			int maxTripleCount) {
		String label = "O";
		
		if ((numInt <= maxTripleCount)
				&& (((ignoreHigherLess > 0) 
						&& (numInt <= (numOfTriples + ignoreHigherLess)))
				|| (ignoreHigherLess == 0))
		) {
			label = "_MAYBE_";
		} else {
			label = "_NO_";
		}
		
		return label;
	}
	
	private String decideOnLabelNonCompositional(boolean nummod, int numOfTriples, long numInt, int k, 
			String deprel, int depIdx, 
			double countInfThreshold, double countDist,
			boolean ignoreHigher, int ignoreHigherLess,
			int maxTripleCount) {
		String label = "O";
		
		if (numInt == numOfTriples
				&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
						|| !nummod)
				) {
			label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
			
		} else if (numInt > numOfTriples
				&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
						|| !nummod)
				) {	
			label = decideOnLabelHigher(numOfTriples, numInt, ignoreHigherLess, maxTripleCount);

		} else {	//numInt < numOfTriples
			label = "_NO_";
		}
		
		return label;
	}
	
	private String decideOnLabelNonCompositionalLatinGreek(boolean nummod, int numOfTriples, long numInt, int k, 
			String deprel, int depIdx, 
			double countInfThreshold, double countDist,
			boolean ignoreHigher, int ignoreHigherLess,
			int maxTripleCount) {
		String label = "O";
		
		if (numInt == numOfTriples
				&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
						|| !nummod)
				) {
			label = "_YES_";
			
		} else if (numInt > numOfTriples
				&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
						|| !nummod)
				) {	
			label = decideOnLabelHigher(numOfTriples, numInt, ignoreHigherLess, maxTripleCount);

		} else {	//numInt < numOfTriples
			label = "_NO_";
		}
		
		return label;
	}
	
	private String decideOnLabelNonCompositionalOrdinal(boolean nummod, int numOfTriples, long numInt, int k, 
			String deprel, int depIdx, 
			double countInfThreshold, double countDist,
			boolean ignoreHigher, int ignoreHigherLess,
			int maxTripleCount) {
		String label = "O";
		
		if (numInt == numOfTriples
				&& ((nummod && deprel.startsWith("amod") && depIdx >= k)
						|| !nummod)
				) {
			label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
			
		} else if (numInt > numOfTriples
				&& ((nummod && deprel.startsWith("amod") && depIdx >= k)
						|| !nummod)
				) {	
			label = decideOnLabelHigher(numOfTriples, numInt, ignoreHigherLess, maxTripleCount);

		} else {	//numInt < numOfTriples
//			label = "_NO_";
			label = "_YES_";
		}
		
		return label;
	}
	
	private StringBuilder generateFeatures(Sentence sent, int j, int numOfTriples, 
			boolean ordinal, boolean nummod, boolean compositional, 
			double countInfThreshold, double countDist,
			boolean ignoreHigher, int ignoreHigherLess,
			boolean ignoreFreq, int maxTripleCount,
			boolean negTrain) {
		String word = "", lemma = "", pos = "", ner = "", deprel = "", dependent = "", label = "";
		StringBuilder sb = new StringBuilder();
		int k, depIdx;
		boolean lrb = false;
		
		List<Integer> idxToAdd = new ArrayList<Integer>();
		List<Integer> idxComp = new ArrayList<Integer>();
		long numToAdd = 0;
		
		List<String> labels = new ArrayList<String>();
		List<String> tokenFeatures = new ArrayList<String>();
		int tokenIdx = 0;
		
		long numInt;
		
		//Conditions for compositionality
		boolean conjExist = false;
		int lastCompIdx = 0;
		
		for (k=0; k<sent.words().size(); k++) {
			pos = sent.posTag(k);
			ner = sent.nerTag(k);
			word = sent.word(k);
			lemma = sent.lemma(k);
			deprel = "O"; 
			if (sent.incomingDependencyLabel(k).isPresent()) {
				deprel = sent.incomingDependencyLabel(k).get();
			}
			dependent = "O"; depIdx = k;
			if (sent.governor(k).isPresent() && !deprel.equals("root")) {
				depIdx = sent.governor(k).get();
				if (depIdx > k) {	//if the modified noun is AFTER the number
					dependent = sent.lemma(depIdx);
					if (dependent.startsWith("latingreek_")) dependent = "O";
				}
			}
			label = "O";
			
			if (!this.isTraining()
					&& this.isArticles() 
					&& Numbers.properArticle(word, pos, deprel)
					&& !dependent.equals("O")
					) {
				lemma = "_num_";
//				lemma += "one_";
				
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
			
			} else if (!this.isTraining()
					&& this.isQuantifiers()
					&& Numbers.properCountableQuantifier(word, pos, deprel)
					&& !dependent.equals("O")
					) {
				lemma = "_num_";
//				lemma += "5_";
				
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
			} else if (!this.isTraining()
					&& this.isPronouns()
					&& Numbers.possessivePronoun(pos)
					&& deprel.equals("nmod:poss")
					&& depIdx == k+1	//only if the modified noun is RIGHT AFTER the personal pronoun, e.g., his children
					&& !dependent.equals("O")
					) {
				lemma = "_num_";
				if (sent.posTag(depIdx).equals("NN")) {
					word = "PRP$_S_" + word;
//					lemma += "one_";
					
				} else if (sent.posTag(depIdx).equals("NNS")) {
					word = "PRP$_P_" + word;
//					lemma += "5_";
				}
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
			} else if (!this.isTraining()
					&& this.isNegation() 
					&& Numbers.properNo(word, pos)
					&& !dependent.equals("O")
					) {
				lemma = "_num_";
				
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
			
			} else if (sent.word(k).startsWith("LatinGreek_")) {
				word = sent.word(k).split("_")[0] + "_" + sent.word(k).split("_")[1] + "_" + sent.word(k).split("_")[2];
				lemma = "_" + sent.word(k).split("_")[3] + "_";
				dependent = "O";
				
				numInt = Long.parseLong(sent.word(k).split("_")[2]);
				
//				if (numInt == 1) lemma += "one_";
//				if (numInt >= 2 && numInt <= 5) lemma += "5_";
//				if (numInt >= 6 && numInt <= 10) lemma += "10_";
//				if (numInt >= 11 && numInt <= 20) lemma += "20_";
//				if (numInt >= 21 && numInt <= 30) lemma += "30_";
//				if (numInt >= 31 && numInt <= 40) lemma += "40_";
//				if (numInt >= 41 && numInt <= 50) lemma += "50_";
//				if (numInt >= 51) lemma += "big_";
				
				if (compositional) {
					if (numToAdd > 0) {
						if (numInt == numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								) {
//							label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
							label = "_YES_";
							numToAdd = 0;
							idxToAdd.clear();
							conjExist = false;
							idxComp.clear();
							
						} else {
							if ((numToAdd+numInt) == numOfTriples
//									&& ((nummod && deprel.startsWith("nummod"))
//											|| !nummod)
									) {
								if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
									label = "_YES_";
									int lastNumIdx = tokenIdx;
									for (Integer nnn : idxToAdd) {
										labels.set(nnn, "_YES_");
										for (Integer ooo : idxComp) {
											if (ooo > nnn && ooo < lastNumIdx) {
												labels.set(ooo, "_COMP_");
											}
										}
										lastNumIdx = nnn;
									}
								}
								numToAdd = 0;
								idxToAdd.clear();
								conjExist = false;
								idxComp.clear();
								
							} else if ((numToAdd+numInt) < numOfTriples
//									&& ((nummod && deprel.startsWith("nummod"))
//											|| !nummod)
									) {
								label = "_NO_";
								if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
									numToAdd += numInt;
									idxToAdd.add(tokenIdx);
									lastCompIdx = tokenIdx;
								} else {
									numToAdd = 0;
									idxToAdd.clear();
								}								
								conjExist = false;
								idxComp.clear();
								
							} else {	//(numToAdd+numInt) > numOfTriples
								label = decideOnLabelHigher(numOfTriples, (numToAdd+numInt), ignoreHigherLess, maxTripleCount);
								numToAdd = 0;
								idxToAdd.clear();
								conjExist = false;
								idxComp.clear();
							}
						}
						
					} else {
						if (numInt == numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								) {
//							label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
							label = "_YES_";
							
						} else if (numInt < numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								) {
							label = "_NO_";
							numToAdd += numInt;
							idxToAdd.add(tokenIdx);
							lastCompIdx = tokenIdx;
							conjExist = false;
							idxComp.clear();
							
						} else if (numInt > numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								){		
							
							label = decideOnLabelHigher(numOfTriples, numInt, ignoreHigherLess, maxTripleCount);
							conjExist = false;
							idxComp.clear();
							
						} else {
							label = "_NO_";
						}
					}
					
				} else {  
					label = decideOnLabelNonCompositionalLatinGreek(false, numOfTriples, numInt, k, deprel, depIdx, countInfThreshold, countDist,
							ignoreHigher, ignoreHigherLess, maxTripleCount);
				}
				
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;								
				
			} else if (Numbers.properNumber(pos, ner)) {						
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
				while (k<sent.words().size()) {
					if (Numbers.properNumber(sent.posTag(k), sent.nerTag(k))
							|| Numbers.properConjNumber(sent.posTag(k), sent.nerTag(k))
							) {
						word += sent.word(k) + "_";
						lemma += sent.lemma(k) + "_";
						if (sent.incomingDependencyLabel(k).isPresent()) deprel = sent.incomingDependencyLabel(k).get();
						if (sent.governor(k).isPresent() && deprel.equals("nummod")) {
							depIdx = sent.governor(k).get();
							dependent = sent.lemma(sent.governor(k).get());
						}
						k++;
						
					} else {
						break;
					}
				}
				word = word.substring(0, word.length()-1);
				lemma = lemma.substring(0, lemma.length()-1);
				
				numInt = Numbers.getInteger(word.toLowerCase());
				
				if (numInt >= 0) {
					lemma = "_num_";
//					if (numInt == 1) lemma += "one_";
//					if (numInt >= 2 && numInt <= 5) lemma += "5_";
//					if (numInt >= 6 && numInt <= 10) lemma += "10_";
//					if (numInt >= 11 && numInt <= 20) lemma += "20_";
//					if (numInt >= 21 && numInt <= 30) lemma += "30_";
//					if (numInt >= 31 && numInt <= 40) lemma += "40_";
//					if (numInt >= 41 && numInt <= 50) lemma += "50_";
//					if (numInt >= 51) lemma += "big_";
					
					if (compositional) {
						if (numToAdd > 0) {
							if (numInt == numOfTriples
									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
											|| !nummod)
									) {
								label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
								numToAdd = 0;
								idxToAdd.clear();
								conjExist = false;
								idxComp.clear();
								
							} else {
								if ((numToAdd+numInt) == numOfTriples
										&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
												|| !nummod)
										) {
									if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
										label = "_YES_";
										int lastNumIdx = tokenIdx;
										for (Integer nnn : idxToAdd) {
											labels.set(nnn, "_YES_");
											for (Integer ooo : idxComp) {
												if (ooo > nnn && ooo < lastNumIdx) {
													labels.set(ooo, "_COMP_");
												}
											}
											lastNumIdx = nnn;
										}
									}
									numToAdd = 0;
									idxToAdd.clear();
									conjExist = false;
									idxComp.clear();
									
								} else if ((numToAdd+numInt) < numOfTriples
										&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
												|| !nummod)
										) {
									label = "_NO_";
									if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
										numToAdd += numInt;
										idxToAdd.add(tokenIdx);
										lastCompIdx = tokenIdx;
									} else {
										numToAdd = 0;
										idxToAdd.clear();
									}								
									conjExist = false;
									idxComp.clear();
									
								} else {	//(numToAdd+numInt) > numOfTriples
									label = decideOnLabelHigher(numOfTriples, (numToAdd+numInt), ignoreHigherLess, maxTripleCount);
									numToAdd = 0;
									idxToAdd.clear();
									conjExist = false;
									idxComp.clear();
								}
							}
							
						} else {
							if (numInt == numOfTriples
									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
											|| !nummod)
									) {
								label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
								
							} else if (numInt < numOfTriples
									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
											|| !nummod)
									) {
								label = "_NO_";
								numToAdd += numInt;
								idxToAdd.add(tokenIdx);
								lastCompIdx = tokenIdx;
								conjExist = false;
								idxComp.clear();
								
							} else if (numInt > numOfTriples
									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
											|| !nummod)
									){
								label = decideOnLabelHigher(numOfTriples, numInt, ignoreHigherLess, maxTripleCount);
								conjExist = false;
								idxComp.clear();
								
							} else {
								label = "_NO_";
							}
						}
						
					} else {
						label = decideOnLabelNonCompositional(true, numOfTriples, numInt, k, deprel, depIdx, countInfThreshold, countDist,
								ignoreHigher, ignoreHigherLess, maxTripleCount);
					}
				}
				
				k--;
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
			} else if (ordinal && Numbers.properOrdinal(pos, ner)) {						
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
				while (k<sent.words().size()) {
					if (Numbers.properOrdinal(sent.posTag(k), sent.nerTag(k))) {
						word += sent.word(k) + "_";
						lemma += sent.lemma(k) + "_";
						if (sent.incomingDependencyLabel(k).isPresent()) deprel = sent.incomingDependencyLabel(k).get();
						if (sent.governor(k).isPresent() && deprel.equals("amod")) {
							depIdx = sent.governor(k).get();
							dependent = sent.lemma(sent.governor(k).get());
						}
						k++;
						
					} else {
						break;
					}
				}
				word = word.substring(0, word.length()-1);
				lemma = lemma.substring(0, lemma.length()-1);
				
				numInt = Numbers.getInteger(word.toLowerCase());
				
				if (numInt >= 0) {
					lemma = "_ord_";
//					if (numInt == 1) lemma += "one_";
//					if (numInt >= 2 && numInt <= 5) lemma += "5_";
//					if (numInt >= 6 && numInt <= 10) lemma += "10_";
//					if (numInt >= 11 && numInt <= 20) lemma += "20_";
//					if (numInt >= 21 && numInt <= 30) lemma += "30_";
//					if (numInt >= 31 && numInt <= 40) lemma += "40_";
//					if (numInt >= 41 && numInt <= 50) lemma += "50_";
//					if (numInt >= 51) lemma += "big_";
					
//					if (compositional) {
//						if (numToAdd > 0) {
//							if (numInt == numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									) {
//								label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
//								numToAdd = 0;
//								idxToAdd.clear();
//								conjExist = false;
//								
//							} else {
//								if ((numToAdd+numInt) == numOfTriples
//										&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//												|| !nummod)
//										) {
//									if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
//										label = "_YES_";
//										for (Integer nnn : idxToAdd) labels.set(nnn, "_YES_");
//									}
//									numToAdd = 0;
//									idxToAdd.clear();
//									conjExist = false;
//									
//								} else if ((numToAdd+numInt) < numOfTriples
//										&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//												|| !nummod)
//										) {
//									label = "_NO_";
//									if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
//										numToAdd += numInt;
//										idxToAdd.add(tokenIdx);
//										lastCompIdx = tokenIdx;
//									} else {
//										numToAdd = 0;
//										idxToAdd.clear();
//									}								
//									conjExist = false;
//									
//								} else {	//(numToAdd+numInt) > numOfTriples
//									label = decideOnLabelHigher(numOfTriples, (numToAdd+numInt), ignoreHigherLess, maxTripleCount);
//									numToAdd = 0;
//									idxToAdd.clear();
//									conjExist = false;
//								}
//							}
//							
//						} else {
//							if (numInt == numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									) {
//								label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
//								
//							} else if (numInt < numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									) {
//								label = "_NO_";
//								numToAdd += numInt;
//								idxToAdd.add(tokenIdx);
//								lastCompIdx = tokenIdx;
//								conjExist = false;
//								
//							} else if (numInt > numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									){
//								label = decideOnLabelHigher(numOfTriples, numInt, ignoreHigherLess, maxTripleCount);
//								conjExist = false;
//								
//							} else {
//								label = "_NO_";
//							}
//						}
//						
//					} else {
						label = decideOnLabelNonCompositionalOrdinal(true, numOfTriples, numInt, k, deprel, depIdx, countInfThreshold, countDist,
								ignoreHigher, ignoreHigherLess, maxTripleCount);
//					}
				}
				
				k--;
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
			} else if (ordinal && Numbers.properNumOrdinal(pos, ner)) {						
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
				while (k<sent.words().size()) {
					if (Numbers.properNumOrdinal(sent.posTag(k), sent.nerTag(k))
							|| Numbers.properOrdinal(sent.posTag(k), sent.nerTag(k))
							|| Numbers.properConjOrdinal(sent.posTag(k), sent.nerTag(k))
							) {
						word += sent.word(k) + "_";
						lemma += sent.lemma(k) + "_";
						if (sent.incomingDependencyLabel(k).isPresent()) deprel = sent.incomingDependencyLabel(k).get();
						if (sent.governor(k).isPresent() && deprel.equals("amod")) {
							depIdx = sent.governor(k).get();
							dependent = sent.lemma(sent.governor(k).get());
						}
						k++;
						
					} else {
						break;
					}
				}
				word = word.substring(0, word.length()-1);
				lemma = lemma.substring(0, lemma.length()-1);
				
				numInt = Numbers.getInteger(word.toLowerCase());
				
				if (numInt >= 0) {
					lemma = "_ord_";
					
//					if (compositional) {
//						if (numToAdd > 0) {
//							if (numInt == numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									) {
//								label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
//								numToAdd = 0;
//								idxToAdd.clear();
//								conjExist = false;
//								
//							} else {
//								if ((numToAdd+numInt) == numOfTriples
//										&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//												|| !nummod)
//										) {
//									if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
//										label = "_YES_";
//										for (Integer nnn : idxToAdd) labels.set(nnn, "_YES_");
//									}
//									numToAdd = 0;
//									idxToAdd.clear();
//									conjExist = false;
//									
//								} else if ((numToAdd+numInt) < numOfTriples
//										&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//												|| !nummod)
//										) {
//									label = "_NO_";
//									if (conjExist && (tokenIdx-lastCompIdx) <= 5) {
//										numToAdd += numInt;
//										idxToAdd.add(tokenIdx);
//										lastCompIdx = tokenIdx;
//									} else {
//										numToAdd = 0;
//										idxToAdd.clear();
//									}								
//									conjExist = false;
//									
//								} else {	//(numToAdd+numInt) > numOfTriples
//									label = decideOnLabelHigher(numOfTriples, (numToAdd+numInt), ignoreHigherLess, maxTripleCount);
//									numToAdd = 0;
//									idxToAdd.clear();
//									conjExist = false;
//								}
//							}
//							
//						} else {
//							if (numInt == numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									) {
//								label = decideOnLabelEqual(numInt, countInfThreshold, countDist);
//								
//							} else if (numInt < numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									) {
//								label = "_NO_";
//								numToAdd += numInt;
//								idxToAdd.add(tokenIdx);
//								lastCompIdx = tokenIdx;
//								conjExist = false;
//								
//							} else if (numInt > numOfTriples
//									&& ((nummod && deprel.startsWith("nummod") && depIdx >= k)
//											|| !nummod)
//									){
//								label = decideOnLabelHigher(numOfTriples, numInt, ignoreHigherLess, maxTripleCount);
//								conjExist = false;
//								
//							} else {
//								label = "_NO_";
//							}
//						}
//						
//					} else {
						label = decideOnLabelNonCompositionalOrdinal(true, numOfTriples, numInt, k, deprel, depIdx, countInfThreshold, countDist,
								ignoreHigher, ignoreHigherLess, maxTripleCount);
//					}
				}
				
				k--;
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
			} else if (this.isNegation() && negTrain && (Numbers.properUnLessAdj(lemma, pos)
							|| Numbers.properNegation(lemma, pos)
						)
					) {						
				deprel = "O"; dependent = "O";
				
				if (numOfTriples == 0) {
					label = "_YES_";
				}
				
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
			} else if (Numbers.properNoun(pos)) {
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
				while (k<sent.words().size()) {
					if (sent.word(k).startsWith("LatinGreek_")) {
						break;
					} else if (Numbers.properNoun(sent.posTag(k))) {
						word += sent.word(k) + "_";
						lemma = "_propernoun_";
						k++;
						
					} else if ((sent.posTag(k).equals("-LRB-") || sent.posTag(k).equals("``")) 
							&& ( (k+1<sent.words().size() && Numbers.properName(sent.posTag(k+1), sent.nerTag(k+1))) 
									|| ((k+2<sent.words().size() && Numbers.properName(sent.posTag(k+2), sent.nerTag(k+2))))
							   )) {
						word += sent.word(k) + "_";
						lemma = "_propernoun_";
						k++;
						lrb = true;
						
					} else if (lrb && (sent.posTag(k).equals("-RRB-") || sent.posTag(k).equals("''"))) {
						word += sent.word(k) + "_";
						lemma = "_propernoun_";
						k++;
						lrb = false;
						
					} else {
						break;
					}
				}
				
				k--;
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word.substring(0, word.length()-1), lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
			} else if (Numbers.personalPronoun(pos)) {
				word = sent.word(k);
				lemma = "_prp_";
				
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, dependent));
				labels.add(label);
				tokenIdx ++;
				
				word = ""; lemma = ""; deprel = "O"; dependent = "O";
				
			} else {
				word = sent.word(k);
				lemma = sent.lemma(k);
				
				if (lemma.equals("and")
						|| lemma.equals(",")) {	//comma or 'and'
					conjExist = true;
					idxComp.add(tokenIdx);
				}
				
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, "O"));
				labels.add(label);
				tokenIdx ++;
			}
			
		}
		
		if (this.isTraining()) {
		
//			if (threshold > 0 || ignoreFreq || ignoreHigher) {		
				Set<String> sentLabels = new HashSet<String>(labels);
				if (sentLabels.contains("_YES_") || sentLabels.contains("_NO_")) {		
					for (int t=0; t<tokenFeatures.size(); t++) {
						label = labels.get(t);
						label = label.replace("_NO_", "O");
						label = label.replace("_MAYBE_", "O");
						sb.append(tokenFeatures.get(t) + "\t" + label);
						sb.append(System.getProperty("line.separator"));
					}
				} 
				
//			} else {
//				for (int t=0; t<tokenFeatures.size(); t++) {
//					label = labels.get(t);
//					label = label.replace("_NO_", "O");
//					label = label.replace("_MAYBE_", "O");
//					sb.append(tokenFeatures.get(t) + "\t" + label);
//					sb.append(System.getProperty("line.separator"));
//				}
//			}
			
		} else {
			for (int t=0; t<tokenFeatures.size(); t++) {
				label = labels.get(t);
				label = label.replace("_NO_", "O");
				label = label.replace("_MAYBE_", "O");
				sb.append(tokenFeatures.get(t) + "\t" + label);
				sb.append(System.getProperty("line.separator"));
			}
		}
		
		sb.append(System.getProperty("line.separator"));
		
		return sb;
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep;
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep, String label) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep + "\t" + label;
	}

	public String getWikidataId() {
		return wikidataId;
	}

	public void setWikidataId(String wikidataId) {
		this.wikidataId = wikidataId;
	}

	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		this.count = count;
	}
	
	public boolean isTraining() {
		return training;
	}

	public void setTraining(boolean training) {
		this.training = training;
	}
	
	public String getDirFeature() {
		return dirFeature;
	}

	public void setDirFeature(String dirFeature) {
		this.dirFeature = dirFeature;
	}
	
	public String getRelName() {
		return relName;
	}

	public void setRelName(String relName) {
		this.relName = relName;
	}
	
	public boolean isOrdinal() {
		return ordinal;
	}

	public void setOrdinal(boolean ordinal) {
		this.ordinal = ordinal;
	}
	
	public boolean isNummod() {
		return nummod;
	}

	public void setNummod(boolean nummod) {
		this.nummod = nummod;
	}
	
	public boolean isCompositional() {
		return compositional;
	}

	public void setCompositional(boolean compositional) {
		this.compositional = compositional;
	}

	public WikipediaArticle getWiki() {
		return wiki;
	}

	public void setWiki(WikipediaArticle wiki) {
		this.wiki = wiki;
	}

	public Integer getCurId() {
		return curId;
	}

	public void setCurId(Integer curId) {
		this.curId = curId;
	}

	public boolean isIgnoreHigher() {
		return ignoreHigher;
	}

	public void setIgnoreHigher(boolean ignoreHigher) {
		this.ignoreHigher = ignoreHigher;
	}

	public boolean isIgnoreFreq() {
		return ignoreFreq;
	}

	public void setIgnoreFreq(boolean ignoreFreq) {
		this.ignoreFreq = ignoreFreq;
	}

	public List<Long> getFrequentNumbers() {
		return frequentNumbers;
	}

	public void setFrequentNumbers(List<Long> frequentNumbers) {
		this.frequentNumbers = frequentNumbers;
	}

	public int getIgnoreHigherLess() {
		return ignoreHigherLess;
	}

	public void setIgnoreHigherLess(int ignoreHigherLess) {
		this.ignoreHigherLess = ignoreHigherLess;
	}

	public double getCountDist() {
		return countDist;
	}

	public void setCountDist(double countDist) {
		this.countDist = countDist;
	}

	public int getMaxTripleCount() {
		return maxTripleCount;
	}

	public void setMaxTripleCount(int maxTripleCount) {
		this.maxTripleCount = maxTripleCount;
	}

	public double getCountInfThreshold() {
		return countInfThreshold;
	}

	public void setCountInfThreshold(double countInfThreshold) {
		this.countInfThreshold = countInfThreshold;
	}

	public boolean isNumterms() {
		return numterms;
	}

	public void setNumterms(boolean numterms) {
		this.numterms = numterms;
	}

	public boolean isArticles() {
		return articles;
	}

	public void setArticles(boolean articles) {
		this.articles = articles;
	}

	public boolean isQuantifiers() {
		return quantifiers;
	}

	public void setQuantifiers(boolean quantifiers) {
		this.quantifiers = quantifiers;
	}

	public boolean isPronouns() {
		return pronouns;
	}

	public void setPronouns(boolean pronouns) {
		this.pronouns = pronouns;
	}

	public boolean isNegation() {
		return negation;
	}

	public void setNegation(boolean negation) {
		this.negation = negation;
	}

	public boolean isNegTrain() {
		return negTrain;
	}

	public void setNegTrain(boolean negTrain) {
		this.negTrain = negTrain;
	}
}
