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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;

public class DistributionExtractionConcurrent {
	
	private String inputCsvFile = "./data/example/wikidata_sample.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	private Double freqThreshold;
	private String delimiter;
	
	private static int NTHREADS = -999;
	
	public static void setNumberOfThreads(int n) {
		NTHREADS = n;
	}
	
	public DistributionExtractionConcurrent() {
		
	}
	
	public DistributionExtractionConcurrent(String inputCsvFilePath, String delimiter, String relationName, String dirOutput, Double freqThreshold) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
		this.setFreqThreshold(freqThreshold);
		this.setDelimiter(delimiter);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		DistributionExtractionConcurrent featExtraction;
		if (args.length < 4) {
			featExtraction = new DistributionExtractionConcurrent();
		} else {
			featExtraction = new DistributionExtractionConcurrent(args[0], ",", args[1], args[2], Double.parseDouble(args[3]));
		}
		
		WikipediaArticle wiki = new WikipediaArticle();
		featExtraction.run(wiki);
	}
	
	public void run(WikipediaArticle wiki) throws IOException, InterruptedException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Generate number distributions... ");
		
		removeOldFeatureFiles();
		
		String line;
		String wikidataId = "", count = "", wikiLabel = "", popularScore = "";
		Integer curId;
		Double countOccur;
		
//		String basename = FilenameUtils.getBaseName(getInputCsvFile());
//		String extension = FilenameUtils.getExtension(getInputCsvFile());
//		String outputCsvFile = FilenameUtils.getFullPath(getInputCsvFile()) + basename + "_dist_freq" + "." + extension;
		String outputCsvFile = getInputCsvFile() + ".tmp";
		
		//******* Reading number distribution in the corpus per entity *******//
		
		Map<String, Double> histogram = new HashMap<String, Double>();
		int numSubjects = 0;
		BufferedReader brpre = new BufferedReader(new FileReader(getInputCsvFile()));
		line = brpre.readLine();
		while (line != null) {
			count = line.split(delimiter)[1];
			
			if (!histogram.containsKey(count)) histogram.put(count, 0.0);
			histogram.put(count, histogram.get(count) + 1.0);
			numSubjects ++;
			
			line = brpre.readLine();
		}
		
		for (String c : histogram.keySet()) {
			histogram.put(c, histogram.get(c)/numSubjects);
		}
		
		brpre.close();
		
		//******* Reading number distribution in the text *******//
		
		BufferedReader br = new BufferedReader(new FileReader(getInputCsvFile()));
		
		int numQuartLine = 0;
		int quarter = numSubjects / 4;
		int numQuartile = 1;
		
		line = br.readLine();
		
		//First wikidataId starts...
		wikidataId = line.split(delimiter)[0];
        count = line.split(delimiter)[1];
        curId = Integer.parseInt(line.split(delimiter)[2]);
        wikiLabel = line.split(delimiter)[3];
        popularScore = line.split(delimiter)[4];
        countOccur = histogram.get(count);
        
        GenerateDistributions ext = new GenerateDistributions(outputCsvFile, getRelName(),
				wiki, wikidataId, count, curId, wikiLabel, popularScore, countOccur, numQuartile, getFreqThreshold(), delimiter);
		ext.run();
		//Done. Next WikidataIds...
		
		numQuartLine ++;
		
		line = br.readLine();
		
		ExecutorService executor;
		if (NTHREADS < 0) {
			executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		} else {
			executor = Executors.newFixedThreadPool(NTHREADS);
		}
		
		while (line != null) {
			wikidataId = line.split(delimiter)[0];
	        count = line.split(delimiter)[1];
	        curId = Integer.parseInt(line.split(delimiter)[2]);
	        wikiLabel = line.split(delimiter)[3];
	        popularScore = line.split(delimiter)[4];
	        countOccur = histogram.get(count);
	        
	        Runnable worker = new GenerateDistributions(outputCsvFile, getRelName(),
					wiki, wikidataId, count, curId, wikiLabel, popularScore, countOccur, numQuartile, getFreqThreshold(), delimiter);
	        executor.execute(worker);
	        
	        numQuartLine++;
	        if (numQuartLine == quarter) {
	        	numQuartile ++;
	        	numQuartLine = 0;
	        }
             
            line = br.readLine();
		}
		
		// This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		
		br.close();
		
		// Once everything is complete, delete old file..
		File oldFile = new File(getInputCsvFile());
		oldFile.delete();

		// And rename tmp file's name to old file name
		File newFile = new File(getInputCsvFile() + ".tmp");
		newFile.renameTo(oldFile);
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
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

	public Double getFreqThreshold() {
		return freqThreshold;
	}

	public void setFreqThreshold(Double freqThreshold) {
		this.freqThreshold = freqThreshold;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
}
