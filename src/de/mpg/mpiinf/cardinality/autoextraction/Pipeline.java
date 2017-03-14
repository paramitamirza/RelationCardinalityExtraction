package de.mpg.mpiinf.cardinality.autoextraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Pipeline {
	
	public static void main(String[] args) throws Exception {
		
		long startTime = System.currentTimeMillis();
		System.out.println("Start the Relation Cardinality Extraction pipeline... ");
		
		Options options = getPreprocessingOptions();

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
            
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			formatter.printHelp("RelationCardinalityExtraction: Pipeline", options);

			System.exit(1);
			return;
		}
		
		String inputCsvFile = cmd.getOptionValue("input");
		String testCsvFile = inputCsvFile;				//evaluation data = training data
		if (cmd.hasOption("e")) testCsvFile = cmd.getOptionValue("eval");
		String relName = cmd.getOptionValue("relname");
		
		//Preprocessing
		String wikipediaDir = cmd.getOptionValue("wikipedia");
		
		List<String> prepArgs = new ArrayList<String>();
		prepArgs.add("-i"); prepArgs.add(inputCsvFile);
		prepArgs.add("-p"); prepArgs.add(relName);
		prepArgs.add("-w"); prepArgs.add(wikipediaDir);
		prepArgs.add("-a");								//append Wikipedia curid to the input .csv file
		prepArgs.add("-f");								//generate feature file
		String dirFeature = "./feature_data/";
		if (cmd.hasOption("f")) {
			dirFeature = cmd.getOptionValue("feature");
		}
		prepArgs.add("-o"); prepArgs.add(dirFeature);
		prepArgs.add("-d");								//nummod setting
		prepArgs.add("-t"); prepArgs.add("1");			//consider num triples > 1
		PreprocessingConcurrent.main(prepArgs.toArray(new String[0]));
		
		//Classifier
		String dirModels = "./models/";
		if (cmd.hasOption("m")) {
			dirModels = cmd.getOptionValue("models");
		}
		String dirCRF = cmd.getOptionValue("crf");
		String templateFile = cmd.getOptionValue("template");
		
		List<String> clArgs = new ArrayList<String>();
		clArgs.add("-c"); clArgs.add(dirCRF);
		clArgs.add("-p"); clArgs.add(relName);
		clArgs.add("-l"); clArgs.add(templateFile);
		clArgs.add("-m"); clArgs.add(dirModels);
		
		String trainData = dirFeature + "/" + relName + "_train_cardinality.data";
		String evalData = trainData;					//evaluation data = training data
		if (cmd.hasOption("e")) evalData = trainData.replace("_train_", "_test_");	
		clArgs.add("-t"); clArgs.add(trainData);
		clArgs.add("-e"); clArgs.add(evalData);	
		Classifier.main(clArgs.toArray(new String[0]));
		
		//Evaluation
		List<String> evalArgs = new ArrayList<String>();
		evalArgs.add("-i"); evalArgs.add(testCsvFile);
		evalArgs.add("-f"); evalArgs.add(evalData.replace(".data", ".out"));
		evalArgs.add("-p"); evalArgs.add(relName);
		if (cmd.hasOption("o")) {
			String predictionFile = cmd.getOptionValue("output");
			evalArgs.add("-o"); evalArgs.add(predictionFile);
		}
		String resultFile = "./performance.txt";
		if (cmd.hasOption("r")) {
			resultFile = cmd.getOptionValue("result");
		}
		evalArgs.add("-r"); evalArgs.add(resultFile);
		Evaluation.main(evalArgs.toArray(new String[0]));
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
	}
	
	public static Options getPreprocessingOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input file (.csv) path");
		input.setRequired(true);
		options.addOption(input);
		
		Option eval = new Option("e", "eval", true, "Input evaluation file (.csv) path");
		eval.setRequired(false);
		options.addOption(eval);
		
		Option relName = new Option("p", "relname", true, "Property/relation name");
		relName.setRequired(true);
		options.addOption(relName);
		
		Option enLinks = new Option("w", "wikipedia", true, "Wikipedia resources directory");
		enLinks.setRequired(true);
		options.addOption(enLinks);
		
		Option feature = new Option("f", "feature", true, "Output directory of CRF++ feature files");
		feature.setRequired(false);
		options.addOption(feature);
		
		Option crf = new Option("c", "crf", true, "CRF++ directory");
		crf.setRequired(true);
		options.addOption(crf);
		
		Option template = new Option("l", "template", true, "CRF++ template file");
		template.setRequired(true);
		options.addOption(template);
		
		Option models = new Option("m", "models", true, "Output directory of CRF++ model files");
		models.setRequired(false);
		options.addOption(models);
        
		Option output = new Option("o", "output", true, "Prediction output file (.csv) path");
		output.setRequired(false);
		options.addOption(output);
		
		Option result = new Option("r", "result", true, "Performance result file path");
		result.setRequired(false);
		options.addOption(result);
		
		return options;
	}
}
