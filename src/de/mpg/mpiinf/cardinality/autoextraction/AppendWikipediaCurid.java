package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AppendWikipediaCurid implements Runnable {
	
	private String wikidataId;
	private String tripleCount;
	private String outFilePath;
	private WikipediaArticle wiki;
	
	public AppendWikipediaCurid(WikipediaArticle wiki, String wikidataId, String tripleCount, String outFilePath) {
		this.setWiki(wiki);
		this.setWikidataId(wikidataId);
		this.setTripleCount(tripleCount);
		this.setOutFilePath(outFilePath);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (this.getWiki().getWikibaseMap().containsKey(this.getWikidataId())) {
			String curIds = this.getWiki().getWikibaseMap().get(this.getWikidataId());
			String article = "";
			for (String curId : curIds.split("\\|")) {
				article = this.getWiki().fetchArticle(Integer.parseInt(curId));
				if (!article.equals("")) {				
					
					synchronized (this) {
						try {
							PrintWriter outfile = new PrintWriter(new BufferedWriter(new FileWriter(this.getOutFilePath(), true)));
							outfile.println(this.getWikidataId() + "," + this.getTripleCount() + "," + curId);
			    			outfile.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    		}
					
					break;
				}
			}
		} else {
			System.err.println("No Wikipedia curid found for " + this.getWikidataId() + ".");
		}
	}

	public WikipediaArticle getWiki() {
		return wiki;
	}

	public void setWiki(WikipediaArticle wiki) {
		this.wiki = wiki;
	}

	public String getWikidataId() {
		return wikidataId;
	}

	public void setWikidataId(String wikidataId) {
		this.wikidataId = wikidataId;
	}

	public String getOutFilePath() {
		return outFilePath;
	}

	public void setOutFilePath(String outFilePath) {
		this.outFilePath = outFilePath;
	}

	public String getTripleCount() {
		return tripleCount;
	}

	public void setTripleCount(String tripleCount) {
		this.tripleCount = tripleCount;
	}

}
