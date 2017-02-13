package de.mpg.mpiinf.cardinality.autoextraction;

public class Preprocessing {
	
	public static void main(String[] args) throws Exception {
		
		String inputCsvFile = "./data/auto_extraction/wikidata_sample.csv";
		
		//If input CSV file doesn't have Wikipedia labels for each Wikidata ID
		String wikipediaLinkFile = "./data/auto_extraction/english_links.txt.gz";
		AddWikipediaTitle addWikiTitle = new AddWikipediaTitle(inputCsvFile, inputCsvFile.replace(".csv", "_links.csv"), wikipediaLinkFile);
		addWikiTitle.append();
		inputCsvFile = inputCsvFile.replace(".csv", "_links.csv");
		
		//Extract Wikipedia sentences (containing numbers) per Wikidata instance
		String outputJsonFile = "./data/auto_extraction/wikidata_sample.jsonl.gz";
		SentenceExtractionFromWikipedia sentExtraction = new SentenceExtractionFromWikipedia(inputCsvFile, outputJsonFile);
		sentExtraction.extractSentences();
		
		//If random CSV file (for evaluation) is not available yet
		
		
		String inputJsonFile = outputJsonFile;
		String inputRandomCsvFile = "./data/auto_extraction/wikidata_sample_random.csv";
		String relName = "sample";
		String dirFeature = "./data/auto_extraction/";
		FeatureExtractionForCRF featExtraction = new FeatureExtractionForCRF(inputJsonFile, inputRandomCsvFile, relName, dirFeature);
		featExtraction.generateColumnsFile();
		
	}

}
