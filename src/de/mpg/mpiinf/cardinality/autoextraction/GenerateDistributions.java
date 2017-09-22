package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class GenerateDistributions implements Runnable {
	
	private String outputCsvFile;
	private String relName;
	
	private WikipediaArticle wiki;
	
	private String wikidataId;
	private String count;
	private Integer curId;
	private String wikiLabel;
	private String popularScore;
	private Double countOccur;
	private Integer numQuartile;
	private String delimiter;
	
	private Double freqThreshold;
	private Map<Long, Integer> numDistributions;
	
	public GenerateDistributions(String outputCsvFile, String relName,
			WikipediaArticle wiki, String wikidataId, String count, Integer curId, 
			String wikiLabel, String popularScore, Double countOccur, Integer numQuartile,
			Double freqThreshold, String delimiter) {
		this.setOutputCsvFile(outputCsvFile);
		this.setRelName(relName);
		
		this.setWiki(wiki);
		
		this.setWikidataId(wikidataId);
		this.setCount(count);
		this.setCurId(curId);
		this.setWikiLabel(wikiLabel);
		this.setPopularScore(popularScore);
		this.setCountOccur(countOccur);
		this.setNumQuartile(numQuartile);
		this.setDelimiter(delimiter);
		
		this.setFreqThreshold(freqThreshold);
		this.setNumDistributions(new HashMap<Long, Integer>());
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		
//		featExtraction.generateColumnsFile(true, false, 0);
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub		
        try {
        	int numOfTriples = Integer.parseInt(this.getCount());
    		String wikipediaText = this.getWiki().fetchArticle(this.getCurId());
			
			if (wikipediaText != "") {
				
				String original;
	    		Sentence sent;
	    		StringBuilder toPrint = new StringBuilder();
	    		
	    		int j=0;
	    		Transform trans = new Transform();
	    		
	    		for (String l : wikipediaText.split("\\r?\\n")) {	//Split the paragraphs
	    			Document doc = new Document(l);
	    			
	    			for (Sentence s : doc.sentences()) {	//Split the sentences
	    				
	    				generateDistributions(s, j);	    				
	    				j ++;
	    	        }
	    	    }
	    		
	    		List<Entry<Long, Integer>> dist = entriesSortedByValues(this.getNumDistributions());
	    		
	    		//numbers occurring only once
	    		String toWriteMoreThanOne = "";
	    		for (Entry<Long, Integer> en : dist) {
	    			if (en.getValue() > 1) {
	    				toWriteMoreThanOne += en.getKey() + ";";
	    			} 
	    		}
	    		if (!toWriteMoreThanOne.isEmpty()) toWriteMoreThanOne = toWriteMoreThanOne.substring(0, toWriteMoreThanOne.length()-1);
	    		toWriteMoreThanOne = "[" + toWriteMoreThanOne + "]";
	    		
	    		//numbers occurring frequently
	    		String toWriteFrequent1 = "";	//< 1 information
	    		String toWriteFrequent2 = "";	//< 1.5 information
	    		String toWriteFrequent3 = "";	//< 2 information
	    		String toWriteFrequent4 = "";	//< 2.5 information
	    		
	    		Double numTotalOccurrences = 0.0;
	    		for (Entry<Long, Integer> en : dist) {
	    			numTotalOccurrences += en.getValue();		
	    		}
	    		
//	    		for (Entry<Long, Integer> en : dist) {
//	    			if ((en.getValue() / numTotalOccurrences) >= this.getFreqThreshold()) {
//	    				toWriteFrequent1 += en.getKey() + ";";
//	    			} 
//	    		}
	    		
	    		for (Entry<Long, Integer> en : dist) {
	    			double information = -Math.log(en.getValue() / numTotalOccurrences);
	    			
	    			if (en.getValue() > 1 && information < 1) toWriteFrequent1 += en.getKey() + ";";
	    			if (en.getValue() > 1 && information < 1.5) toWriteFrequent2 += en.getKey() + ";";
	    			if (en.getValue() > 1 && information < 2) toWriteFrequent3 += en.getKey() + ";";
	    			if (en.getValue() > 1 && information < 2.5) toWriteFrequent4 += en.getKey() + ";";
	    		}
	    		if (!toWriteFrequent1.isEmpty()) toWriteFrequent1 = toWriteFrequent1.substring(0, toWriteFrequent1.length()-1);
	    		toWriteFrequent1 = "[" + toWriteFrequent1 + "]";
	    		if (!toWriteFrequent2.isEmpty()) toWriteFrequent2 = toWriteFrequent2.substring(0, toWriteFrequent2.length()-1);
	    		toWriteFrequent2 = "[" + toWriteFrequent2 + "]";
	    		if (!toWriteFrequent3.isEmpty()) toWriteFrequent3 = toWriteFrequent3.substring(0, toWriteFrequent3.length()-1);
	    		toWriteFrequent3 = "[" + toWriteFrequent3 + "]";
	    		if (!toWriteFrequent4.isEmpty()) toWriteFrequent4 = toWriteFrequent4.substring(0, toWriteFrequent4.length()-1);
	    		toWriteFrequent4 = "[" + toWriteFrequent4 + "]";
	    		
	    		
	    		WriteToFile.getInstance().appendContents(getOutputCsvFile(), 
	    				this.getWikidataId() + delimiter
	    				+ this.getCount() + delimiter
	    				+ this.getCurId() + delimiter
	    				+ this.getWikiLabel() + delimiter
	    				+ this.getPopularScore() + delimiter
	    				// Extra information
	    				+ this.getCountOccur() + delimiter
	    				+ this.getNumQuartile() + delimiter
	    				+ toWriteMoreThanOne + delimiter
	    				+ toWriteFrequent1 + delimiter
	    				+ toWriteFrequent2 + delimiter
	    				+ toWriteFrequent3 + delimiter
	    				+ toWriteFrequent4 + "\n");
			}			
			
		} catch (JSONException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static <K,V extends Comparable<? super V>> 
    	List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

		List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());
		
		Collections.sort(sortedEntries, 
		    new Comparator<Entry<K,V>>() {
		        @Override
		        public int compare(Entry<K,V> e1, Entry<K,V> e2) {
		            return e2.getValue().compareTo(e1.getValue());
		        }
		    }
		);
		
		return sortedEntries;
	}
	
	private void generateDistributions(Sentence sent, int j) {
		String word = "", pos = "", ner = "";
		int k;		
		long numInt;
		
		for (k=0; k<sent.words().size(); k++) {
			pos = sent.posTag(k);
			ner = sent.nerTag(k);
			
			if (Numbers.properNumber(pos, ner)) {						
				word = ""; 
				
				while (k<sent.words().size()) {
					if (Numbers.properNumber(sent.posTag(k), sent.nerTag(k))) {
						word += sent.word(k) + "_";
						k++;
						
					} else {
						break;
					}
				}
				word = word.substring(0, word.length()-1);				
				numInt = Numbers.getInteger(word.toLowerCase());
				
				if (numInt > 0) {
					if (!this.getNumDistributions().containsKey(numInt)) this.getNumDistributions().put(numInt, 0);
					this.getNumDistributions().put(numInt, this.getNumDistributions().get(numInt) + 1);
				}
			}
		}
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep;
	}
	
	public String generateLine(String wikidataId, String sentId, String wordId, String word, String lemma, String pos, String ner, String dep, String label) {
		return wikidataId + "\t" + sentId + "\t" + wordId + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + ner + "\t" + dep + "\t" + label;
	}

	public String getWikidataId() {
		return wikidataId;
	}

	public void setWikidataId(String wikidataId) {
		this.wikidataId = wikidataId;
	}

	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		this.count = count;
	}
	
	public String getRelName() {
		return relName;
	}

	public void setRelName(String relName) {
		this.relName = relName;
	}

	public WikipediaArticle getWiki() {
		return wiki;
	}

	public void setWiki(WikipediaArticle wiki) {
		this.wiki = wiki;
	}

	public Integer getCurId() {
		return curId;
	}

	public void setCurId(Integer curId) {
		this.curId = curId;
	}

	public Map<Long, Integer> getNumDistributions() {
		return numDistributions;
	}

	public void setNumDistributions(Map<Long, Integer> numDistributions) {
		this.numDistributions = numDistributions;
	}

	public Double getFreqThreshold() {
		return freqThreshold;
	}

	public void setFreqThreshold(Double freqThreshold) {
		this.freqThreshold = freqThreshold;
	}

	public String getWikiLabel() {
		return wikiLabel;
	}

	public void setWikiLabel(String wikiLabel) {
		this.wikiLabel = wikiLabel;
	}

	public String getPopularScore() {
		return popularScore;
	}

	public void setPopularScore(String popularScore) {
		this.popularScore = popularScore;
	}

	public Double getCountOccur() {
		return countOccur;
	}

	public void setCountOccur(Double countOccur) {
		this.countOccur = countOccur;
	}

	public String getOutputCsvFile() {
		return outputCsvFile;
	}

	public void setOutputCsvFile(String outputCsvFile) {
		this.outputCsvFile = outputCsvFile;
	}

	public Integer getNumQuartile() {
		return numQuartile;
	}

	public void setNumQuartile(Integer numQuartile) {
		this.numQuartile = numQuartile;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
}
