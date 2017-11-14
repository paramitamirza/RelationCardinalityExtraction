package de.mpg.mpiinf.cardinality.autoextraction.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.mpg.mpiinf.cardinality.autoextraction.Numbers;
import de.mpg.mpiinf.cardinality.autoextraction.WikidataLabel;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

public class Evaluation {
	
	public static void main(String[] args) throws IOException {
		
		Options options = getEvalOptions();
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
            
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			formatter.printHelp("RelationCardinalityExtraction: Evaluation", options);
			
			System.exit(1);
			return;
		}
		
		String csvPath = cmd.getOptionValue("input");
		String allPath = csvPath;
		if (cmd.hasOption("all")) allPath = cmd.getOptionValue("all");
		
		String delimiter = ",";
		if (cmd.hasOption("tab")) delimiter = "\t";
		
		String crfOutPath = cmd.getOptionValue("crfout");
		String relName = cmd.getOptionValue("relname");
		String outputPath = null;
		if (cmd.hasOption("o")) {
			outputPath = cmd.getOptionValue("output");
		}
		String resultPath = null;
		if (cmd.hasOption("r")) {
			resultPath = cmd.getOptionValue("result");
		}
		
		Evaluation eval = new Evaluation();
//		String[] labels = {"O", "_YES_"};
		String[] labels = {"_YES_", "O"};
		boolean compositional = cmd.hasOption("compositional");
		boolean ordinals = cmd.hasOption("ordinals");
		
		float minConfScore = (float)-1.0;
		if (cmd.hasOption("v")) minConfScore = Float.parseFloat(cmd.getOptionValue("confidence"));
		
		float zScore = (float)100.0;
		if (cmd.hasOption("z")) zScore = Float.parseFloat(cmd.getOptionValue("zscore"));
		
		boolean relaxed = false;
		if (cmd.hasOption("x")) relaxed = true;
		
		boolean zero = false;
		if (cmd.hasOption("zero")) zero = true;
		
		boolean label = false;
		if (cmd.hasOption("label")) label = true;
		
