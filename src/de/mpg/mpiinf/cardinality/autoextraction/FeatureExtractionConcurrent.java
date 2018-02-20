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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FeatureExtractionConcurrent {
	
	private String inputCsvFile = "./data/example/wikidata_sample.csv";
	private String inputRandomCsvFile = "./data/example/wikidata_sample_random10.csv";
	private String inputTrainCsvFile = "./data/example/wikidata_sample_train.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	private String delimiter;
	
	private static int NTHREADS = -999;
	
	public static void setNumberOfThreads(int n) {
		NTHREADS = n;
	}
	
	public FeatureExtractionConcurrent() {
		
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, String delimiter, String relationName, String dirOutput) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile("");
		this.setInputTrainCsvFile("");
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
		this.setDelimiter(delimiter);
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, String delimiter, int nRandom, String relationName, String dirOutput) throws IOException {
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile("");
		this.setInputTrainCsvFile("");
		this.generateRandomInstances(nRandom);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
		this.setDelimiter(delimiter);
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, String inputRandomCsvFilePath, String delimiter, String relationName, String dirOutput) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile(inputRandomCsvFilePath);
		this.setInputTrainCsvFile("");
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
		this.setDelimiter(delimiter);
	}
	
	public FeatureExtractionConcurrent(String inputCsvFilePath, String inputRandomCsvFilePath, String inputTrainCsvFilePath, String delimiter, String relationName, String dirOutput) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile("");
		this.setInputTrainCsvFile(inputTrainCsvFilePath);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
		this.setDelimiter(delimiter);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		FeatureExtractionConcurrent featExtraction;
		if (args.length < 4) {
			featExtraction = new FeatureExtractionConcurrent();
		} else {
			featExtraction = new FeatureExtractionConcurrent(args[0], args[1], args[2], args[3]);
		}
		
		WikipediaArticle wiki = new WikipediaArticle();
		featExtraction.run(wiki, false, -99, 0, 0, (float) 1.0, 0,
				true, false, false, 
				false, false, false, 
				false,
				false, false);
	}
	
	public void run(WikipediaArticle wiki, boolean ignoreHigher, int ignoreHigherLess,
			float infThreshold, int ignoreFreq, float topPopular, int quarterPart,			
			boolean nummod, boolean ordinal, boolean numterms,
			boolean articles, boolean quantifiers, boolean pronouns,
			boolean compositional, 
			boolean negation, boolean negTrain
			) throws IOException, InterruptedException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Generate feature file (in column format) for CRF++... ");
		
		removeOldFeatureFiles();
		
		List<String> testInstances = new ArrayList<String>();
		List<String> trainInstances = new ArrayList<String>();
		if (!this.getInputTrainCsvFile().equals("")) {
			trainInstances = readRandomInstances(this.getInputTrainCsvFile());
		}
		if (!this.getInputRandomCsvFile().equals("")) {
			testInstances = readRandomInstances(this.getInputRandomCsvFile());
		}
		String line;
		String wikidataId = "", count = "", freqNum = "", quarter = "", countDist = "";
		Integer curId;
		boolean training, isIgnoreFreq;
		
		//******* Reading maximum triple count in the dataset *******//
		
		int maxCount = 0, tripleCount = 0;;
		BufferedReader brpre = new BufferedReader(new FileReader(getInputCsvFile()));
		List<Integer> tripleCounts = new ArrayList<Integer>(); 
		line = brpre.readLine();
		while (line != null) {
			tripleCount = Integer.parseInt(line.split(delimiter)[1]);
			tripleCounts.add(tripleCount);
//			if (tripleCount >= maxCount) maxCount = tripleCount;
			line = brpre.readLine();
		}
		brpre.close();
		
		Collections.sort(tripleCounts);
		maxCount = tripleCounts.get(tripleCounts.size()*99/100);
		System.out.println("99%%tile = " + maxCount);
		
		BufferedReader br = new BufferedReader(new FileReader(getInputCsvFile()));
		
		int numTrain = ReadFromFile.countLines(this.getInputCsvFile());
		int maxNumTrain = Math.round(topPopular * numTrain);
