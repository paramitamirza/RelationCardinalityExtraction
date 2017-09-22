package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

public class WikipediaArticle {
	
	private String wikiDir = "/home/paramita/D5data-8/RCE_pipeline/enwiki_20170320_pages_articles/";
	private String zindexDir = "/home/paramita/D5data-8/RCE_pipeline/enwiki_20170320_pages_articles/zindex/";
	private String wikibaseMapFile = "/home/paramita/D5data-8/RCE_pipeline/enwiki_20170320_pages_articles/wikibase_item.txt.gz";
	
	private NavigableMap<Integer, String> wikiIndex;
	private Map<String, String> wikibaseMap;
	
	private static int NTHREADS = -999;
	
	public static void setNumberOfThreads(int n) {
		NTHREADS = n;
	}
	
	public WikipediaArticle() throws IOException {
		
		wikiIndex = new TreeMap<Integer, String>();
		loadWikiIndex();
		
		wikibaseMap = new HashMap<String, String>();
	}
	
	public WikipediaArticle(String wikiDir, String zindexDir, String wikibaseMapFile) throws IOException {
		this.setWikiDir(wikiDir);
		this.setZindexDir(zindexDir);
		this.setWikibaseMapFile(wikibaseMapFile);
		
		wikiIndex = new TreeMap<Integer, String>();
		loadWikiIndex();
		
		wikibaseMap = new HashMap<String, String>();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		WikipediaArticle wa = new WikipediaArticle();
		
//		System.out.println(wa.fetchArticle(7256246));
		
//		System.out.println("Load Wikidata Id to Wikipedia article mapping...");
		wa.mapWikidataWikipediaCurId();
//		System.out.println(wa.fetchArticleFromWikidataId("Q7721753"));

		wa.appendCurId("./data/example/children2.csv", ",");
//		wa.appendCurIdWithoutMap("./data/example/children2.csv");
	}
	
	public void appendCurId(String inputCsvFilePath, String delimiter) throws IOException, InterruptedException {	
		
		long startTime = System.currentTimeMillis();
		System.out.print("Append " + new File(inputCsvFilePath).getName() + " file with Wikipedia curId... ");
		
		BufferedReader br = new BufferedReader(new FileReader(inputCsvFilePath));
		String eid = "", count = "", label = "", score = "";
		String line = br.readLine();	
		
		ExecutorService executor;
		if (NTHREADS < 0) {
			executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		} else {
			executor = Executors.newFixedThreadPool(NTHREADS);
		}
		
		while (line != null) {
			eid = line.split(delimiter)[0];
			count = line.split(delimiter)[1];
			label = line.split(delimiter)[2];
			score = line.split(delimiter)[3];
			
			Runnable worker = new AppendWikipediaCurid(this, eid, count, label, score, inputCsvFilePath + ".tmp", delimiter);
			executor.execute(worker);
			
			line = br.readLine();
		}
		
		// This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        
        br.close();
		
		// Once everything is complete, delete old file..
		File oldFile = new File(inputCsvFilePath);
		oldFile.delete();

		// And rename tmp file's name to old file name
		File newFile = new File(inputCsvFilePath + ".tmp");
		newFile.renameTo(oldFile);
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
	}
	
