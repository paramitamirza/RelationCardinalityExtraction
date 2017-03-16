package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

public class AppendWikipediaTitle {
	
	private String inputCsvFile = "./data/auto_extraction/wikidata_sample.csv";
	private String wikipediaLinkFile = "./data/auto_extraction/english_links.txt.gz";
	
	public AppendWikipediaTitle() {
		
	}
	
	public AppendWikipediaTitle(String inputCsvFilePath, String wikiLinkFilePath) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setWikipediaLinkFile(wikiLinkFilePath);
	}
	
	public static void main(String[] args) throws Exception {
		
		AppendWikipediaTitle addWikiTitle;
		if (args.length < 2) {
			addWikiTitle = new AppendWikipediaTitle();
		} else {
			addWikiTitle = new AppendWikipediaTitle(args[0], args[1]);
		}
		
		addWikiTitle.append(0);
	}
	
	public void append(Integer nRandom) throws Exception{
		System.out.println("Read Wikidata-to-WikipediaTitle mapping file...");
		Map<String, String> wikiLinks = mapWikidataWikipediaTitle();
		
		BufferedReader br = new BufferedReader(new FileReader(this.getInputCsvFile()));
		BufferedWriter bw = new BufferedWriter(new FileWriter(this.getInputCsvFile().replace(".csv", ".tmp")));
		String eid = "", label = "", count = "";
		String line = br.readLine();	
		
		BufferedWriter bwr = new BufferedWriter(new FileWriter(this.getInputCsvFile().replace(".csv", "_random"+nRandom+".csv")));
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
		
		int n = 0;
		while (line != null) {
			eid = line.split(",")[0];
			count = line.split(",")[1];
			if (wikiLinks.containsKey(eid)) {
				label = wikiLinks.get(eid);
				bw.write(eid + "," + label + "," + count);
				bw.newLine();
				System.out.println(eid + "\t" + label + "\t" + count);
				
				if (randomList.contains(n)) {
					bwr.write(eid + "," + label + "," + count);
					bwr.newLine();
				}
					
			} else {
				System.out.println(eid + "\tNO_ENGLISH_WIKI" + "\t" + count);
			}
			
			line = br.readLine();
			n ++;
		}
		wikiLinks = null;
		br.close();
		bw.close();
		bwr.close();
		
		// Once everything is complete, delete old file..
		File oldFile = new File(this.getInputCsvFile());
		oldFile.delete();

		// And rename tmp file's name to old file name
		File newFile = new File(this.getInputCsvFile().replace(".csv", ".tmp"));
		newFile.renameTo(oldFile);
	}
	
	public Map<String, String> mapWikidataWikipediaTitle() throws FileNotFoundException, IOException {
		Map<String, String> mapping = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(this.getWikipediaLinkFile()))
                    ));
		String line = br.readLine();
		while (line != null) {
			mapping.put(line.split(",")[1], line.split(",")[0]);
			line = br.readLine();
		}
		br.close();
		
		return mapping;
	}

	public String getInputCsvFile() {
		return inputCsvFile;
	}

	public void setInputCsvFile(String inputCsvFile) {
		this.inputCsvFile = inputCsvFile;
	}

	public String getWikipediaLinkFile() {
		return wikipediaLinkFile;
	}

	public void setWikipediaLinkFile(String wikipediaLinkFile) {
		this.wikipediaLinkFile = wikipediaLinkFile;
	}
	

}
