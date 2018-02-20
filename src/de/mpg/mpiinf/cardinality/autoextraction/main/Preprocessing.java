package de.mpg.mpiinf.cardinality.autoextraction.main;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.mpg.mpiinf.cardinality.autoextraction.DistributionExtractionConcurrent;
import de.mpg.mpiinf.cardinality.autoextraction.FeatureExtractionConcurrent;
import de.mpg.mpiinf.cardinality.autoextraction.WikipediaArticle;

public class Preprocessing {
	
	public static void main(String[] args) throws Exception {
		
		Options options = getPreprocessingOptions();

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
            
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			formatter.printHelp("RelationCardinalityExtraction: Preprocessing", options);

			System.exit(1);
			return;
		}
		
//		String inputCsvFile = "./data/auto_extraction/wikidata_sample.csv";
		String inputCsvFile = cmd.getOptionValue("input");
		
		//Set Wikipedia resource (Wikipedia dump in plain text, index and mapping files) directory
		WikipediaArticle wiki;
		if (cmd.hasOption("w")) {
			String wikipediaDir = cmd.getOptionValue("wikipedia");
			wiki = new WikipediaArticle(wikipediaDir, wikipediaDir + "zindex/", wikipediaDir + "wikibase_item.txt.gz");
			
		} else {
			System.err.println("Wikipedia resource directory path is missing!");
			System.err.println("-- Specify -w [Wikipedia resource directory path]");

            System.exit(1);
            return;
		}
		
		if (cmd.hasOption("n")) WikipediaArticle.setNumberOfThreads(Integer.parseInt(cmd.getOptionValue("thread")));
		
		String delimiter = ",";
		if (cmd.hasOption("tab")) delimiter = "\t";
		
		if (cmd.hasOption("a")) {
			wiki.mapWikidataWikipediaCurId();
			wiki.appendCurId(inputCsvFile, delimiter);
			wiki.destroyMapping();
			
		} else if (cmd.hasOption("b")) {
			wiki.mapWikidataWikipediaCurId();
			File folder = new File(cmd.getOptionValue("batch"));
			File[] listOfFiles = folder.listFiles();
			
			for (File f : listOfFiles) {
				if (f.isFile()) {
					wiki.appendCurId(f.getPath(), delimiter);
				}
			}
			wiki.destroyMapping();
		}
		
		if (cmd.hasOption("u")) {
			String relName = cmd.getOptionValue("relname");
			String dirFeature = "./feature_data/";
			if (cmd.hasOption("o")) {
				dirFeature = cmd.getOptionValue("output");
			} 
			Double ignoreFreq = 0.1;
//			if (cmd.hasOption("q")) ignoreFreq = Integer.parseInt(cmd.getOptionValue("ignorefreq"));
			
			DistributionExtractionConcurrent distExtraction = new DistributionExtractionConcurrent(inputCsvFile, delimiter, relName, dirFeature, ignoreFreq);
			distExtraction.run(wiki);
		}
		
		//Generate feature file (in column format) for CRF++
		if (cmd.hasOption("f")) {
			String evalCsvFile = null;
			if (cmd.hasOption("e")) {
				evalCsvFile = cmd.getOptionValue("eval");
			}
			String relName = cmd.getOptionValue("relname");
			
			String dirFeature = "./feature_data/";
			if (cmd.hasOption("o")) {
				dirFeature = cmd.getOptionValue("output");
			} 
			
			FeatureExtractionConcurrent featExtraction;
			if (evalCsvFile != null)
				featExtraction = new FeatureExtractionConcurrent(inputCsvFile, evalCsvFile, delimiter, relName, dirFeature);
			else {
				if (cmd.hasOption("r")) {
					int nRandom = Integer.parseInt(cmd.getOptionValue("randomize"));
					featExtraction = new FeatureExtractionConcurrent(inputCsvFile, delimiter, nRandom, relName, dirFeature);
				} else {
					featExtraction = new FeatureExtractionConcurrent(inputCsvFile, delimiter, relName, dirFeature);
				}
			}
			
			if (cmd.hasOption("n")) FeatureExtractionConcurrent.setNumberOfThreads(Integer.parseInt(cmd.getOptionValue("thread")));
			
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
		
		Option appendCurId = new Option("a", "curid", false, "Append input file (.csv) with Wikipedia curId for each Wikidata instance");
		appendCurId.setRequired(false);
		options.addOption(appendCurId);
		
		Option batch = new Option("b", "batch", true, "Append input files (.csv) in a directory with Wikipedia curId for each Wikidata instance");
		batch.setRequired(false);
		options.addOption(batch);
		
		Option random = new Option("r", "randomize", true, "Generate n random instances for testing");
		random.setRequired(false);
		options.addOption(random);
		
		Option extractFeature = new Option("f", "features", false, "Generate feature file (in column format) for CRF++");
		extractFeature.setRequired(false);
		options.addOption(extractFeature);
		
		Option extractDist = new Option("u", "distributions", false, "Generate number distributions");
		extractDist.setRequired(false);
		options.addOption(extractDist);
		
		Option output = new Option("o", "output", true, "Output directory of feature files (in column format) for CRF++");
		output.setRequired(false);
		options.addOption(output);
		
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
		
		return options;
	}

}
