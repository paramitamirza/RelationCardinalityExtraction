package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class AddWikipediaTitle {
	
	private String inputCsvFile = "./data/auto_extraction/wikidata_sample.csv";
	private String outputCsvFile = "./data/auto_extraction/wikidata_sample_links.csv";
	private String wikipediaLinkFile = "./data/auto_extraction/english_links.txt.gz";
	
	public AddWikipediaTitle() {
		
	}
	
	public AddWikipediaTitle(String inputCsvFilePath, String outputCsvFilePath, String wikiLinkFilePath) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setOutputCsvFile(outputCsvFilePath);
		this.setWikipediaLinkFile(wikiLinkFilePath);
	}
	
	public static void main(String[] args) throws Exception {
		
		AddWikipediaTitle addWikiTitle;
		if (args.length < 3) {
			addWikiTitle = new AddWikipediaTitle();
		} else {
			addWikiTitle = new AddWikipediaTitle(args[0], args[1], args[2]);
		}
		
		System.out.println("Read Wikidata-to-WikipediaTitle mapping file...");
		Map<String, String> wikiLinks = addWikiTitle.mapWikidataWikipediaTitle();
		
		BufferedReader br = new BufferedReader(new FileReader(addWikiTitle.getInputCsvFile()));
		BufferedWriter bw = new BufferedWriter(new FileWriter(addWikiTitle.getOutputCsvFile()));
		String eid = "", label = "", count = "";
		String line = br.readLine();	
		
		while (line != null) {
			eid = line.split(",")[0];
			count = line.split(",")[1];
			if (wikiLinks.containsKey(eid)) {
				label = wikiLinks.get(eid);
				bw.write(eid + "," + label + "," + count);
				bw.newLine();
				System.out.println(eid + "\t" + count + "\t" + label);
			} else {
				System.out.println(eid + "\t" + count + "\tNO_ENGLISH_WIKI");
			}
			
			line = br.readLine();
		}
		
		br.close();
		bw.close();
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

	public String getOutputCsvFile() {
		return outputCsvFile;
	}

	public void setOutputCsvFile(String outputCsvFile) {
		this.outputCsvFile = outputCsvFile;
	}

	public String getWikipediaLinkFile() {
		return wikipediaLinkFile;
	}

	public void setWikipediaLinkFile(String wikipediaLinkFile) {
		this.wikipediaLinkFile = wikipediaLinkFile;
	}
	

}
