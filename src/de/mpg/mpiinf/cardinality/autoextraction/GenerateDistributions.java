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

import org.json.*;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class GenerateDistributions implements Runnable {
	
	private String dirFeature;
	private String relName;
	
	private WikipediaArticle wiki;
	
	private String wikidataId;
	private String count;
	private Integer curId;
	
	private Map<Long, Integer> numDistributions;
	
	public GenerateDistributions(String dirFeature, String relName,
			WikipediaArticle wiki, String wikidataId, String count, Integer curId) {
		this.setDirFeature(dirFeature);
		this.setRelName(relName);
		
		this.setWiki(wiki);
		
		this.setWikidataId(wikidataId);
		this.setCount(count);
		this.setCurId(curId);
		
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
	    		
	    		String distFilePath = this.getDirFeature() + "/" + this.getRelName() + "_dist_freq_cardinality.data";
	    		String toWrite = "";
	    		List<Entry<Long, Integer>> dist = entriesSortedByValues(this.getNumDistributions());
	    		for (Entry<Long, Integer> en : dist) {
	    			if (en.getValue() >= 5) {
	    				toWrite += en.getKey() + ";";
	    			} 
	    		}
	    		if (!toWrite.isEmpty()) toWrite = toWrite.substring(0, toWrite.length()-1);
	    		toWrite = "[" + toWrite + "]";
	    		WriteToFile.getInstance().appendContents(distFilePath, this.getWikidataId() + "," + toWrite + "\n");
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
	
	public String getDirFeature() {
		return dirFeature;
	}

	public void setDirFeature(String dirFeature) {
		this.dirFeature = dirFeature;
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
}
