package de.mpg.mpiinf.cardinality.autoextraction.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.mpg.mpiinf.cardinality.autoextraction.FeatureExtractionConcurrent;
import de.mpg.mpiinf.cardinality.autoextraction.ReadFromFile;
import de.mpg.mpiinf.cardinality.autoextraction.WikipediaArticle;

public class Pipeline {
	
	public static void main(String[] args) throws Exception {
		
		// Run configurations' arguments for example
		// -i ./data/example/wikidata_sample_new.csv -p sample -w /local/home/paramita/D5data-8/RelationCardinalityExtraction_pipeline/enwiki_20170101_pages_articles/ -c /local/home/paramita/CRF++-0.58/ -l ./data/example/CRF/template_lemma.txt -d -t 1 -f ./data/example/ -m ./data/example/CRF/models/ -o ./data/example/predicted_children_count.csv -r ./data/example/performance.txt
		// -Xms2g -Xmx4g
		
		// ACL
		// -i /local/home/paramita/D5data-8/RCE_pipeline/example_dir/wikidata_children 
		// -e /local/home/paramita/D5data-8/RCE_pipeline/example_dir/wikidata_children_random200
		// -p wikidata_children 
		// -d
		// -w /local/home/paramita/D5data-8/RCE_pipeline/enwiki_20170320_pages_articles/
		// -c /local/home/paramita/CRF++-0.58/
		// -l /local/home/paramita/D5data-8/RCE_pipeline/data/template_lemma.txt
		// -m ./data/example/CRF/models/ 
		// -o ./data/example/predicted_wikidata_children.csv 
		// -r ./data/example/acl_performance.txt
		
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
		long trainSize = ReadFromFile.countLines(inputCsvFile);
		
		String delimiter = ",";
		if (cmd.hasOption("tab")) delimiter = "\t";
		
		//Preprocessing
		String wikipediaDir = cmd.getOptionValue("wikipedia");
		WikipediaArticle wiki = new WikipediaArticle(wikipediaDir, wikipediaDir + "/zindex/", wikipediaDir + "/wikibase_item.txt.gz");
		if (cmd.hasOption("n")) WikipediaArticle.setNumberOfThreads(Integer.parseInt(cmd.getOptionValue("thread")));
		
		String dirFeature = "./feature_data/";
		if (cmd.hasOption("f")) {
			dirFeature = cmd.getOptionValue("feature");
		}
		FeatureExtractionConcurrent featExtraction;
		if (cmd.hasOption("e")) {
			featExtraction = new FeatureExtractionConcurrent(inputCsvFile, testCsvFile, delimiter, relName, dirFeature);
		} else {
			featExtraction = new FeatureExtractionConcurrent(inputCsvFile, delimiter, relName, dirFeature);
		} 
		if (cmd.hasOption("n")) FeatureExtractionConcurrent.setNumberOfThreads(Integer.parseInt(cmd.getOptionValue("thread")));
		
//		wiki.appendCurId(inputCsvFile);				//No need anymore... should be handled by PreprocessingConcurrent with -b option
		
		boolean nummod = cmd.hasOption("d");
		boolean compositional = cmd.hasOption("s");
		boolean numterms = cmd.hasOption("numterms");
		boolean ordinals = cmd.hasOption("ordinals");
		boolean articles = cmd.hasOption("articles");
		boolean quantifiers = cmd.hasOption("quantifiers");
		boolean pronouns = cmd.hasOption("pronouns");
		boolean negation = cmd.hasOption("negation");	
		
		int ignoreFreq = -1;
		if (cmd.hasOption("g")) ignoreFreq = Integer.parseInt(cmd.getOptionValue("ignorefreq"));
		
		float threshold = (float)0;
		if (cmd.hasOption("t")) threshold = Float.parseFloat(cmd.getOptionValue("threshold"));
		
		float topPopular = (float)1;
		if (cmd.hasOption("k")) topPopular = Float.parseFloat(cmd.getOptionValue("popular"));
		
		int quarterPart = 0;
		if (cmd.hasOption("q")) quarterPart = Integer.parseInt(cmd.getOptionValue("quarter"));
		
		boolean ignoreHigher = cmd.hasOption("h");
		int ignoreHigherLess = -1;
		if (cmd.hasOption("h")) ignoreHigherLess = Integer.parseInt(cmd.getOptionValue("ignorehigher"));
		
		boolean negTrain = false;
		
		featExtraction.run(wiki, ignoreHigher, ignoreHigherLess, 
				threshold, ignoreFreq, topPopular, quarterPart,
				nummod, ordinals, numterms,
				articles, quantifiers, pronouns,
				compositional, 
				negation, negTrain 
				);
		
		//Classifier
		String dirModels = "./models/";
		if (cmd.hasOption("m")) {
			dirModels = cmd.getOptionValue("models");
		}
		String dirCRF = cmd.getOptionValue("crf");
		String templateFile = cmd.getOptionValue("template");
		String trainData = dirFeature + "/" + relName + "_train_cardinality.data";
		String evalData;
		if (cmd.hasOption("e")) {
			evalData = dirFeature + "/" + relName + "_test_cardinality.data";
		} else {
			evalData = trainData;						//evaluation data = training data
		}
		
		Classifier cl = new Classifier(relName, dirCRF, dirModels, templateFile);
		if (cmd.hasOption("n")) Classifier.setNumberOfThreads(Integer.parseInt(cmd.getOptionValue("thread")));
		cl.trainModel(trainData);						//train model
		cl.testModel(evalData);							//test model
		
		//Evaluation
		String predictionFile = null;
		if (cmd.hasOption("o")) {
			predictionFile = cmd.getOptionValue("output");
		}
		String resultFile = "./performance.txt";
		if (cmd.hasOption("r")) {
			resultFile = cmd.getOptionValue("result");
		}
		
		float minProb = (float)0.1;
		if (cmd.hasOption("prob")) minProb = Float.parseFloat(cmd.getOptionValue("prob"));
		
		float minConfScore = (float)0.0;
		if (cmd.hasOption("v")) minConfScore = Float.parseFloat(cmd.getOptionValue("confidence"));
		
		float zScore = (float)100.0;
		if (cmd.hasOption("z")) zScore = Float.parseFloat(cmd.getOptionValue("zscore"));
		
		boolean label = false;
		if (cmd.hasOption("label")) label = true;
		
		boolean crf = false;
		if (cmd.hasOption("crf")) crf = true;
		
		Evaluation eval = new Evaluation();
		String[] labels = {"O", "_YES_"};
		
		File file = new File(evalData);
		String crfOutPath = file.getAbsoluteFile().getParent() + "/" + relName + "_cardinality.out";
//		String crfOutPath = evalData.replace(".data", ".out");
		
		eval.evaluate(relName, testCsvFile, testCsvFile, delimiter, 
				crfOutPath, labels, 
				predictionFile, resultFile, 
				compositional, false, ordinals, negation,
				minProb, minConfScore, zScore, trainSize, false, 
				label, crf);
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
		
		// Once everything is done, delete data file...
		if (cmd.hasOption("delete")) {
			File dataFile = new File(trainData);
			dataFile.deleteOnExit();
			dataFile = new File(evalData);
			dataFile.deleteOnExit();
			File crfOutFile = new File(crfOutPath);
			crfOutFile.deleteOnExit();
			File modelFile = new File(dirModels + "/" + relName + ".model");
			modelFile.deleteOnExit();
		}
	}
	
