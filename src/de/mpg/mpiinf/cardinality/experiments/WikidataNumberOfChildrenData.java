package de.mpg.mpiinf.cardinality.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

public class WikidataNumberOfChildrenData {
	
	public String wikiArticleTitlesPath = "D:/Wikipedia/wiki_article_titles_line_num.txt";
	public String wikiArticlesPath = "D:/Wikipedia/wiki_articles_in_plain_text.txt";
	public String wikiArticlesDirPath = "./data/auto-extraction/articles/";
	public TreeMap<String, String> articleTitles = new TreeMap<String, String>();
	
	public WikidataNumberOfChildrenData() {
		
	}
	
	public WikidataNumberOfChildrenData(String wikiArticleTitlesPath, String wikiArticlesPath, String wikiArticlesDirPath) {
		this.wikiArticleTitlesPath = wikiArticleTitlesPath;
		this.wikiArticlesPath = wikiArticlesPath;
		this.wikiArticlesDirPath = wikiArticlesDirPath;
	}

	public static void main(String[] args) throws Exception {
//		WikidataNumberOfChildrenData numOfChildren = new WikidataNumberOfChildrenData();
		WikidataNumberOfChildrenData numOfChildren = new WikidataNumberOfChildrenData(args[0], args[1], args[2]);
		
		System.out.println("Load article titles and corresponding line numbers from file...");
		numOfChildren.extractArticleTitlesFromFile();
		
		numOfChildren.matchNumberOfChildrenTrain(numOfChildren.wikiArticlesDirPath);
	}
	
	public void extractArticleTitlesFromFile() throws IOException, ClassNotFoundException {
		BufferedReader br = new BufferedReader(new FileReader(wikiArticleTitlesPath));
		String line;
		String articleTitle = "", wikidataId = "", wikidataInstance = "";
		int startLine = 0, endLine = 0;
		while ((line = br.readLine()) != null) {
			String[] cols = line.split("\t");			
			endLine = Integer.parseInt(cols[0])-1;
			if (!articleTitle.equals("")) {
				articleTitles.put(articleTitle, startLine + "," + endLine);
			}
			startLine = Integer.parseInt(cols[0]);
			articleTitle = cols[1];
	    }
		br.close();
	}
	
