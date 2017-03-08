package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FeatureExtractionConcurrent {
	
	private String inputCsvFile = "./data/example/wikidata_sample.csv";
	private String inputRandomCsvFile = "./data/example/wikidata_sample_random10.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	
	private static final int NTHREDS = 200;
	
	public FeatureExtractionConcurrent() {
		
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, String inputRandomCsvFilePath, String relationName, String dirOutput) {
		this();
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile(inputRandomCsvFilePath);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		FeatureExtractionConcurrent featExtraction;
		if (args.length < 4) {
			featExtraction = new FeatureExtractionConcurrent();
		} else {
			featExtraction = new FeatureExtractionConcurrent(args[0], args[1], args[2], args[3]);
		}
		
		featExtraction.run(true, false, 0);
	}
	
	public void run(boolean nummod, boolean compositional, int threshold) throws IOException, InterruptedException {
		
		removeOldFeatureFiles();
		
		List<String> testInstances = readRandomInstances(getInputRandomCsvFile());
		String line;
		String wikidataId = "", label = "", count = "";
		boolean training;
		
		BufferedReader br = new BufferedReader(new FileReader(getInputCsvFile()));
		line = br.readLine();
		
		List<Thread> threads = new ArrayList<Thread>();
		
		System.out.println("Generate feature file (in column format) for CRF++...");
		
		//First wikidataId starts...
		wikidataId = line.split(",")[0];
        label = line.split(",")[1];
        count = line.split(",")[2];
        System.out.println(wikidataId + "\t" + label + "\t" + count);
        
		training = true;
        if (testInstances.contains(wikidataId)) {
			training = false;
		} 
		
		GenerateFeatures ext = new GenerateFeatures(getDirFeature(), getRelName(),
        		wikidataId, label, count, training,
        		nummod, compositional, threshold);
		ext.run();
		//Done. Next WikidataIds...
		
		line = br.readLine();
		
		ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
		
		while (line != null) {
			wikidataId = line.split(",")[0];
	        label = line.split(",")[1];
	        count = line.split(",")[2];
	        System.out.println(wikidataId + "\t" + label + "\t" + count);
	        
	        training = true;
	        if (testInstances.contains(wikidataId)) {
				training = false;
			} 
	        
	        Runnable worker = new GenerateFeatures(getDirFeature(), getRelName(),
	        		wikidataId, label, count, training,
	        		true, false, 1);
            executor.execute(worker);
             
            line = br.readLine();
		}
		
		// This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        executor.awaitTermination(1, TimeUnit.NANOSECONDS);
        System.out.println("Finished all threads");
		
		br.close();
	}
	
	public void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public void removeOldFeatureFiles() throws IOException {
		ensureDirectory(new File(dirFeature));
		File train = new File(dirFeature + relName + "_train_cardinality.data");
		File test = new File(dirFeature + relName + "_test_cardinality.data");
		Files.deleteIfExists(train.toPath());
		Files.deleteIfExists(test.toPath());
	}
	
	public List<String> readRandomInstances(String inputFile) throws IOException {
		System.out.println("Read random instances...");
		List<String> randomInstances = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String line = br.readLine();		
		while (line != null) {
			randomInstances.add(line.split(",")[0]);
			line = br.readLine();
		}
		br.close();
		return randomInstances;
	}

	public String getInputCsvFile() {
		return inputCsvFile;
	}

	public void setInputCsvFile(String inputCsvFile) {
		this.inputCsvFile = inputCsvFile;
	}

	public String getInputRandomCsvFile() {
		return inputRandomCsvFile;
	}

	public void setInputRandomCsvFile(String inputRandomCsvFile) {
		this.inputRandomCsvFile = inputRandomCsvFile;
	}
	
	public String getRelName() {
		return relName;
	}

	public void setRelName(String relationName) {
		this.relName = relationName;
	}
	
	public String getDirFeature() {
		return dirFeature;
	}

	public void setDirFeature(String dirFeature) {
		this.dirFeature = dirFeature;
	}
}