	public void appendCurIdWithoutMap(String inputCsvFilePath) throws IOException {	
		
		long startTime = System.currentTimeMillis();
		System.out.print("Append .csv file with Wikipedia curId... ");
		
		BufferedReader br = new BufferedReader(new FileReader(inputCsvFilePath));
		BufferedWriter bw = new BufferedWriter(new FileWriter(inputCsvFilePath.replace(".csv", ".tmp")));
		String eid = "", count = "", label = "";
		String line = br.readLine();	
		
		while (line != null) {
			eid = line.split(",")[0];
			count = line.split(",")[1];
			label = line.split(",")[2];
			
			String curIds = this.fetchCurId(eid);
			String article = "";
			for (String curId : curIds.split("\\|")) {
				article = fetchArticle(Integer.parseInt(curId));
				if (!article.equals("")) {
					bw.write(eid + "," + count + "," + curId + "," + label);
					bw.newLine();
					break;
				}
			}
			line = br.readLine();
		}
		destroyMapping();
		br.close();
		bw.close();
		
		// Once everything is complete, delete old file..
		File oldFile = new File(inputCsvFilePath);
		oldFile.delete();

		// And rename tmp file's name to old file name
		File newFile = new File(inputCsvFilePath.replace(".csv", ".tmp"));
		newFile.renameTo(oldFile);
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
	}
	
	public void destroyMapping() {
		wikibaseMap = null;
	}
	
	public void mapWikidataWikipediaCurId() throws IOException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Load Wikidata Id to Wikipedia article mapping... ");
		
		BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(this.getWikibaseMapFile()))
                    ));
		String line = br.readLine();
		while (line != null) {
			wikibaseMap.put(line.split(",")[0], line.split(",")[1]);
			line = br.readLine();
		}
		br.close();
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
	}
	
	public String fetchArticleFromWikidataId(String wdId) {
		String curIds = wikibaseMap.get(wdId);
		String article = "";
		for (String curId : curIds.split("\\|")) {
			article = fetchArticle(Integer.parseInt(curId));
			if (!article.equals("")) return article;
		}
		return "";
	}
	
	public NavigableMap<Integer, String> getWikiIndex() {
		return wikiIndex;
	}

	public void setWikiIndex(NavigableMap<Integer, String> wikiIndex) {
		this.wikiIndex = wikiIndex;
	}

	public Map<String, String> getWikibaseMap() {
		return wikibaseMap;
	}

	public void setWikibaseMap(Map<String, String> wikibaseMap) {
		this.wikibaseMap = wikibaseMap;
	}

	public String fetchCurId(String wikidataId) {
		try {
	    	ProcessBuilder builder = new ProcessBuilder("zgrep", "-m", "1", wikidataId, this.getWikibaseMapFile());
	        Process process = builder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
            String line = bufferedReader.readLine();
            
            if (!line.equals("")) return line.split(",")[1];
            
            inputStream.close();
            bufferedReader.close();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		return null;
	}
	
	public String fetchArticle(Integer curId) {
		Integer key = wikiIndex.floorKey(curId);
		String article = "";
		
		String zqFile = zindexDir + "zq";
	    try {
	    	ProcessBuilder builder = new ProcessBuilder(zqFile, wikiDir + wikiIndex.get(key), curId.toString());
	        Process process = builder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                article += line;
            }
            inputStream.close();
            bufferedReader.close();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    if (!article.trim().isEmpty()) {
	    	JSONObject obj = new JSONObject(article.trim());
	    	return obj.getString("text");
	    } else {
	    	return "";
	    }
	}
	
	private void loadWikiIndex() throws IOException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Load Wikipedia article index... ");
		
		String indexFile = "articles-index.txt";
		BufferedReader br = new BufferedReader(new FileReader(wikiDir + "/" + indexFile));
		String line = br.readLine();
		while (line != null) {
			wikiIndex.put(Integer.parseInt(line.split(",")[0]), line.split(",")[1]);
			line = br.readLine();
		}
		br.close();
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
	}

	public String getWikiDir() {
		return wikiDir;
	}

	public void setWikiDir(String wikiDir) {
		this.wikiDir = wikiDir;
	}
	
	public String getZindexDir() {
		return zindexDir;
	}

	public void setZindexDir(String zindexDir) {
		this.zindexDir = zindexDir;
	}

	public String getWikibaseMapFile() {
		return wikibaseMapFile;
	}

	public void setWikibaseMapFile(String wikibaseMapFile) {
		this.wikibaseMapFile = wikibaseMapFile;
	}

}