	public static Options getPreprocessingOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input file (.csv) path");
		input.setRequired(true);
		options.addOption(input);
		
		Option eval = new Option("e", "eval", true, "Input evaluation file (.csv) path");
		eval.setRequired(false);
		options.addOption(eval);
		
		Option tab = new Option("tab", "tab", false, "Tab separated input files");
		tab.setRequired(false);
		options.addOption(tab);
		
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
		
		Option delete = new Option("del", "delete", false, "Delete feature and model files");
		delete.setRequired(false);
		options.addOption(delete);
		
		Option nummod = new Option("d", "nummod", false, "Only if dependency label is 'nummod' to be labelled as positive examples");
		nummod.setRequired(false);
		options.addOption(nummod);
		
		Option compositional = new Option("s", "compositional", false, "Label compositional numbers as true examples");
		compositional.setRequired(false);
		options.addOption(compositional);
		
		Option threshold = new Option("t", "threshold", true, "Informativeness threshold for number of triples to be labelled as positive examples");
		threshold.setRequired(false);
		options.addOption(threshold);
		
		Option transform = new Option("numterms", "numterms", false, "Transform non-numeric concepts into numbers");
		transform.setRequired(false);
		options.addOption(transform);
		
		Option ordinal = new Option("ordinals", "ordinals", false, "Consider ordinals as candidates");
		ordinal.setRequired(false);
		options.addOption(ordinal);
		
		Option transformArticles = new Option("articles", "articles", false, "Transform articles into 1");
		transformArticles.setRequired(false);
		options.addOption(transformArticles);
		
		Option transformQuantifiers = new Option("quantifiers", "quantifiers", false, "Transform imprecise quantifiers (many, several, etc.) into 2");
		transformQuantifiers.setRequired(false);
		options.addOption(transformQuantifiers);
		
		Option transformPronouns = new Option("pronouns", "pronouns", false, "Transform personal pronouns (many, several, etc.) into 1 or 2");
		transformPronouns.setRequired(false);
		options.addOption(transformPronouns);
		
		Option transformZero = new Option("0", "negation", false, "Transform negative sentences into (containing) 0");
		transformZero.setRequired(false);
		options.addOption(transformZero);
		
		Option ignoreHigher = new Option("h", "ignorehigher", true, "Ignore numbers > num_of_triples, but < (num_of_triples + h), as negative examples");
		ignoreHigher.setRequired(false);
		options.addOption(ignoreHigher);
		
		Option ignoreFreq = new Option("g", "ignorefreq", true, "Ignore frequent numbers in the text (do not label as positive examples)");
		ignoreFreq.setRequired(false);
		options.addOption(ignoreFreq);
		
		Option topPopular = new Option("k", "popular", true, "Cutoff percentage of popular instances as training examples");
		topPopular.setRequired(false);
		options.addOption(topPopular);
		
		Option quarterPart = new Option("q", "quarter", true, "Quarter part of popular instances as training examples");
		quarterPart.setRequired(false);
		options.addOption(quarterPart);
		
		Option nThreads = new Option("n", "thread", true, "Number of threads");
		nThreads.setRequired(false);
		options.addOption(nThreads);
		
		Option minProb = new Option("prob", "prob", true, "Minimum marginal probability");
		minProb.setRequired(false);
		options.addOption(minProb);
		
		Option minConfScore = new Option("v", "confidence", true, "Minimum confidence score");
		minConfScore.setRequired(false);
		options.addOption(minConfScore);
		
		Option zScoreRange = new Option("z", "zscore", true, "Maximum range of z-score");
		zScoreRange.setRequired(false);
		options.addOption(zScoreRange);
		
		Option label = new Option("label", "label", false, "Print property/relation and class labels");
		label.setRequired(false);
		options.addOption(label);
		
		Option isCRF = new Option("crf", "crf", false, "CRF evaluation");
		isCRF.setRequired(false);
		options.addOption(isCRF);
		
		return options;
	}
}
