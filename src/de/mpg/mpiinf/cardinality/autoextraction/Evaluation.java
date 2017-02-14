package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		eval.evaluate(csvPath, resultPath, labels, outputPath, false, false);
	}
	
	public static Options getEvalOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input evaluation file (.csv) path");
		input.setRequired(true);
        options.addOption(input);
        
        Option relName = new Option("c", "crfout", true, "CRF++ output file (.out) path");
        relName.setRequired(true);
        options.addOption(relName);
        
        Option output = new Option("o", "output", true, "Output file (.csv) path");
        output.setRequired(false);
        options.addOption(output);
        
        return options;
	}
	
	public void evaluate(String csvPath, String resultPath, String[] labels, String outPath,
			boolean addSameSentence, boolean addDiffSentence) throws IOException {
		
		//Read .csv file
		BufferedReader br; String line;
		Map<String, Integer> instanceNum = new HashMap<String, Integer>();
		Map<String, String> instanceLabel = new HashMap<String, String>();
		br = new BufferedReader(new FileReader(csvPath));
		line = br.readLine();
		while (line != null) {
			instanceNum.put(line.split(",")[0], Integer.parseInt(line.split(",")[2]));
			instanceLabel.put(line.split(",")[0], line.split(",")[1]);
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
		
		double threshold = 0.1;
		
		String[] cols;
		List<String> sentence = new ArrayList<String>();
		String entityId = null;
		line = br.readLine();
		
		long predictedCardinal = 0;
		double predictedProb = 0.0;
		int numPredicted = 0;
		String evidence = "";
		
		while (line != null) {
			
			if(!StringUtils.join(nums, "").equals("")) {
				Map<Long, String> numbers = extractNumber(nums, probs);
				long n = 0;
				double p = 0.0, pp;
				int m = 0, mm;
				List<Integer> mlist = new ArrayList<Integer>();
				
				if (addSameSentence) {	
					//When there are more than one in a sentence, add them up
					for (Long key : numbers.keySet()) {
						pp = Double.parseDouble(numbers.get(key).split("|")[1]);
						mm = Integer.parseInt(numbers.get(key).split("|")[0]);
						if (pp > p) {
							n += key;
							p += pp;
							mlist.add(mm);
						}
					}
					p = p/numbers.size();
					
				} else {	
					//When there are more than one in a sentence, choose the most probable
					for (Long key : numbers.keySet()) {
						pp = Double.parseDouble(numbers.get(key).split("#")[1]);
						mm = Integer.parseInt(numbers.get(key).split("#")[0]);
						if (pp > p) {
							n = key;
							p = pp;
							m = mm;
						}
					}
				}
				
				if (addDiffSentence) {	
					//When there are more than one sentences, add them up
					if (p > threshold) {
						predictedCardinal += n;
						predictedProb += p;
						evidence += wordsToSentence(sentence, mlist) + "|";
						numPredicted++;
					}
					predictedProb = predictedProb/numPredicted;
					
				} else {
					//When there are more than one sentences, choose the most probable
					if (p > predictedProb
							&& p > threshold) {
						predictedCardinal = n;
						predictedProb = p;
						evidence = wordsToSentence(sentence, m);
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
				
				if (entityId != null && !cols[0].equals(entityId)) {	//Entity ends
					int numChild = instanceNum.get(entityId);
					String wikiLabel = instanceLabel.get(entityId);
					
					if (bw != null) {
						bw.write(entityId + ",https://en.wikipedia.org/wiki/" + wikiLabel + "," + numChild + "," + predictedCardinal + "," + predictedProb + ",\"" + evidence + "\"");
						bw.newLine();
					} else {
						System.out.println(entityId + ",https://en.wikipedia.org/wiki/" + wikiLabel + "," + numChild + "," + predictedCardinal + "," + predictedProb + ",\"" + evidence + "\"");
					}
					if (numChild > 0) {
						if (numChild == predictedCardinal) tp ++;
						else if (numChild != predictedCardinal && predictedCardinal > 0) fp ++;
					}
					
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
		
		double precision = (double)tp / (tp + fp);
		double recall = (double)tp / instanceNum.size();
		double fscore = (2 * precision * recall) / (precision + recall);
		System.out.println("Precision: " + precision);
		System.out.println("Recall: " + recall);
		System.out.println("F1-score: " + fscore);
	}
	
	public Map<Long, String> extractNumber(List<String> nums, List<Double> probs) {
		Map<Long, String> numChild = new HashMap<Long, String>();
		String number = "";
		Double prob = 0.0;
		for (int i=0; i<nums.size(); i++) {
			if (!nums.get(i).equals("")) {
				number = nums.get(i);
				prob = probs.get(i);
				if (i+1<nums.size() && !nums.get(i+1).equals("")) {
					number += " " + nums.get(i+1);
					prob = (prob + probs.get(i))/2;
					i ++;
				}
				if (Numbers.getInteger(number) > 0) {
					numChild.put(Numbers.getInteger(number), i + "#" + prob);
				}
			}
		}
		return numChild;
	}
	
	public String wordsToSentence(List<String> words, int idx) {
		String sent = "";
		for (int i=0; i<words.size(); i++) {
			if (i == idx) sent += "[" + words.get(i) + "]" + " ";
			else sent += words.get(i) + " ";
		}
		return sent.substring(0, sent.length()-1);
	}
	
	public String wordsToSentence(List<String> words, List<Integer> idx) {
		String sent = "";
		for (int i=0; i<words.size(); i++) {
			if (idx.contains(i)) sent += "[" + words.get(i) + "]" + " ";
			else sent += words.get(i) + " ";
		}
		return sent.substring(0, sent.length()-1);
	}

}
