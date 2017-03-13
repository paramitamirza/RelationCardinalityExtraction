package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
		String resultPath = cmd.getOptionValue("crfout");
		String outputPath = null;
		boolean outFile = cmd.hasOption("o");
		if (outFile) {
			outputPath = cmd.getOptionValue("output");
		}
		
		Evaluation eval = new Evaluation();
		String[] labels = {"O", "_YES_"};
		boolean compositional = cmd.hasOption("c");
		eval.evaluate(csvPath, resultPath, labels, outputPath, compositional, false);
	}
	
	public static Options getEvalOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input evaluation file (.csv) path");
		input.setRequired(true);
		options.addOption(input);
        
		Option relName = new Option("f", "crfout", true, "CRF++ output file (.out) path");
		relName.setRequired(true);
		options.addOption(relName);
        
		Option output = new Option("o", "output", true, "Output file (.csv) path");
		output.setRequired(false);
		options.addOption(output);
		
		Option compositional = new Option("c", "compositional", false, "Label compositional numbers as true examples");
		compositional.setRequired(false);
		options.addOption(compositional);
        
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
	
	public void evaluate(String csvPath, String resultPath, String[] labels, String outPath,
			boolean addSameSentence, boolean addDiffSentence) throws IOException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Evaluate CRF++ output file... ");
		
		//Read .csv file
		BufferedReader br; String line;
		Map<String, Integer> instanceNum = new HashMap<String, Integer>();
		Map<String, String> instanceLabel = new HashMap<String, String>();
		br = new BufferedReader(new FileReader(csvPath));
		line = br.readLine();
		while (line != null) {
			instanceNum.put(line.split(",")[0], Integer.parseInt(line.split(",")[1]));
			instanceLabel.put(line.split(",")[0], line.split(",")[2]);
			line = br.readLine();
		}
		br.close();
		
		//Read result (.out) file
		br = new BufferedReader(new FileReader(resultPath));
		BufferedWriter bw = null;
		if (outPath != null) {
			bw = new BufferedWriter(new FileWriter(outPath));
		}
		
		Double prob = 0.0;
		List<String> nums = new ArrayList<String>();
		List<Double> probs = new ArrayList<Double>();
		
		int tp = 0;
		int fp = 0;
		int total = 0;
		double threshold = 0.1;
		
		String[] cols;
		List<String> sentence = new ArrayList<String>();
		String entityId = null;
		line = br.readLine();
		
		long predictedCardinal = 0;
		double predictedProb = 0.0;
		int numPredicted = 0;
		String evidence = "";
		
		Set<String> entities = new HashSet<String>();
		
		while (line != null) {
			
			if(!StringUtils.join(nums, "").equals("")) {
				Map<Integer, String> numbers = extractNumber(nums, probs);
				long n = 0;
				double p = 0.0, pp;
				int m = 0, mm;
				List<Integer> mlist = new ArrayList<Integer>();
				
				if (addSameSentence) {	
					//When there are more than one in a sentence:
					
					//if a number is a total of its following sequence of numbers, choose the total
					Object[] keys = numbers.keySet().toArray();
					int totalIdx = findTotalNumberOfComposition(numbers);
					
					if (totalIdx > 0) {
						pp = Double.parseDouble(numbers.get(totalIdx).split("#")[1]);
						if (pp > threshold) {
							p = pp;
							n = Integer.parseInt(numbers.get(totalIdx).split("#")[0]);
							mlist.add(totalIdx);
						}
					
					} else {
						//else, add them up
						for (Integer key : numbers.keySet()) {
							pp = Double.parseDouble(numbers.get(key).split("#")[1]);
							mm = key;
							if (pp > threshold) {
								p += pp;
								n += Long.parseLong(numbers.get(key).split("#")[0]);
								mlist.add(mm);
							}
						}
						p = p/numbers.size();
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
				
				if (addDiffSentence) {	
					//When there are more than one sentences, add them up
					predictedCardinal += n;
					predictedProb += p;
					evidence += wordsToSentence(sentence, mlist) + "|";
					numPredicted++;
					predictedProb = predictedProb/numPredicted;
					
				} else {
					//When there are more than one sentences, choose the most probable
					if (p > predictedProb) {
						predictedCardinal = n;
						predictedProb = p;
						
						evidence = wordsToSentence(sentence, mlist);
					}
				}
			}
			
			//Sentence starts			
			
			nums = new ArrayList<String>();
			probs = new ArrayList<Double>();
			sentence = new ArrayList<String>();
			
			line = br.readLine();
			line = br.readLine();
			
			while (line != null && !line.equals("")) {
				cols = line.split("\t");
				
				if (entityId != null && !cols[0].equals(entityId)
						&& !entities.contains(entityId)
						) {	//Entity ends
					int numChild = instanceNum.get(entityId);
					String wikiLabel = instanceLabel.get(entityId);
					
					if (bw != null) {
						bw.write(entityId + ",https://en.wikipedia.org/wiki?curid=" + wikiLabel + "," + numChild + "," + predictedCardinal + "," + predictedProb + ",\"" + evidence + "\"");
						bw.newLine();
//					} else {
//						System.err.println(entityId + ",https://en.wikipedia.org/wiki?curid=" + wikiLabel + "," + numChild + "," + predictedCardinal + "," + predictedProb + ",\"" + evidence + "\"");
					}
					if (numChild > 0) {
						if (numChild == predictedCardinal) tp ++;
						else if (numChild != predictedCardinal && predictedCardinal > 0) fp ++;
					}
					total ++;
					entities.add(entityId);
					
					predictedCardinal = 0;
					predictedProb = 0.0;
					numPredicted = 0;
					evidence = "";
				}
				
				entityId = cols[0];
				sentence.add(cols[3]);
				for (int l=0; l<labels.length; l++) {
					if (labels[l].equals("_YES_")) {
						prob = Double.valueOf(cols[cols.length-labels.length+l].split("/")[1]);
					}
				}
				if (prob > threshold) {
					nums.add(cols[3]);
					probs.add(prob);
				} else {
					nums.add("");
					probs.add(0.0);
				}
				
				line = br.readLine();
			}
		}
		
		br.close();
		if (bw != null) bw.close();
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
		
		double precision = (double)tp / (tp + fp);
		double recall = (double)tp / instanceNum.size();
		double fscore = (2 * precision * recall) / (precision + recall);
		System.out.println("tp\tfp\ttotal\tprec\trecall\tf1-score");
		System.out.println(tp + "\t" + fp + "\t" + total  
				+ "\t" + String.format("%.4f", precision)
				+ "\t" + String.format("%.4f", recall)
				+ "\t" + String.format("%.4f", fscore));
	}
	
	//TODO Change the key, should be the index
	public Map<Integer, String> extractNumber(List<String> nums, List<Double> probs) {
		Map<Integer, String> numTriple = new LinkedHashMap<Integer, String>();
		String number = "";
		Double prob = 0.0;
		for (int i=0; i<nums.size(); i++) {
			if (!nums.get(i).equals("")) {
				number = nums.get(i);
				prob = probs.get(i);
				
				if (number.startsWith("LatinGreek_")) {
					numTriple.put(i, number.split("_")[2] + "#" + prob);
					
				} else if (Numbers.getInteger(number) > 0) {
					numTriple.put(i, Numbers.getInteger(number) + "#" + prob);
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
