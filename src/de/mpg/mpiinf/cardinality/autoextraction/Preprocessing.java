package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
		
		if (cmd.hasOption("a")) {
			wiki.mapWikidataWikipediaCurId();
			wiki.appendCurId(inputCsvFile);
			wiki.destroyMapping();
			
		} else if (cmd.hasOption("b")) {
			wiki.mapWikidataWikipediaCurId();
			File folder = new File(cmd.getOptionValue("batch"));
			File[] listOfFiles = folder.listFiles();
			
			for (File f : listOfFiles) {
				if (f.isFile()) {
					wiki.appendCurId(f.getPath());
				}
			}
			wiki.destroyMapping();
		}
		
		//Generate feature file (in column format) for CRF++
		if (cmd.hasOption("f")) {
			String inputRandomCsvFile = null;
			if (cmd.hasOption("r")) {
				inputRandomCsvFile = cmd.getOptionValue("random");
			}
			String relName = cmd.getOptionValue("relname");
			
			String dirFeature = "./feature_data/";
			if (cmd.hasOption("o")) {
				dirFeature = cmd.getOptionValue("output");
			} 
			
			FeatureExtractionConcurrent featExtraction;
			if (inputRandomCsvFile != null)
				featExtraction = new FeatureExtractionConcurrent(inputCsvFile, inputRandomCsvFile, relName, dirFeature);
			else {
				if (cmd.hasOption("n")) {
					int nRandom = Integer.parseInt(cmd.getOptionValue("randomize"));
					featExtraction = new FeatureExtractionConcurrent(inputCsvFile, nRandom, relName, dirFeature);
				} else {
					featExtraction = new FeatureExtractionConcurrent(inputCsvFile, relName, dirFeature);
				}
			}
			
			if (cmd.hasOption("n")) FeatureExtractionConcurrent.setNumberOfThreads(Integer.parseInt(cmd.getOptionValue("thread")));
			
			boolean nummod = cmd.hasOption("d");
			boolean compositional = cmd.hasOption("c");
			boolean transform = cmd.hasOption("x");
			boolean transformZeroOne = cmd.hasOption("z");
			boolean ignoreHigher = cmd.hasOption("h");
			int threshold = 0;
			if (cmd.hasOption("t")) threshold = Integer.parseInt(cmd.getOptionValue("threshold"));
			featExtraction.run(wiki, nummod, compositional, threshold, transform, transformZeroOne, ignoreHigher);
		}
		
	}
	
	public static Options getPreprocessingOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input file (.csv) path");
		input.setRequired(true);
		options.addOption(input);
		
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
		
		Option random = new Option("n", "randomize", true, "Generate n random instances for testing");
		random.setRequired(false);
		options.addOption(random);
		
		Option randomFile = new Option("r", "random", true, "Input random file (.csv) path for testing");
		randomFile.setRequired(false);
		options.addOption(randomFile);
		
		Option extractFeature = new Option("f", "features", false, "Generate feature file (in column format) for CRF++");
		extractFeature.setRequired(false);
		options.addOption(extractFeature);
		
		Option output = new Option("o", "output", true, "Output directory of feature files (in column format) for CRF++");
		output.setRequired(false);
		options.addOption(output);
		
		Option nummod = new Option("d", "nummod", false, "Only if dependency label is 'nummod' to be labelled as positive examples");
		nummod.setRequired(false);
		options.addOption(nummod);
		
		Option compositional = new Option("c", "compositional", false, "Label compositional numbers as true examples");
		compositional.setRequired(false);
		options.addOption(compositional);
		
		Option threshold = new Option("t", "threshold", true, "Threshold for number of triples to be labelled as positive examples");
		threshold.setRequired(false);
		options.addOption(threshold);
		
		Option transform = new Option("x", "transform", false, "Transform non-numeric concepts into numbers");
		transform.setRequired(false);
		options.addOption(transform);
		
		Option transformZeroOne = new Option("z", "transformzeroone", false, "Transform negative sentences into (containing) 0 and articles into 1");
		transformZeroOne.setRequired(false);
		options.addOption(transformZeroOne);
		
		Option ignoreHigher = new Option("h", "ignorehigher", false, "Ignore numbers > num_of_triples as negative examples");
		ignoreHigher.setRequired(false);
		options.addOption(ignoreHigher);
		
		Option nThreads = new Option("n", "thread", true, "Number of threads");
		nThreads.setRequired(false);
		options.addOption(nThreads);
		
		return options;
	}

}
