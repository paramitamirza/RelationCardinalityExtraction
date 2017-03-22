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

public class FeatureExtractionConcurrent {
	
	private String inputCsvFile = "./data/example/wikidata_sample.csv";
	private String inputRandomCsvFile = "./data/example/wikidata_sample_random10.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	
	private static int NTHREADS = -999;
	
	public static void setNumberOfThreads(int n) {
		NTHREADS = n;
	}
	
	public FeatureExtractionConcurrent() {
		
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, String relationName, String dirOutput) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile("");
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, int nRandom, String relationName, String dirOutput) throws IOException {
		this.setInputCsvFile(inputCsvFilePath);
		this.generateRandomInstances(nRandom);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, String inputRandomCsvFilePath, String relationName, String dirOutput) {
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
		
		WikipediaArticle wiki = new WikipediaArticle();
		featExtraction.run(wiki, true, false, 0, false, false, false);
	}
	
	public void run(WikipediaArticle wiki, boolean nummod, boolean compositional, int threshold,
			boolean transform, boolean transformZeroOne,
			boolean ignoreHigher) throws IOException, InterruptedException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Generate feature file (in column format) for CRF++... ");
		
		removeOldFeatureFiles();
		
		List<String> testInstances = new ArrayList<String>();
		if (!getInputRandomCsvFile().equals("")) {
			testInstances = readRandomInstances(getInputRandomCsvFile());
		}
		String line;
		String wikidataId = "", count = "";
		Integer curId;
		boolean training;
		
		BufferedReader br = new BufferedReader(new FileReader(getInputCsvFile()));
		line = br.readLine();
		
		//First wikidataId starts...
		wikidataId = line.split(",")[0];
        count = line.split(",")[1];
        curId = Integer.parseInt(line.split(",")[2]);
        
		training = true;
        if (testInstances.contains(wikidataId)) {
			training = false;
		} 
		
		GenerateFeatures ext = new GenerateFeatures(getDirFeature(), getRelName(),
				wiki, wikidataId, count, curId, training,
        		nummod, compositional, threshold,
        		transform, transformZeroOne,
        		ignoreHigher);
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
	        
	        training = true;
	        if (testInstances.contains(wikidataId)) {
				training = false;
			} 
	        
	        Runnable worker = new GenerateFeatures(getDirFeature(), getRelName(),
	        		wiki, wikidataId, count, curId, training,
	        		nummod, compositional, threshold,
	        		transform, transformZeroOne,
	        		ignoreHigher);
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
		File train = new File(dirFeature + relName + "_train_cardinality.data");
		File test = new File(dirFeature + relName + "_test_cardinality.data");
		Files.deleteIfExists(train.toPath());
		Files.deleteIfExists(test.toPath());
	}
	
	public List<String> readRandomInstances(String inputFile) throws IOException {
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
	
	public void generateRandomInstances(int nRandom) throws IOException {
		this.setInputRandomCsvFile(this.getInputCsvFile().replace(".csv", "_random"+nRandom+".csv"));
		BufferedReader br = new BufferedReader(new FileReader(this.getInputCsvFile()));
		BufferedWriter bwr = new BufferedWriter(new FileWriter(this.getInputRandomCsvFile()));
		List<Integer> randomList = new ArrayList<Integer>();
		if (nRandom > 0) {
			LineNumberReader lnr = new LineNumberReader(new FileReader(this.getInputCsvFile()));
			Stack<Integer> randomPool = new Stack<Integer>();
			int linenumber = 0;
			while (lnr.readLine() != null) {
				randomPool.add(linenumber);
				linenumber++;
			}
			Collections.shuffle(randomPool);
			randomList = randomPool.subList(0, nRandom);
			lnr.close();
		}
		
		String eid = "", count = "";
		String line = br.readLine();	
		int n = 0;
		while (line != null) {
			eid = line.split(",")[0];
			count = line.split(",")[1];
				
			if (randomList.contains(n)) {
				bwr.write(eid + "," + count);
				bwr.newLine();
			}
			
			line = br.readLine();
			n ++;
		}
		br.close();
		bwr.close();
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