	public void matchNumberOfChildrenGold() throws JSONException, FileNotFoundException {
		//Get people with number of chidren property
		String q = "SELECT ?parent ?name ?numOfChildren ?countChildren WHERE { "
		 + "{ "
		 + "  ?parent wdt:P1971 ?numOfChildren . "
		 + "  ?parent wdt:P31 wd:Q5 . "
		 + "  OPTIONAL { "
		 + "    ?parent rdfs:label ?name filter (lang(?name) = \"en\") . "
		 + "  } "
		 + "  { " 
		 + "    SELECT ?parent (COUNT(?child) AS ?countChildren) WHERE { "
		 + "      ?parent wdt:P40 ?child . "
		 + "    } GROUP BY ?parent "
		 + "  } "
		 + "} "
		 + "UNION "
		 + "{ " 
		 + "  { " 
		 + "    ?parent wdt:P1971 ?numOfChildren . " 
		 + "    ?parent wdt:P31 wd:Q5 . "
		 + "    OPTIONAL { "
		 + "      ?parent rdfs:label ?name filter (lang(?name) = \"en\") . "
		 + "    } "
		 + "  } " 
		 + "  MINUS " 
		 + "  { "
		 + "    SELECT ?parent WHERE { "
		 + "      ?parent wdt:P40 ?child . "
		 + "    } GROUP BY ?parent "
		 + "  } " 
		 + "} "
		 + "}";
		
		JSONObject initialRes = SPARQLQuery.execute(q);
		JSONArray res = initialRes.getJSONObject ("results").getJSONArray("bindings");
		
		PrintWriter goldData = new PrintWriter(new File("./data/auto-extraction/gold-cardinality.txt"));
		
		String entity = "", name = "";
		int numChild, countChild, match = 0;
		for (int i=0; i<res.length(); i++) {
			if (res.getJSONObject(i).has("name")) {
				entity = res.getJSONObject(i).getJSONObject("parent").getString("value");
				name = res.getJSONObject(i).getJSONObject("name").getString("value");
				numChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("numOfChildren").getString("value"));
				if (res.getJSONObject(i).isNull("countChildren")) {
					countChild = 0;
				} else {
					countChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("countChildren").getString("value"));
				}
				//System.out.println(entity + "\t" + numChild + "\t" + countChild);
				
				if (numChild == countChild) {
					goldData.write(entity + "\t" + name + "\t" + numChild + "\n");
					match ++;
				}
			}
		}
		System.out.println(match);
		goldData.close();
	}
	
	private Boolean containNumber(String str) {
		String hasNumRegex = "\\b(\\d+)\\b";
		Pattern hasNum = Pattern.compile(hasNumRegex);
		String hasNumStrRegex = "\\b((twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)*[ -]?(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety))\\b";
		Pattern hasNumStr = Pattern.compile(hasNumStrRegex);
		
		Matcher mNum = hasNum.matcher(str);
		Matcher mNumStr = hasNumStr.matcher(str);
        if (mNum.find() || mNumStr.find()) {
        	return true;
        }
        
        return false;
		
	}
	
	private List<String> filterText(String inputFile) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader (
		new FileInputStream(inputFile)));
		List<String> filtered = new ArrayList<String>();
		
		String line;
		while ((line = br.readLine()) != null) {
			if (containNumber(line)) {
				Document doc = new Document(line);
				
				for (Sentence sent : doc.sentences()) {
					if (containNumber(sent.text())) {
						boolean numberFound = false;
						for (int i=0; i<sent.words().size(); i++) {
//							System.out.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
							if (sent.posTag(i).equals("CD")
									&& !sent.nerTag(i).equals("MONEY")
									&& !sent.nerTag(i).equals("PERCENT")
									&& !sent.nerTag(i).equals("DATE")
									&& !sent.nerTag(i).equals("TIME")
									&& !sent.nerTag(i).equals("DURATION")) {
								numberFound = true;
							}
						}
						if (numberFound) {
//							System.out.println(sent.text());
							filtered.add(sent.text());
						}
					}
		        }
				
				
			}
	    }
		br.close();
		return filtered;
	}
	
	public void matchNumberOfChildrenTrain(String articlePath) throws JSONException, IOException, InterruptedException {
		//Get people with number of chidren property
		String q = "SELECT ?parent ?name ?numOfChildren ?countChildren WHERE { "
		 + "{ "
		 + "  ?parent wdt:P1971 ?numOfChildren . "
		 + "  ?parent wdt:P31 wd:Q5 . "
		 + "  OPTIONAL { "
		 + "    ?parent rdfs:label ?name filter (lang(?name) = \"en\") . "
		 + "  } "
		 + "  { " 
		 + "    SELECT ?parent (COUNT(?child) AS ?countChildren) WHERE { "
		 + "      ?parent wdt:P40 ?child . "
		 + "    } GROUP BY ?parent "
		 + "  } "
		 + "} "
		 + "UNION "
		 + "{ " 
		 + "  { " 
		 + "    ?parent wdt:P1971 ?numOfChildren . " 
		 + "    ?parent wdt:P31 wd:Q5 . "
		 + "    OPTIONAL { "
		 + "      ?parent rdfs:label ?name filter (lang(?name) = \"en\") . "
		 + "    } "
		 + "  } " 
		 + "  MINUS " 
		 + "  { "
		 + "    SELECT ?parent WHERE { "
		 + "      ?parent wdt:P40 ?child . "
		 + "    } GROUP BY ?parent "
		 + "  } " 
		 + "} "
		 + "}";
		
		JSONObject initialRes = SPARQLQuery.execute(q);
		JSONArray res = initialRes.getJSONObject ("results").getJSONArray("bindings");
		
//		PrintWriter goldData = new PrintWriter(new File("train-cardinality.txt"));
//		PrintWriter sedCommand = new PrintWriter(new File("train-cardinality.sh"));
		
		String entity = "", eid = "", name = "", start = "", end = "";
		int numChild, countChild, match = 0;
		JSONArray result = new JSONArray();
		for (int i=0; i<res.length(); i++) {
			if (res.getJSONObject(i).has("name")) {
				entity = res.getJSONObject(i).getJSONObject("parent").getString("value");
				eid = entity.replace("http://www.wikidata.org/entity/", "");
				name = res.getJSONObject(i).getJSONObject("name").getString("value");
				numChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("numOfChildren").getString("value"));
				if (res.getJSONObject(i).isNull("countChildren")) {
					countChild = 0;
				} else {
					countChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("countChildren").getString("value"));
				}
				//System.out.println(entity + "\t" + numChild + "\t" + countChild);
				
				if (
						numChild > 0
//						numChild > 1
						&& numChild == countChild) {
					if (articleTitles.containsKey(name)) {
//						goldData.write(eid + "\t" + numChild + "\n");
						
						start = articleTitles.get(name).split(",")[0];
						end = articleTitles.get(name).split(",")[1];
//						sedCommand.write("sed -n '" + start + "," + end + "p;" + end + "q' " + wikiArticlesPath + " > ./articles/" + eid + ".txt\n");
						System.out.println("sed -n '" + start + "," + end + "p;" + end + "q' " + wikiArticlesPath + " > ./articles/" + eid + ".txt");
						
						if (!new File(articlePath + eid + ".txt").exists()) { 
							Process p = Runtime.getRuntime().exec("sed -n '" + start + "," + end + "p;" + end + "q' " + wikiArticlesPath + " > ./articles/" + eid + ".txt\n");
							p.waitFor();
							
						}

						List<String> articleText = filterText(articlePath + eid + ".txt");
//							System.out.println(eid + "\t" + numChild + "\t" + StringUtils.join(articleText, "|"));
						
						JSONObject obj = new JSONObject();
						obj.put("wikidata-id", eid);
						obj.put("wikidata-label", name);
						obj.put("num-child", numChild);

						JSONArray list = new JSONArray();
						for (String s : articleText) {
							list.put(s);
						}
						obj.put("article-num-only", list);
						result.put(obj);
						
						match ++;
					}
				}
			}
		}
		try {

			FileWriter file = new FileWriter("test-cardinality.json");
			file.write(result.toString(4));
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(match);
//		goldData.close();
//		sedCommand.close();
	}
}
