package de.mpg.mpiinf.cardinality.autoextraction;

public class AppendWikipediaCurid implements Runnable {
	
	private String wikidataId;
	private String tripleCount;
	private String label;
	private String score;
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	private String outFilePath;
	private WikipediaArticle wiki;
	
	public AppendWikipediaCurid(WikipediaArticle wiki, String wikidataId, String tripleCount, String label, String score, String outFilePath) {
		this.setWiki(wiki);
		this.setWikidataId(wikidataId);
		this.setTripleCount(tripleCount);
		this.setLabel(label);
		this.setScore(score);
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
					
					WriteToFile.getInstance().appendContents(this.getOutFilePath(), 
		    				this.getWikidataId() + "," + this.getTripleCount() + "," 
		    						+ curId + "," 
		    						+ this.getLabel() + "," 
		    						+ this.getScore() + "\n");
					
					break;
				}
			}
//		} else {
//			System.err.println("No Wikipedia curid found for " + this.getWikidataId() + ".");
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

	public String getScore() {
		return score;
	}

	public void setScore(String score) {
		this.score = score;
	}

}
