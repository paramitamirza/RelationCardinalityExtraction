package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	private boolean transform;
	private boolean transformZeroOne;
	
	public GenerateFeatures(String dirFeature, String relName,
			WikipediaArticle wiki, String wikidataId, String count, Integer curId, boolean training,
			boolean nummod, boolean compositional, int threshold,
			boolean transform, boolean transformZeroOne) {
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
		
		this.setTransform(transform);
		this.setTransformZeroOne(transformZeroOne);
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
	    			Document doc = new Document(l);
	    			
	    			for (Sentence s : doc.sentences()) {	//Split the sentences
	    				
	    				original = s.text();	    				
	    				sent = filter(original, this.isTraining(), trans, this.isTransform(), this.isTransformZeroOne());
	    				
	    				if (sent != null) {
	    					
	    					toPrint.append(generateFeatures(sent, j, numOfTriples, 
	    							this.isNummod(), this.isCompositional(), this.getThreshold()).toString());
	    				}
	    				
	    				j ++;
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
	
	private Sentence filter(String sentence, boolean training, 
			Transform trans, boolean transform, boolean transformZeroOne) throws IOException {
		
		Sentence sent;
		String transformed, transformedZeroOne;
		
		if (training) {
			if (transform || transformZeroOne) {
				transformed = trans.transform(sentence, false, false, true, true);
				sent = new Sentence(transformed);
				if (Numbers.containNumbers(transformed, sent, false, false))
					return sent;
				else
					return null;
			} else {
				sent = new Sentence(sentence);
				if (Numbers.containNumbers(sentence, sent, false, false))
					return sent;
				else
					return null;
			}
		} else {
			if (transform || transformZeroOne) {
				transformed = trans.transform(sentence, false, false, true, true);
				
				if (transformZeroOne) {
					transformedZeroOne = trans.transform(transformed, true, true, false, false);
					sent = new Sentence(transformedZeroOne);
					if (Numbers.containNumbers(transformedZeroOne, sent, false, false))
						return sent;
					else
						return null;
				} else {
					transformed = trans.transform(sentence, false, false, true, true);
					sent = new Sentence(transformed);
					if (Numbers.containNumbers(transformed, sent, false, false))
						return sent;
					else
						return null;
				}
			} else {
				sent = new Sentence(sentence);
				if (Numbers.containNumbers(sentence, sent, false, false))
					return sent;
				else
					return null;
			}
		}
	}
	
	private StringBuilder generateFeatures(Sentence sent, int j, int numOfTriples, 
			boolean nummod, boolean compositional, int threshold) {
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
//						} else if (numInt < numOfTriples) {
//							label = "_NO_";
//						} else if (numInt > numOfTriples) {
//							label = "_MAYBE_";
					} else {
						label = "O";
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
//							} else if (numInt < numOfTriples) {
//								label = "_NO_";
//							} else if (numInt > numOfTriples) {
//								label = "_MAYBE_";
						} else {
							label = "O";
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
		
		for (int t=0; t<tokenFeatures.size(); t++) {
			sb.append(tokenFeatures.get(t) + "\t" + labels.get(t));
			sb.append(System.getProperty("line.separator"));
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

	public boolean isTransformZeroOne() {
		return transformZeroOne;
	}

	public void setTransformZeroOne(boolean transformZeroOne) {
		this.transformZeroOne = transformZeroOne;
	}
}
