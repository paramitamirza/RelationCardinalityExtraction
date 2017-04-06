package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DistributionExtractionConcurrent {
	
	private String inputCsvFile = "./data/example/wikidata_sample.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	private int freq;
	
	private static int NTHREADS = -999;
	
	public static void setNumberOfThreads(int n) {
		NTHREADS = n;
	}
	
	public DistributionExtractionConcurrent() {
		
	}
	
	public DistributionExtractionConcurrent(String inputCsvFilePath, String relationName, String dirOutput, int freq) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
		this.setFreq(freq);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		DistributionExtractionConcurrent featExtraction;
		if (args.length < 4) {
			featExtraction = new DistributionExtractionConcurrent();
		} else {
			featExtraction = new DistributionExtractionConcurrent(args[0], args[1], args[2], Integer.parseInt(args[3]));
		}
		
		WikipediaArticle wiki = new WikipediaArticle();
		featExtraction.run(wiki);
	}
	
	public void run(WikipediaArticle wiki) throws IOException, InterruptedException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Generate number distributions... ");
		
		removeOldFeatureFiles();
		
		String line;
		String wikidataId = "", count = "";
		Integer curId;
		
		BufferedReader br = new BufferedReader(new FileReader(getInputCsvFile()));
		
		line = br.readLine();
		
		//First wikidataId starts...
		wikidataId = line.split(",")[0];
        count = line.split(",")[1];
        curId = Integer.parseInt(line.split(",")[2]);
        
        GenerateDistributions ext = new GenerateDistributions(getDirFeature(), getRelName(),
				wiki, wikidataId, count, curId, getFreq());
		ext.run();
		//Done. Next WikidataIds...
		
		line = br.readLine();
		
		ExecutorService executor;
		if (NTHREADS < 0) {
			executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		} else {
			executor = Executors.newFixedThreadPool(NTHREADS);
		}
		
		while (line != null) {
			wikidataId = line.split(",")[0];
	        count = line.split(",")[1];
	        curId = Integer.parseInt(line.split(",")[2]);
	        
	        Runnable worker = new GenerateDistributions(getDirFeature(), getRelName(),
					wiki, wikidataId, count, curId, getFreq());
	        executor.execute(worker);
             
            line = br.readLine();
		}
		
		// This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        
        long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
		
		br.close();
	}
	
	public void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public void removeOldFeatureFiles() throws IOException {
		ensureDirectory(new File(dirFeature));
		File dist = new File(dirFeature + relName + "_dist_cardinality.data");
		Files.deleteIfExists(dist.toPath());
	}

	public String getInputCsvFile() {
		return inputCsvFile;
	}

	public void setInputCsvFile(String inputCsvFile) {
		this.inputCsvFile = inputCsvFile;
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

	public int getFreq() {
		return freq;
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}
}