//		int maxNumTrain = Math.round(topPopular * 4);
		
		Set<String> topPopularIds = new HashSet<String>();
		line = br.readLine();
		int idxId = 0;
		while (line != null
				&& idxId < maxNumTrain) {
			wikidataId = line.split(delimiter)[0];
			topPopularIds.add(wikidataId);
			line = br.readLine();
			idxId ++;
		}
		br.close();
		
		br = new BufferedReader(new FileReader(getInputCsvFile()));
		line = br.readLine();
		
		//First wikidataId starts...
		wikidataId = line.split(delimiter)[0];
        count = line.split(delimiter)[1];
        curId = Integer.parseInt(line.split(delimiter)[2]);
        
        if (line.split(delimiter).length > 5) {
	        countDist = line.split(delimiter)[5];
	        quarter = line.split(delimiter)[6];
        } else {
        	countDist = "0.0";
	        quarter = "0";
        }
        
        isIgnoreFreq = false;
        if (ignoreFreq >= 0) {
        	isIgnoreFreq = true;
        	if (ignoreFreq == 0) freqNum = line.split(delimiter)[7];
            else if (ignoreFreq == 1) freqNum = line.split(delimiter)[8];
            else if (ignoreFreq == 2) freqNum = line.split(delimiter)[9];
            else if (ignoreFreq == 3) freqNum = line.split(delimiter)[10];
            else if (ignoreFreq == 4) freqNum = line.split(delimiter)[11];
            else freqNum = "[]";
        } else {
        	freqNum = "[]";
        }
        
		training = true;
        if (testInstances.contains(wikidataId) 
//        		|| !trainInstances.contains(wikidataId)
        	) {
			training = false;
		} 
        if (training) {
        	if (topPopularIds.contains(wikidataId)) {
	        	if ((quarterPart == 0) 
	        			|| (quarterPart > 0 && quarterPart == Integer.parseInt(quarter))) {
	        		
					GenerateFeatures ext = new GenerateFeatures(getDirFeature(), getRelName(),
							wiki, wikidataId, count, curId, freqNum,
			        		training,
			        		ignoreHigher, ignoreHigherLess, 
			        		infThreshold, countDist,
			        		isIgnoreFreq, maxCount,
			        		nummod, ordinal, numterms, 
			        		articles, quantifiers, pronouns,
			        		compositional,
			        		negation, negTrain);
					ext.run();
	        	}
        	}
        } else {
        	GenerateFeatures ext = new GenerateFeatures(getDirFeature(), getRelName(),
					wiki, wikidataId, count, curId, freqNum,
	        		training,
	        		ignoreHigher, ignoreHigherLess, 
	        		infThreshold, countDist,
	        		isIgnoreFreq, maxCount,
	        		nummod, ordinal, numterms, 
	        		articles, quantifiers, pronouns,
	        		compositional,
	        		negation, negTrain);
			ext.run();
        }
		//Done. Next WikidataIds...
		
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
	        
	        if (line.split(delimiter).length > 5) {
		        countDist = line.split(delimiter)[5];
		        quarter = line.split(delimiter)[6];
	        } else {
	        	countDist = "0.0";
		        quarter = "0";
	        }
	        
	        isIgnoreFreq = false;
	        if (ignoreFreq >= 0) {
	        	isIgnoreFreq = true;
	        	if (ignoreFreq == 0) freqNum = line.split(delimiter)[7];
	            else if (ignoreFreq == 1) freqNum = line.split(delimiter)[8];
	            else if (ignoreFreq == 2) freqNum = line.split(delimiter)[9];
	            else if (ignoreFreq == 3) freqNum = line.split(delimiter)[10];
	            else if (ignoreFreq == 4) freqNum = line.split(delimiter)[11];
	        }   
	        
	        training = true;
	        if (testInstances.contains(wikidataId)) {
				training = false;
			} 
	        
	        if (training) {
	        	if (topPopularIds.contains(wikidataId)) {
		        	if ((quarterPart == 0) 
		        			|| (quarterPart > 0 && quarterPart == Integer.parseInt(quarter))) {
			        	Runnable worker = new GenerateFeatures(getDirFeature(), getRelName(),
								wiki, wikidataId, count, curId, freqNum,
				        		training,
				        		ignoreHigher, ignoreHigherLess, 
				        		infThreshold, countDist,
				        		isIgnoreFreq, maxCount,
				        		nummod, ordinal, numterms, 
				        		articles, quantifiers, pronouns,
				        		compositional,
				        		negation, negTrain);
				        executor.execute(worker);
		        	}
	        	}
	        } else {
	        	Runnable worker = new GenerateFeatures(getDirFeature(), getRelName(),
						wiki, wikidataId, count, curId, freqNum,
		        		training,
		        		ignoreHigher, ignoreHigherLess, 
		        		infThreshold, countDist,
		        		isIgnoreFreq, maxCount,
		        		nummod, ordinal, numterms, 
		        		articles, quantifiers, pronouns,
		        		compositional,
		        		negation, negTrain);
		        executor.execute(worker);
	        }
             
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
			randomInstances.add(line.split(delimiter)[0]);
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
			eid = line.split(delimiter)[0];
			count = line.split(delimiter)[1];
				
			if (randomList.contains(n)) {
				bwr.write(eid + delimiter + count);
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

	public String getInputTrainCsvFile() {
		return inputTrainCsvFile;
	}

	public void setInputTrainCsvFile(String inputTrainCsvFile) {
		this.inputTrainCsvFile = inputTrainCsvFile;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
}
