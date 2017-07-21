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
	private boolean compositional;
	
	private int threshold;
	private double countDist;
	
	private boolean transform;
	private boolean transformZero;
	private boolean transformOne;
	
	private boolean ignoreHigher;
	private boolean ignoreFreq;
	private int ignoreHigherLess;
	private int maxTripleCount;
	
	private List<Long> frequentNumbers;
	
	public GenerateFeatures(String dirFeature, String relName,
			WikipediaArticle wiki, String wikidataId, String count, Integer curId, 
			String freqNum,
			boolean training,
			boolean nummod, boolean compositional, 
			int threshold, String countDist,
			boolean transform, boolean transformZero, boolean transformOne,
			boolean ignoreHigher, int ignoreHigherLess,
			boolean ignoreFreq, int maxCount) {
		this.setDirFeature(dirFeature);
		this.setRelName(relName);
		
		this.setWiki(wiki);
		
		this.setWikidataId(wikidataId);
		this.setCount(count);
		this.setCurId(curId);
		
		this.setTraining(training);
		
		this.setNummod(nummod);
		this.setCompositional(compositional);
		
		this.setThreshold(threshold);
		this.setCountDist(Double.parseDouble(countDist));
		
		this.setTransform(transform);
		this.setTransformZero(transformZero);
		this.setTransformOne(transformOne);
		
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
		    				sent = filter(original, this.isTraining(), trans);
		    				
		    				if (sent != null) {
		    					
		    					toPrint.append(generateFeatures(sent, j, numOfTriples, 
		    							this.isNummod(), this.isCompositional(), 
		    							this.getThreshold(), this.getCountDist(),
		    							this.isIgnoreHigher(), this.getIgnoreHigherLess(),
		    							this.isIgnoreFreq(), this.getMaxTripleCount()).toString());
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
	
	private Sentence filter(String sentence, boolean training, Transform trans) throws IOException {
		
		Sentence sent;
		String sentStr;
		
		if (training) {
			sentStr = sentence;
			if (this.isTransform()) {
				sentStr = trans.transform(sentStr, false, false, this.isTransform(), this.isTransform());
			}
			sent = new Sentence(sentStr);
			if (Numbers.containNumbers(sentStr, sent, false, false))
				return sent;
			else
				return null;
			
		} else {
			sentStr = sentence;
			if (this.isTransform() || this.isTransformZero() || this.isTransformOne()) {
				sentStr = trans.transform(sentStr, this.isTransformOne(), this.isTransformZero(), this.isTransform(), this.isTransform());
			}
			sent = new Sentence(sentStr);
			if (Numbers.containNumbers(sentStr, sent, false, false))
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
	
	private StringBuilder generateFeatures(Sentence sent, int j, int numOfTriples, 
			boolean nummod, boolean compositional, 
			int threshold, double countDist,
			boolean ignoreHigher, int ignoreHigherLess,
			boolean ignoreFreq, int maxTripleCount) {
		String word = "", lemma = "", pos = "", ner = "", deprel = "", label = "";
		StringBuilder sb = new StringBuilder();
		int k;
		boolean lrb = false;
		
		List<Integer> idxToAdd = new ArrayList<Integer>();
		long numToAdd = 0;
		
		List<String> labels = new ArrayList<String>();
		List<String> tokenFeatures = new ArrayList<String>();
		int tokenIdx = 0;
		
		long numInt;
		
		double countInfThreshold = 0.0;
		if (threshold > 0) {
			if (threshold == 1) countInfThreshold = 0.75;
			if (threshold == 2) countInfThreshold = 1;
			if (threshold == 3) countInfThreshold = 1.25;
			if (threshold == 4) countInfThreshold = 1.5;
		}
		
		for (k=0; k<sent.words().size(); k++) {
			pos = sent.posTag(k);
			ner = sent.nerTag(k);
			deprel = "O";
			if (sent.incomingDependencyLabel(k).isPresent()) {
				deprel = sent.incomingDependencyLabel(k).get();
			}
			label = "O";
			
			if (sent.word(k).startsWith("LatinGreek_")) {
				word = sent.word(k).split("_")[0] + "_" + sent.word(k).split("_")[1] + "_" + sent.word(k).split("_")[2];
				lemma = "_" + sent.word(k).split("_")[3] + "_";
				
				numInt = Long.parseLong(sent.word(k).split("_")[2]);
				
				if (compositional) {
					if (numToAdd > 0) {
						if (numInt == numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								) {
							if (-Math.log(countDist) >= countInfThreshold) { //numOfTriples > threshold
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
							numToAdd = 0;
							idxToAdd.clear();
							
						} else {
							if ((numToAdd+numInt) == numOfTriples
//									&& ((nummod && deprel.startsWith("nummod"))
//											|| !nummod)
									) {
								label = "_YES_";
								for (Integer nnn : idxToAdd) labels.set(nnn, "_YES_");
								numToAdd = 0;
								idxToAdd.clear();
							} else if ((numToAdd+numInt) < numOfTriples
//									&& ((nummod && deprel.startsWith("nummod"))
//											|| !nummod)
									) {
								label = "_NO_";
								numToAdd += numInt;
								idxToAdd.add(tokenIdx);
							} else {	//(numToAdd+numInt) > numOfTriples
								if (((ignoreHigherLess > 0) 
												&& ((numToAdd+numInt) <= (numOfTriples + ignoreHigherLess))
												&& ((numToAdd+numInt) <= maxTripleCount)
												)
										|| ((ignoreHigherLess == 0)
												&& ((numToAdd+numInt) <= maxTripleCount)
												)
								) {
									label = "_MAYBE_";
								} else {
									label = "_NO_";
								}
								numToAdd = 0;
								idxToAdd.clear();
							}
						}
						
					} else {
						if (numInt == numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								) {
							if (-Math.log(countDist) >= countInfThreshold) { //numOfTriples > threshold
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
							
						} else if (numInt < numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								) {
							label = "_NO_";
							numToAdd += numInt;
							idxToAdd.add(tokenIdx);
							
						} else if (numInt > numOfTriples
//								&& ((nummod && deprel.startsWith("nummod"))
//										|| !nummod)
								){		
							
							if (((ignoreHigherLess > 0) 
											&& (numInt <= (numOfTriples + ignoreHigherLess))
											&& (numInt <= maxTripleCount)
											)
									|| ((ignoreHigherLess == 0)
											&& (numInt <= maxTripleCount)
											)
							) {
								label = "_MAYBE_";
							} else {
								label = "_NO_";
							}
							
						} else {
							label = "_NO_";
						}
					}
					
				} else {
					if (numInt == numOfTriples
//							&& ((nummod && deprel.startsWith("nummod"))
//									|| !nummod)
							) {
						
						if (-Math.log(countDist) >= countInfThreshold) { //numOfTriples > threshold
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
						
					} else if (numInt > numOfTriples
//							&& ((nummod && deprel.startsWith("nummod"))
//									|| !nummod)
							) {	
						
						if (((ignoreHigherLess > 0) 
										&& (numInt <= (numOfTriples + ignoreHigherLess))
										&& (numInt <= maxTripleCount)
										)
								|| ((ignoreHigherLess == 0)
										&& (numInt <= maxTripleCount)
										)
						) {
							label = "_MAYBE_";
						} else {
							label = "_NO_";
						}

					} else {	//numInt < numOfTriples
						label = "_NO_";
					}
				}
				
//				sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
//				sb.append(System.getProperty("line.separator"));
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
									) {
								if (-Math.log(countDist) >= countInfThreshold) { //numOfTriples > threshold
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
								numToAdd = 0;
								idxToAdd.clear();
								
							} else {
								if ((numToAdd+numInt) == numOfTriples
										&& ((nummod && deprel.startsWith("nummod"))
												|| !nummod)
										) {
									label = "_YES_";
									for (Integer nnn : idxToAdd) labels.set(nnn, "_YES_");
									numToAdd = 0;
									idxToAdd.clear();
								} else if ((numToAdd+numInt) < numOfTriples
										&& ((nummod && deprel.startsWith("nummod"))
												|| !nummod)
										) {
									label = "_NO_";
									numToAdd += numInt;
									idxToAdd.add(tokenIdx);
								} else {	//(numToAdd+numInt) > numOfTriples
									if (((ignoreHigherLess > 0) 
													&& ((numToAdd+numInt) <= (numOfTriples + ignoreHigherLess))
													&& ((numToAdd+numInt) <= maxTripleCount)
													)
											|| ((ignoreHigherLess == 0)
													&& ((numToAdd+numInt) <= maxTripleCount)
													)
									) {
										label = "_MAYBE_";
									} else {
										label = "_NO_";
									}
									numToAdd = 0;
									idxToAdd.clear();
								}
							}
							
						} else {
							if (numInt == numOfTriples
									&& ((nummod && deprel.startsWith("nummod"))
											|| !nummod)
									) {
								if (-Math.log(countDist) >= countInfThreshold) { //numOfTriples > threshold
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
								
							} else if (numInt < numOfTriples
									&& ((nummod && deprel.startsWith("nummod"))
											|| !nummod)
									) {
								label = "_NO_";
								numToAdd += numInt;
								idxToAdd.add(tokenIdx);
								
							} else if (numInt > numOfTriples
									&& ((nummod && deprel.startsWith("nummod"))
											|| !nummod)
									){
								if (((ignoreHigherLess > 0) 
												&& (numInt <= (numOfTriples + ignoreHigherLess))
												&& (numInt <= maxTripleCount)
												)
										|| ((ignoreHigherLess == 0)
												&& (numInt <= maxTripleCount)
												)
								) {
									label = "_MAYBE_";
								} else {
									label = "_NO_";
								}
								
							} else {
								label = "_NO_";
							}
						}
						
					} else {
						if (numInt == numOfTriples
								&& ((nummod && deprel.startsWith("nummod"))
										|| !nummod)
								) {
							
							if (-Math.log(countDist) >= countInfThreshold) { //numOfTriples > threshold
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
							
						} else if (numInt > numOfTriples
								&& ((nummod && deprel.startsWith("nummod"))
										|| !nummod)
								) {	
							if (((ignoreHigherLess > 0) 
											&& (numInt <= (numOfTriples + ignoreHigherLess))
											&& (numInt <= maxTripleCount)
											)
									|| ((ignoreHigherLess == 0)
											&& (numInt <= maxTripleCount)
											)
							) {
								label = "_MAYBE_";
							} else {
								label = "_NO_";
							}

						} else {	//numInt < numOfTriples
							label = "_NO_";
						}
					}
				}
				
//				sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
//				sb.append(System.getProperty("line.separator"));
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
				
//					sb.append(generateLine(wikidataId, j+"", k+"", word.substring(0, word.length()-1), lemma, pos, ner, deprel.substring(0, deprel.length()-1), label));
//					sb.append(System.getProperty("line.separator"));
				k--;
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word.substring(0, word.length()-1), lemma, pos, ner, deprel.substring(0, deprel.length()-1)));
				labels.add(label);
				tokenIdx ++;
				
				word = ""; lemma = ""; deprel = "";
				
			} else {							
				word = sent.word(k);
				lemma = sent.lemma(k);
//					sb.append(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel, label));
//					sb.append(System.getProperty("line.separator"));
				tokenFeatures.add(generateLine(wikidataId, j+"", k+"", word, lemma, pos, ner, deprel));
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
	
	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
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

	public boolean isTransform() {
		return transform;
	}

	public void setTransform(boolean transform) {
		this.transform = transform;
	}

	public boolean isTransformZero() {
		return transformZero;
	}

	public void setTransformZero(boolean transformZero) {
		this.transformZero = transformZero;
	}

	public boolean isIgnoreHigher() {
		return ignoreHigher;
	}

	public void setIgnoreHigher(boolean ignoreHigher) {
		this.ignoreHigher = ignoreHigher;
	}

	public boolean isTransformOne() {
		return transformOne;
	}

	public void setTransformOne(boolean transformOne) {
		this.transformOne = transformOne;
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
}
