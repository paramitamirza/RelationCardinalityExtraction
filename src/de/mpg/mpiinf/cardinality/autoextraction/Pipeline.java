package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.File;
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
		
		// Run configurations' arguments for example
		// -i ./data/example/wikidata_sample_new.csv -p sample -w /local/home/paramita/D5data-8/RelationCardinalityExtraction_pipeline/enwiki_20170101_pages_articles/ -c /local/home/paramita/CRF++-0.58/ -l ./data/example/CRF/template_lemma.txt -d -t 1 -f ./data/example/ -m ./data/example/CRF/models/ -o ./data/example/predicted_children_count.csv -r ./data/example/performance.txt
		// -Xms2g -Xmx4g
		
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
		WikipediaArticle wiki = new WikipediaArticle(wikipediaDir, wikipediaDir + "/zindex/", wikipediaDir + "/wikibase_item.txt.gz");
		String dirFeature = "./feature_data/";
		if (cmd.hasOption("f")) {
			dirFeature = cmd.getOptionValue("feature");
		}
		FeatureExtractionConcurrent featExtraction = new FeatureExtractionConcurrent(inputCsvFile, relName, dirFeature);
		
//		wiki.appendCurId(inputCsvFile);				//No need anymore... should be handled by PreprocessingConcurrent with -b option
		
		boolean nummod = cmd.hasOption("d");
		boolean compositional = cmd.hasOption("s");
		int threshold = 0;
		if (cmd.hasOption("t")) threshold = Integer.parseInt(cmd.getOptionValue("threshold"));
		featExtraction.run(wiki, nummod, compositional, threshold);
		
		//Classifier
		String dirModels = "./models/";
		if (cmd.hasOption("m")) {
			dirModels = cmd.getOptionValue("models");
		}
		String dirCRF = cmd.getOptionValue("crf");
		String templateFile = cmd.getOptionValue("template");
		String trainData = dirFeature + "/" + relName + "_train_cardinality.data";
		String evalData = trainData;					//evaluation data = training data
		
		Classifier cl = new Classifier(relName, dirCRF, dirModels, templateFile);
		cl.trainModel(trainData);						//train model
		cl.testModel(evalData);							//test model
		
		// Once training is complete, delete data file...
		File dataFile = new File(trainData);
		dataFile.delete();
		
		//Evaluation
		String predictionFile = null;
		if (cmd.hasOption("o")) {
			predictionFile = cmd.getOptionValue("output");
		}
		String resultFile = "./performance.txt";
		if (cmd.hasOption("r")) {
			resultFile = cmd.getOptionValue("result");
		}
		
		Evaluation eval = new Evaluation();
		String[] labels = {"O", "_YES_"};
		String crfOutPath = evalData.replace(".data", ".out");
		eval.evaluate(relName, testCsvFile, crfOutPath, labels, predictionFile, resultFile, compositional, false);
		
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
		
		Option nummod = new Option("d", "nummod", false, "Only if dependency label is 'nummod' to be labelled as positive examples");
		nummod.setRequired(false);
		options.addOption(nummod);
		
		Option compositional = new Option("s", "compositional", false, "Label compositional numbers as true examples");
		compositional.setRequired(false);
		options.addOption(compositional);
		
		Option threshold = new Option("t", "threshold", true, "Threshold for number of triples to be labelled as positive examples");
		threshold.setRequired(false);
		options.addOption(threshold);
		
		return options;
	}
}