		eval.evaluate(relName, csvPath, allPath, delimiter, 
				crfOutPath, labels, 
				outputPath, resultPath, 
				compositional, false, ordinals, zero,
				minConfScore, zScore, 0, relaxed,
				label);
	}
	
	public static Options getEvalOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input evaluation file (.csv) path");
		input.setRequired(true);
		options.addOption(input);
		
		Option all = new Option("a", "all", true, "Input 'all entities' file (.csv) path");
		all.setRequired(false);
		options.addOption(all);
		
		Option tab = new Option("tab", "tab", false, "Tab separated input files");
		tab.setRequired(false);
		options.addOption(tab);
		
		Option label = new Option("label", "label", false, "Print property/relation and class labels");
		label.setRequired(false);
		options.addOption(label);
		
		Option relName = new Option("p", "relname", true, "Property/relation name");
		relName.setRequired(true);
		options.addOption(relName);
        
		Option crfout = new Option("f", "crfout", true, "CRF++ output file (.out) path");
		crfout.setRequired(true);
		options.addOption(crfout);
        
		Option output = new Option("o", "output", true, "Output file (.csv) path");
		output.setRequired(false);
		options.addOption(output);
		
		Option result = new Option("r", "result", true, "Performance result file path");
		result.setRequired(false);
		options.addOption(result);
		
		Option compositional = new Option("compositional", "compositional", false, "Label compositional numbers as true examples");
		compositional.setRequired(false);
		options.addOption(compositional);
		
		Option ordinal = new Option("ordinals", "ordinals", false, "Consider ordinals as candidates");
		ordinal.setRequired(false);
		options.addOption(ordinal);
		
		Option transformZero = new Option("0", "zero", false, "Consider 0 count prediction");
		transformZero.setRequired(false);
		options.addOption(transformZero);
		
		Option minConfScore = new Option("v", "confidence", true, "Minimum confidence score");
		minConfScore.setRequired(false);
		options.addOption(minConfScore);
		
		Option zScoreRange = new Option("z", "zscore", true, "Maximum range of z-score");
		zScoreRange.setRequired(false);
		options.addOption(zScoreRange);
		
		Option relaxedMatch = new Option("x", "relaxed", false, "Relaxed match to the triple count (less than count is considered correct)");
		relaxedMatch.setRequired(false);
		options.addOption(relaxedMatch);
        
		return options;
	}
	
	private int findTotalNumberOfComposition(Map<Integer, String> numbers) {
		long pivot, number, total;
		int pivotIdx;
		Object[] numIdxs = numbers.keySet().toArray();
		for (int i=0; i<numIdxs.length; i++) {
			pivotIdx = (int)numIdxs[i];
			pivot = Long.parseLong(numbers.get(pivotIdx).split("#")[0]);
			total = 0;
			for (int j=i+1; j<numIdxs.length; j++) {
				number = Long.parseLong(numbers.get((int)numIdxs[j]).split("#")[0]);
				if (number > pivot) break;
				else if (number == pivot) break;
				else {
					total += number;
					if (total == pivot) {
						return pivotIdx;
					}
				}
			}
			
		}
		return -999;
	}
	
	private boolean conjExist(List<String> sentence, int startIdx, int endIdx) {
		for (int i=startIdx; i<endIdx; i++) {
			if (sentence.get(i).toLowerCase().equals(",")
//					|| sentence.get(i).toLowerCase().equals(";")
					|| sentence.get(i).toLowerCase().equals("and")
					) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isQuantifier(List<String> sentence, int wIdx) {
		String word = sentence.get(wIdx).toLowerCase();
		if (word.equals("both")
				|| word.equals("some")
				|| word.equals("few")
				|| word.equals("many")
				|| word.equals("several")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isPossPronouns(List<String> sentence, int wIdx) {
		String word = sentence.get(wIdx).toLowerCase();
		if (word.equals("my")
				|| word.equals("your")
				|| word.equals("his")
				|| word.equals("her")
				|| word.equals("its")
				|| word.equals("their")
				|| word.equals("our")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	private DescriptiveStatistics getDescriptiveStatistics(String crfOutPath, String[] labels, long maxNum) throws IOException {
		BufferedReader br; String line;
		
		//Read result (.out) file
		br = new BufferedReader(new FileReader(crfOutPath));
		
		Double prob = 0.0;
		String[] cols;
		double threshold = 0.1;
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		line = br.readLine();	//CRF sentence score
		
		while (line != null) {
			if (!line.trim().equals("") && !line.startsWith("#")) {
				cols = line.split("\t");
				for (int l=0; l<labels.length; l++) {
					if (labels[l].equals("_YES_")) {
						prob = Double.valueOf(cols[cols.length-labels.length+l].split("/")[1]);
					}
				}
				if (prob > threshold 
						&& cols[4].equals("_num_")) {
					stats.addValue(prob);
				}
			}
			
			line = br.readLine();
		}
		br.close();
		
		return stats;
	}
	
	private DescriptiveStatistics getDescriptiveStatisticsOrdinals(String crfOutPath, String[] labels, long maxNum) throws IOException {
		BufferedReader br; String line;
		
		//Read result (.out) file
		br = new BufferedReader(new FileReader(crfOutPath));
		
		Double prob = 0.0;
		String[] cols;
		double threshold = 0.1;
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		line = br.readLine();	//CRF sentence score
		
		while (line != null) {
			if (!line.trim().equals("") && !line.startsWith("#")) {
				cols = line.split("\t");
				for (int l=0; l<labels.length; l++) {
					if (labels[l].equals("_YES_")) {
						prob = Double.valueOf(cols[cols.length-labels.length+l].split("/")[1]);
					}
				}
				if (prob > threshold 
						&& cols[4].equals("_ord_")) {
					stats.addValue(prob);
				}
			}
			
			line = br.readLine();
		}
		br.close();
		
		return stats;
	}
	
	private double getMedian(DescriptiveStatistics stats) {
		int size = stats.getSortedValues().length;
		if (stats.getSortedValues().length% 2 == 0)
		    return ((double) stats.getSortedValues()[size/2] + (double) stats.getSortedValues()[size/2 - 1]) / 2;
		else
		    return (double) stats.getSortedValues()[size/2];
	}
	
	private double getMAD(DescriptiveStatistics stats) {
		DescriptiveStatistics st = new DescriptiveStatistics();
		
		double median = getMedian(stats);
		
		for (double d : stats.getValues()) {
			st.addValue(Math.abs(d - median));
		}
		
		return getMedian(st);
	}
	
	public void evaluate(String relName, String csvPath, String allPath,
			String delimiter, String crfOutPath, 
			String[] labels, String outPath, String resultPath,
			boolean addSameSentence, boolean addDiffSentence,
			boolean addOrdinals, boolean addZero,
			float tConf, float zRange, long trainSize, boolean relaxedMatch,
			boolean label) throws IOException {
		
		long startTime = System.currentTimeMillis();
		
		System.out.print("Evaluate CRF++ output file... ");
		
		//Read .csv file
		BufferedReader br; 
		String line;
		
		Map<String, Integer> instanceNum = new HashMap<String, Integer>();
		Map<String, String> instanceCurId = new HashMap<String, String>();
		Map<String, String> instanceLabel = new HashMap<String, String>();
		
		int num = 0;
		long maxNum = 0;
		
		br = new BufferedReader(new FileReader(csvPath));
		line = br.readLine();
		while (line != null) {
			num = Integer.parseInt(line.split(delimiter)[1]);
			instanceNum.put(line.split(delimiter)[0], num);
			if (num > maxNum) maxNum = num;
			instanceCurId.put(line.split(delimiter)[0], line.split(delimiter)[2]);
			instanceLabel.put(line.split(delimiter)[0], line.split(delimiter)[3]);
			line = br.readLine();
		}
		br.close();
		
		br = new BufferedReader(new FileReader(allPath));
		line = br.readLine();
		while (line != null) {
			instanceCurId.put(line.split(delimiter)[0], line.split(delimiter)[2]);
			instanceLabel.put(line.split(delimiter)[0], line.split(delimiter)[3]);
			line = br.readLine();
		}
		br.close();
		
		boolean zscore = true;
		
		DescriptiveStatistics dstats = new DescriptiveStatistics();
		DescriptiveStatistics dstatsO = new DescriptiveStatistics();
		double median = 0.0, mad = 0.0;
		double medianO = 0.0, madO = 0.0;
		
		dstats = getDescriptiveStatistics(crfOutPath, labels, maxNum);
		if (dstats.getSortedValues().length > 0) {
			median = getMedian(dstats);
			mad = getMAD(dstats);
		}
		if (addOrdinals) {
			dstatsO = getDescriptiveStatisticsOrdinals(crfOutPath, labels, maxNum);
			if (dstatsO.getSortedValues().length > 0) {
				medianO = getMedian(dstatsO);
				madO = getMAD(dstatsO);
			}
		} 
		
		//Read result (.out) file
		br = new BufferedReader(new FileReader(crfOutPath));
		BufferedWriter bw = null;
		if (outPath != null) {
			bw = new BufferedWriter(new FileWriter(outPath));
		}
		
		Double prob = 0.0;
		List<String> nums = new ArrayList<String>();
		List<Double> probs = new ArrayList<Double>();
		
		List<String> ords = new ArrayList<String>();
		List<Double> oprobs = new ArrayList<Double>();
		
		int tp = 0;
		int fp = 0;
		int complete = 0, incomplete = 0, less = 0;
		int available = 0, missing = 0;
		int total = 0, ctotal = 0;
//		double threshold = minConfScore;
		double threshold = 0.1;
		
		String[] cols;
		List<String> sentence = new ArrayList<String>();
		String entityId = null;
		
		long predictedCardinal = 0, predictedOrdinal = 0;
		double predictedProb = 0.0, predictedProbZ = 0.0, predictedProbS = 0.0;
		double predictedOProb = 0.0, predictedOProbZ = 0.0, predictedOProbS = 0.0;
		int numPredicted = 0;
		String evidence = "", evidenceo = "";
		
		Set<String> entities = new HashSet<String>();
		
		line = br.readLine();	//CRF sentence score
		
		while (line != null) {
			
			if(!StringUtils.join(nums, "").equals("")
					|| !StringUtils.join(ords, "").equals("")) {
				
				Map<Integer, String> numbers = extractNumber(nums, probs, maxNum);
				Map<Integer, String> ordinals = extractNumber(ords, oprobs, maxNum);
				
				long n = 0, no = 0;
				double p = 0.0, pp;
				double po = 0.0, ppo;
				int m = 0, mm;
				int mo = 0, mmo;
				List<Integer> mlist = new ArrayList<Integer>();
				List<Integer> mlisto = new ArrayList<Integer>();
				
				if (!numbers.isEmpty()) {
				
					if (addSameSentence) {	
						//When there are more than one in a sentence:
						
						//if a number is a total of its following sequence of numbers, choose the total
						int totalIdx = findTotalNumberOfComposition(numbers);
						
						if (totalIdx > 0) {
							pp = Double.parseDouble(numbers.get(totalIdx).split("#")[1]);
							if (pp > threshold) {
								p = pp;
								n = Integer.parseInt(numbers.get(totalIdx).split("#")[0]);
								mlist.add(totalIdx);
							}
						
						} else {
							//else, add them up if there exists conjunction (comma, semicolon or 'and') in between
							
							Object[] keys = numbers.keySet().toArray();
							pp = Double.parseDouble(numbers.get(keys[0]).split("#")[1]);
							mm = (Integer)keys[0];
							if (pp > threshold) {	
								n = Long.parseLong(numbers.get(keys[0]).split("#")[0]);
								mlist.add(mm);
								p = pp;
							}
							
							if (keys.length > 1) {						
								for (int k = 1; k < keys.length; k++) {
									pp = Double.parseDouble(numbers.get(keys[k]).split("#")[1]);
									mm = (Integer)keys[k];	
									
									if (pp > threshold) {
										if (mlist.isEmpty()) {
											n = Long.parseLong(numbers.get(keys[k]).split("#")[0]);
											mlist.add(mm);
											p = pp;
											
										} else {
											if (conjExist(sentence, mlist.get(mlist.size()-1), mm)
													&& (mm - mlist.get(mlist.size()-1)) <= 5 
													&& !isQuantifier(sentence, mm)
													&& !isPossPronouns(sentence, mm)
													) {
												if (pp > p) p = pp;
	//											p += pp;
												n += Long.parseLong(numbers.get(keys[k]).split("#")[0]);
												mlist.add(mm);
											
											} else {
												if (pp > p) {
													mlist.clear();
													n = Long.parseLong(numbers.get(keys[k]).split("#")[0]);
													mlist.add(mm);
													p = pp;
												}
											}
										}									
									}
								}							
							} 
	//						p = p/numbers.size();
						}
						
					} else {	
						//When there are more than one in a sentence, choose the most probable
						for (Integer key : numbers.keySet()) {
							pp = Double.parseDouble(numbers.get(key).split("#")[1]);
							mm = key;
							if (pp > p
									&& pp > threshold) {
								n = Long.parseLong(numbers.get(key).split("#")[0]);
								p = pp;
								m = mm;
							}
						}
						mlist.add(m);
					}
				}
				
				if (addOrdinals && !ordinals.isEmpty()) {
					
					//When there are more than one in a sentence, choose the most probable
					for (Integer key : ordinals.keySet()) {
						ppo = Double.parseDouble(ordinals.get(key).split("#")[1]);
						mmo = key;
						if (ppo > threshold) {
							if (ppo > po) {
								no = Long.parseLong(ordinals.get(key).split("#")[0]);
								po = ppo;
								mo = mmo;
							} else if (ppo == po) {
								if (Long.parseLong(ordinals.get(key).split("#")[0]) >= no) {
									no = Long.parseLong(ordinals.get(key).split("#")[0]);
									po = ppo;
									mo = mmo;
								}
							}
						}
					}
					mlisto.add(mo);
				}
				
//				if (addDiffSentence) {	
//					//When there are more than one sentences, add them up
//					predictedCardinal += n;
//					predictedProb += p;
//					evidence += wordsToSentence(sentence, mlist) + "|";
//					numPredicted++;
//					predictedProb = predictedProb / numPredicted;
//					
//				} else {
					//When there are more than one sentences, choose the most probable
					if (p > predictedProb) {
						predictedCardinal = n;
						predictedProb = p;
						evidence = wordsToSentence(sentence, mlist);
					}
					
					if (addOrdinals && po > predictedOProb) {
						predictedOrdinal = no;
						predictedOProb = po;
						evidenceo = wordsToSentence(sentence, mlisto);
					}
//				}
			}
			
			//Sentence starts			
			
			nums = new ArrayList<String>();
			probs = new ArrayList<Double>();
			
			ords = new ArrayList<String>();
			oprobs = new ArrayList<Double>();
			
			sentence = new ArrayList<String>();
			
			line = br.readLine();
			
			while (line != null && !line.trim().equals("")) {
				cols = line.split("\t");
				
				if (entityId != null && !cols[0].equals(entityId)
						&& !entities.contains(entityId)
						) {	//Entity ends
					int numChild = 0;
					if (instanceNum.containsKey(entityId)) numChild = instanceNum.get(entityId);
					String wikiCurid = instanceCurId.get(entityId);
					String wikiLabel = instanceLabel.get(entityId);
					
					predictedProbZ = 0.0; predictedProbS = 0.0;
					if (predictedProb > 0 && mad > 0.0) predictedProbZ = 0.6745 * (predictedProb - median) / mad;	//modified z-score: normalize the probability score!						
					if (predictedProb > 0) predictedProbS = (predictedProb - dstats.getMin()) / (dstats.getMax() - dstats.getMin());	//rescaling: normalize the probability score!
						
					predictedOProbZ = 0.0; predictedOProbS = 0.0;
					if (addOrdinals) {
						if (predictedOProb > 0 && madO > 0.0) predictedOProbZ = 0.6745 * (predictedOProb - medianO) / madO;	//modified z-score: normalize the probability score!						
						if (predictedOProb > 0) predictedOProbS = (predictedOProb - dstatsO.getMin()) / (dstatsO.getMax() - dstatsO.getMin());	//rescaling: normalize the probability score!
						
//						System.out.println("cardinal::: " + predictedCardinal + ":" + predictedProbS + ":" + evidence);
//						System.out.println("ordinal::: " + predictedOrdinal + ":" + predictedOProbS + ":" + evidenceo);
						
						if (predictedOProbS > predictedProbS
//								&& predictedOrdinal > predictedCardinal
								) {
							predictedCardinal = predictedOrdinal;
							predictedProb = predictedOProb;
							predictedProbS = predictedOProbS;
							predictedProbZ = predictedOProbZ;
							evidence = evidenceo;
						}
					}
					
					if (bw != null) {
						if (
								(tConf < 0 && predictedProb > 0)
								||
								(tConf >= 0 && zRange >= 100.0 && predictedProbS > tConf)
								||
								(tConf >= 0 && zRange < 100.0 && predictedProbS > tConf && predictedProbZ <= zRange && predictedProbZ >= -zRange)
								){
//							System.out.println("final::: " + predictedCardinal + ":" + predictedProbS + ":" + evidence);
							bw.write(entityId + "\t"
									+ "https://en.wikipedia.org/wiki?curid=" + wikiCurid + "\t"
									+ java.net.URLDecoder.decode(wikiLabel, "UTF-8") + "\t" 
									+ numChild + "\t" 
									+ predictedCardinal + "\t" 
									+ predictedProb + "\t" 
									+ evidence);
							bw.newLine();
//						} else {
//							bw.write(entityId + "\t" 
//									+ "https://en.wikipedia.org/wiki?curid=" + wikiCurid + "\t" 
//									+ java.net.URLDecoder.decode(wikiLabel, "UTF-8") + "\t" 
//									+ numChild + "\t" + 0 + "\t" + 0 + "\t" + "");
						}
					}
					if (numChild > 0
							|| (addZero && numChild == 0)) {
						
						if (
								(tConf < 0 && predictedProb > 0)
								||
								(tConf >= 0 && zRange >= 100.0 && predictedProbS > tConf)
								||
								(tConf >= 0 && zRange < 100.0 && predictedProbS > tConf && predictedProbZ <= zRange && predictedProbZ >= -zRange)
								){
							if (relaxedMatch) {
								if (numChild >= predictedCardinal && predictedCardinal > 0) tp ++;
								else if (numChild < predictedCardinal && predictedCardinal > 0) fp ++;
								
							} else {
								if (numChild == predictedCardinal) tp ++;
								else if (numChild != predictedCardinal && predictedCardinal > 0) fp ++;
							}
						}
						total ++;
					}
					if (numChild >= 0) {
						available += numChild;
						
						if (
								(tConf < 0 && predictedProb > 0)
								||
								(tConf >= 0 && zRange >= 100.0 && predictedProbS > tConf)
								||
								(tConf >= 0 && zRange < 100.0 && predictedProbS > tConf && predictedProbZ <= zRange && predictedProbZ >= -zRange)
								){
							if (predictedCardinal == numChild) {
								complete ++;
							} else if (predictedCardinal > numChild) {
								incomplete ++;
								missing += predictedCardinal - numChild;
							} else {
								less ++;
							}
						}
					}
					ctotal ++;
					
					entities.add(entityId);
					
					predictedCardinal = 0;
					predictedProb = 0.0;
					evidence = "";
					
					predictedOrdinal = 0;
					predictedOProb = 0.0;
					evidenceo = "";
					
					numPredicted = 0;
				}
				
				entityId = cols[0];
				
				if (cols[4].equals("_propernoun_")) {
					sentence.add(cols[3].replaceAll("_", " "));
				
				} else if (cols[3].startsWith("PRP$_")) {
					sentence.add(cols[3].split("_")[2]);
					
				} else {
					sentence.add(cols[3]);
				}
				
				for (int l=0; l<labels.length; l++) {
					if (labels[l].equals("_YES_")) {
						prob = Double.valueOf(cols[cols.length-labels.length+l].split("/")[1]);
					}
				}
				
				if (prob > threshold) {
					if (addOrdinals) {
						if (cols[4].equals("_ord_")) {
							ords.add(cols[3]);
							oprobs.add(prob);
							nums.add("");
							probs.add(prob);
						} else {
							ords.add("");
							oprobs.add(prob);
							nums.add(cols[3]);
							probs.add(prob);
						}
					} else {
						nums.add(cols[3]);
						probs.add(prob);
					}
					
				} else {
					nums.add("");
					probs.add(0.0);
					if (addOrdinals) {
						ords.add("");
						oprobs.add(prob);
					}
				}
				
				line = br.readLine();
			}
			
			line = br.readLine();
			
		}
		
		//Last entity
		int numChild = 0;
		if (instanceNum.containsKey(entityId)) numChild = instanceNum.get(entityId);
		String wikiCurid = instanceCurId.get(entityId);
		String wikiLabel = instanceLabel.get(entityId);
		
		predictedProbZ = 0.0; predictedProbS = 0.0;
		if (predictedProb > 0) predictedProbZ = 0.6745 * (predictedProb - median) / mad;	//modified z-score: normalize the probability score!						
		if (predictedProb > 0) predictedProbS = (predictedProb - dstats.getMin()) / (dstats.getMax() - dstats.getMin());	//rescaling: normalize the probability score!
			
		predictedOProbZ = 0.0; predictedOProbS = 0.0;
		if (addOrdinals) {
			if (predictedOProb > 0 && madO > 0.0) predictedOProbZ = 0.6745 * (predictedOProb - medianO) / madO;	//modified z-score: normalize the probability score!						
			if (predictedOProb > 0) predictedOProbS = (predictedOProb - dstatsO.getMin()) / (dstatsO.getMax() - dstatsO.getMin());	//rescaling: normalize the probability score!
			
//			System.out.println("cardinal::: " + predictedCardinal + ":" + predictedProbS + ":" + evidence);
//			System.out.println("ordinal::: " + predictedOrdinal + ":" + predictedOProbS + ":" + evidenceo);
			
			if (predictedOProbS > predictedProbS
//					&& predictedOrdinal > predictedCardinal
					) {
				predictedCardinal = predictedOrdinal;
				predictedProb = predictedOProb;
				predictedProbS = predictedOProbS;
				predictedProbZ = predictedOProbZ;
				evidence = evidenceo;
			}
		}
		
		if (bw != null) {
			if (
					(tConf < 0 && predictedProb > 0)
					||
					(tConf >= 0 && zRange >= 100.0 && predictedProbS > tConf)
					||
					(tConf >= 0 && zRange < 100.0 && predictedProbS > tConf && predictedProbZ <= zRange && predictedProbZ >= -zRange)
					){
//				System.out.println("final::: " + predictedCardinal + ":" + predictedProbS + ":" + evidence);
				bw.write(entityId + "\t"
						+ "https://en.wikipedia.org/wiki?curid=" + wikiCurid + "\t"
						+ java.net.URLDecoder.decode(wikiLabel, "UTF-8") + "\t" 
						+ numChild + "\t" 
						+ predictedCardinal + "\t" 
						+ predictedProb + "\t" 
						+ evidence);
				bw.newLine();
//			} else {
//				bw.write(entityId + "\t" 
//						+ "https://en.wikipedia.org/wiki?curid=" + wikiCurid + "\t" 
//						+ java.net.URLDecoder.decode(wikiLabel, "UTF-8") + "\t" 
//						+ numChild + "\t" + 0 + "\t" + 0 + "\t" + "");
			}
		}
		if (numChild > 0
				|| (addZero && numChild == 0)) {
			
			if (
					(tConf < 0 && predictedProb > 0)
					||
					(tConf >= 0 && zRange >= 100.0 && predictedProbS > tConf)
					||
					(tConf >= 0 && zRange < 100.0 && predictedProbS > tConf && predictedProbZ <= zRange && predictedProbZ >= -zRange)
					){
				if (relaxedMatch) {
					if (numChild >= predictedCardinal && predictedCardinal > 0) tp ++;
					else if (numChild < predictedCardinal && predictedCardinal > 0) fp ++;
					
				} else {
					if (numChild == predictedCardinal) tp ++;
					else if (numChild != predictedCardinal && predictedCardinal > 0) fp ++;
				}
			}
			total ++;
		}
		if (numChild >= 0) {
			available += numChild;
			
			if (
					(tConf < 0 && predictedProb > 0)
					||
					(tConf >= 0 && zRange >= 100.0 && predictedProbS > tConf)
					||
					(tConf >= 0 && zRange < 100.0 && predictedProbS > tConf && predictedProbZ <= zRange && predictedProbZ >= -zRange)
					){
				if (predictedCardinal == numChild) {
					complete ++;
				} else if (predictedCardinal > numChild) {
					incomplete ++;
					missing += predictedCardinal - numChild;
				} else {
					less ++;
				}
			}
		}
		ctotal ++;

		entities.add(entityId);
		
		predictedCardinal = 0;
		predictedProb = 0.0;
		evidence = "";
		
		predictedOrdinal = 0;
		predictedOProb = 0.0;
		evidenceo = "";
		
		numPredicted = 0;
		
		br.close();
		if (bw != null) bw.close();
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec] for " + ctotal + " entities.");
		
		double precision = (double)tp / (double)(tp + fp);
//		double recall = (double)tp / instanceNum.size();
		double recall = (double)tp / (double)total;
		double fscore = (2 * precision * recall) / (precision + recall);
		
		String propRegex = ".*(P\\d+).*";
		Pattern propPattern = Pattern.compile(propRegex);
		Matcher propMatcher = propPattern.matcher(relName);
		
		String classRegex = ".*(Q\\d+).*";
		Pattern classPattern = Pattern.compile(classRegex);
		Matcher classMatcher = classPattern.matcher(relName);
		
		WikidataLabel wl = new WikidataLabel();
		if (label) {
			if (propMatcher.find()) {
				relName +=  "\t" + wl.getLabel(propMatcher.group(1));
			}
			if (classMatcher.find()) {
				relName += "\t" + wl.getLabel(classMatcher.group(1));
			}
		}
		
		if (resultPath != null) {
			bw = new BufferedWriter(new FileWriter(resultPath, true));
			bw.write(relName + "\t" + trainSize + "\t" + tp + "\t" + fp + "\t" + total  
					+ "\t" + String.format("%.4f", precision)
					+ "\t" + String.format("%.4f", recall)
					+ "\t" + String.format("%.4f", fscore)
					+ "\t" + complete + "\t" + incomplete + "\t" + less
					+ "\t" + available + "\t" + missing + "\t" + String.format("%.2f", ((float)missing / (float)available * 100)) + "%");
			bw.newLine();
			bw.close();
		} else {
			System.out.println("train\ttp\tfp\ttotal\tprec\trecall\tf1-score\tpred=num\tpred>num\tpred<num\tnum\tmissing\tincrease");
			System.out.println(trainSize + "\t" + tp + "\t" + fp + "\t" + total  
					+ "\t" + String.format("%.4f", precision)
					+ "\t" + String.format("%.4f", recall)
					+ "\t" + String.format("%.4f", fscore)
					+ "\t" + complete + "\t" + incomplete + "\t" + less
					+ "\t" + available + "\t" + missing + "\t" + String.format("%.2f", ((float)missing / (float)available * 100)) + "%");
		}
	}
	
	//TODO Change the key, should be the index
	public Map<Integer, String> extractNumber(List<String> nums, List<Double> probs, long maxNum) {
		Map<Integer, String> numTriple = new LinkedHashMap<Integer, String>();
		String number = "";
		Double prob = 0.0;
		for (int i=0; i<nums.size(); i++) {
			if (!nums.get(i).equals("")) {
				number = nums.get(i);
				prob = probs.get(i);
				
				if (number.startsWith("LatinGreek_")) {
					numTriple.put(i, number.split("_")[2] + "#" + prob);
					
				} else if (Numbers.getInteger(number) >= 0 
						&& Numbers.getInteger(number) <= maxNum
						) {
					numTriple.put(i, Numbers.getInteger(number) + "#" + prob);
				
				} else if ((number.equals("a") || number.equals("an"))
						&& Numbers.getInteger(nums.get(i+1)) < 0	//not followed by a cardinal or ordinal, e.g., a second daughter
						) {
					numTriple.put(i, "1" + "#" + prob);
				
				} else if (number.equals("both") 
						|| number.equals("some")
						|| number.equals("few")
						|| number.equals("many")
						|| number.equals("several")
						) {
					numTriple.put(i, "2" + "#" + prob);
				
				} else if (number.startsWith("PRP$_")) {
					
					if (number.split("_")[1].equals("S")) {
						numTriple.put(i, "1" + "#" + prob);
						
					} else if (number.split("_")[1].equals("P")) {
						numTriple.put(i, "2" + "#" + prob);
					}
				} else if (number.equals("no")
						|| number.equals("any")
						|| number.equals("without")
						|| number.equals("never")
						) {
					numTriple.put(i, "0" + "#" + prob);
				
				} else if (number.startsWith("un") 
						|| number.endsWith("less")) {
					numTriple.put(i, "0" + "#" + prob);
				}
			}
		}
		return numTriple;
	}
	
	public String wordsToSentence(List<String> words, int idx) {
		String sent = "", word = "";
		for (int i=0; i<words.size(); i++) {
			word = words.get(i);
			if (word.startsWith("LatinGreek_")) word = word.split("_")[1];
			if (i == idx) sent += "[" + word + "]" + " ";
			else sent += word + " ";
		}
		return sent.substring(0, sent.length()-1);
	}
	
	public String wordsToSentence(List<String> words, List<Integer> idx) {
		String sent = "", word = "";
		for (int i=0; i<words.size(); i++) {
			word = words.get(i);
			if (word.startsWith("LatinGreek_")) word = word.split("_")[1];
			if (idx.contains(i)) sent += "[" + word + "]" + " ";
			else sent += word + " ";
		}
		return sent.substring(0, sent.length()-1);
	}

}
