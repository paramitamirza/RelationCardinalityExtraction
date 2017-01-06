package it.unibz.inf.cardinality.statistics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import it.unibz.inf.cardinality.manualextraction.WikiArticle;

public class WikidataCardinalityPattern {

	
	private static String cardinalStatementsPath = "data/cardinal_statements_wikidata.txt";
	
	private HashSet<String> listOfHumans;
	private HashSet<String> listOfHumansPerOccupation;
	private HashSet<String> listOfDeadHumans;
	
	private Map<String, Integer> numOfChildren;
	private Map<String, Integer> countOfChildren;
	
	private Map<String, WikiArticle> cardinalStatements;
	private Map<String, Integer> lenArticles;
	
	public WikidataCardinalityPattern() throws IOException, JSONException {
		//List all humans in Wikidata
		getAllHumans();
		
		//Number of children property of humans in Wikidata
		numOfChildrenOfHumans();
		
		//Count children of humans in Wikidata
		countChildrenOfHumans();
	}
	
	public static void main(String[] args) {
		try {
			//List of humans in Wikidata per occupation
//			String[] occupation = {"Q82955", "Q937857", "Q33999", "Q36180", "Q1028181", "Q1930187", "Q1622272", "Q177220", "Q40348", "Q36834", "Q49757", "Q201788", "Q42973", "Q10871364", "Q2526255", "Q16533", "Q28389", "Q11513337", "Q639669", "Q11774891"};
//			for (String occ : occupation) {
//				listHumansPerOccupation(occ);
//				System.out.println(listOfHumansPerOccupation.size());
//			}
			
			WikidataCardinalityPattern cardinality = new WikidataCardinalityPattern();
//			cardinality.listHumansPerOccupation("Q82955");
//			cardinality.listDeadHumans();
			cardinality.mergeCardinalSplitFiles();
			cardinality.getCardinalityStatements();
//			cardinality.getRandomSamples(50);
			cardinality.matchNumberOfChildren();
			
//			//For top 20 occupations
//			String[] occupation = {"Q82955", "Q937857", "Q33999", "Q36180", "Q1028181", "Q1930187", "Q1622272", "Q177220", "Q40348", "Q36834", "Q49757", "Q201788", "Q42973", "Q10871364", "Q2526255", "Q16533", "Q28389", "Q11513337", "Q639669", "Q11774891"};
//			WikidataCardinalityPattern cardinality = new WikidataCardinalityPattern();
//			for (String occ : occupation) {
//				cardinality.listHumansPerOccupation(occ);
//				cardinality.mergeCardinalSplitFiles();
//				cardinality.getCardinalityStatements();
//				cardinality.matchNumberOfChildren();
//			}
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void getAllHumans() throws IOException {
		listOfHumans = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader("D:/Wikipedia/wikidata-instances.nt"));
		String line;
		while((line = br.readLine()) != null) {
			if (line.contains("<http://www.wikidata.org/entity/Q5>")) {
				Pattern p = Pattern.compile("<http://www.wikidata.org/entity/(.+?)>");
				Matcher m = p.matcher(line);
		        if (m.find()) {
		        	listOfHumans.add(m.group(1));
		        }
			}
		}
//		System.out.println(listOfHumans.size() + " humans");
	}
	
	public boolean isHuman(String wikidataID) {
		if (listOfHumans.contains(wikidataID)) return true;
		else return false;
	}
	
	public void numOfChildrenOfHumans() throws JSONException {
		numOfChildren = new HashMap<String, Integer>();
		
		String numOfChildrenQ = "SELECT ?human ?num WHERE {"
							+ "  {"
							+ "    ?human " + SPARQLQuery.PROP_INSTANCE_OF + " " + SPARQLQuery.OBJ_HUMAN + " ."
							+ "    ?human " + SPARQLQuery.PROP_NUM_OF_CHILDREN + " ?num ."
							+ "  }"
							+ " }"
							+ " ORDER BY ?num";
		
		JSONObject numOfChildrenRes = SPARQLQuery.execute(numOfChildrenQ);
		JSONArray numOfChildrenArr = numOfChildrenRes.getJSONObject ("results").getJSONArray("bindings");
		
		String uri, eid; int count;
		for (int i=0; i<numOfChildrenArr.length(); i++) {
			uri = numOfChildrenArr.getJSONObject(i).getJSONObject("human").getString("value");
			eid = uri.substring(uri.lastIndexOf("/")+1);
			count = Integer.parseInt(numOfChildrenArr.getJSONObject(i).getJSONObject("num").getString("value"));
			numOfChildren.put(eid, count);
		}
	}
	
	public void countChildrenOfHumans() throws JSONException {
		countOfChildren = new HashMap<String, Integer>();
		
		String countChildrenQ = "SELECT ?human (COUNT(?child) AS ?count) WHERE {"
							+ "  {"
							+ "    ?human " + SPARQLQuery.PROP_INSTANCE_OF + " " + SPARQLQuery.OBJ_HUMAN + " ."
							+ "    ?human " + SPARQLQuery.PROP_CHILD + " ?child ."
							+ "  }"
							+ " }" 
							+ " GROUP BY ?human"
							+ " ORDER BY ?count";
		
		JSONObject countChildrenRes = SPARQLQuery.execute(countChildrenQ);
		JSONArray countChildrenArr = countChildrenRes.getJSONObject ("results").getJSONArray("bindings");
		
		String uri, eid; int count;
		for (int i=0; i<countChildrenArr.length(); i++) {
			uri = countChildrenArr.getJSONObject(i).getJSONObject("human").getString("value");
			eid = uri.substring(uri.lastIndexOf("/")+1);
			count = Integer.parseInt(countChildrenArr.getJSONObject(i).getJSONObject("count").getString("value"));
			countOfChildren.put(eid, count);
		}
	}
	
	public void listHumansPerOccupation(String occupationID) throws JSONException {
		listOfHumansPerOccupation = new HashSet<String>();
		
		String humanOccQ = "SELECT DISTINCT ?human WHERE {"
							+ "  {"
							+ "    ?human " + SPARQLQuery.PROP_INSTANCE_OF + " " + SPARQLQuery.OBJ_HUMAN + " ."
							+ "    ?human " + SPARQLQuery.PROP_OCCUPATION + " wd:" + occupationID + " ."
							+ "  }"
							+ " }";
		
		JSONObject humanOccRes = SPARQLQuery.execute(humanOccQ);
		JSONArray humanOccArr = humanOccRes.getJSONObject ("results").getJSONArray("bindings");
		
		String uri, eid;
		for (int i=0; i<humanOccArr.length(); i++) {
			uri = humanOccArr.getJSONObject(i).getJSONObject("human").getString("value");
			eid = uri.substring(uri.lastIndexOf("/")+1);
			listOfHumansPerOccupation.add(eid);
		}
//		System.out.println(listOfHumansPerOccupation.size() + " humans with occupation " + occupationID);
	}
	
	public void listDeadHumans() throws JSONException, IOException {
		listOfDeadHumans = new HashSet<String>();		
		BufferedReader br = new BufferedReader(new FileReader("D:/Wikipedia/wikidata-simple-statements.nt"));
		String line;
		while((line = br.readLine()) != null) {
			if (line.contains("> <http://www.wikidata.org/entity/P570c> ")) {
				Pattern p = Pattern.compile("<http://www.wikidata.org/entity/(.+?)> <http://www.wikidata.org/entity/P570c> ");
				Matcher m = p.matcher(line);
		        if (m.find()) {
		        	listOfDeadHumans.add(m.group(1));
		        }
			}
		}
//		System.out.println(listOfDeadHumans.size() + " dead humans");
	}
	
	public void mergeCardinalSplitFiles() throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(cardinalStatementsPath), "UTF-16"));
		PrintWriter writer = new PrintWriter(bw);
		
		Integer numLine = 0;
		for (int i=0; i<14; i++) {
			String filename = "data/cardinal_statements_splits/cardinal_statements_" + i + "_wikidata.txt";
			BufferedReader br = new BufferedReader(new InputStreamReader (
					new FileInputStream(filename), "UTF-16"));
			String line; 
			while ((line = br.readLine()) != null) {
				writer.println(line.trim());
				numLine ++;
		    }
			br.close();
		}
		bw.close();
		
//		System.out.println(numLine);
	}
	
	private Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap) {

		// Convert Map to List
		List<Map.Entry<String, Integer>> list = 
			new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
                                           Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	private void getCardinalityStatements() throws IOException, JSONException {
		BufferedReader br = new BufferedReader(new InputStreamReader (
				new FileInputStream(cardinalStatementsPath), "UTF-16"));
		cardinalStatements = new HashMap<String, WikiArticle>();
		Map<String, Integer> lengthOfArticles = new HashMap<String, Integer>();
		
		String line; int human = 0, nonHuman = 0, nonWikidata = 0;
		while ((line = br.readLine()) != null) {
			WikiArticle wa = new WikiArticle(line);
			if (wa.getWikidataID().isEmpty()) {
				nonWikidata ++;
			} else {
				if (isHuman(wa.getWikidataID()) 
//						&& wa.getSentences().size() == 1
//						&& !wa.getSentences().get(0).contains("adopt")			//doesn't improve much
//						&& !wa.getSentences().get(0).contains("illegitimate")	//doesn't improve much
						) {
					cardinalStatements.put(wa.getTitle(), wa);
					lengthOfArticles.put(wa.getTitle(), wa.getLength());
					human ++;
				} else {
					nonHuman ++;
				}
			}
	    }
		br.close();
		
		lenArticles = sortByComparator(lengthOfArticles);
		
//		System.out.println("Human: " + human + " Non-human: " + nonHuman + " Non-wikidata: " + nonWikidata);
//		System.out.println("Human articles: " + cardinalStatements.size());
	}
	
	public void getRandomSamples(int num) {
		List<String> keys = new ArrayList<String>(lenArticles.keySet());
		Collections.shuffle(keys);
		int i=0;
		while (i < num) {
			WikiArticle wa = cardinalStatements.get(keys.get(i));
			System.out.println(wa.toString());
			i ++;
		}
	}

	public void matchNumberOfChildren() throws JSONException, IOException {
		//Get people with children cardinality statements
		Integer numUnknown = 0, numKnown = 0, numMatched = 0;
		int numC, numChild, countChild, numMiss;
		int match = 0, numZero = 0, countZero = 0, numLess = 0, countLess = 0;
		int[][] numbers = new int[7][7];
		int missingChild = 0, completeChild = 0;
		int wdMissingChild = 0, wdCompleteChild = 0;
		
		Integer threshold = lenArticles.size() - (int) Math.round(lenArticles.size() * 0.25);
//		System.out.println(threshold);
		
		Set<String> overlapped = new HashSet<String>();
		
		PrintWriter statements = new PrintWriter(new File("./data/cardinality-statements.txt"));
		PrintWriter complete = new PrintWriter(new File("./data/complete-child-entities.txt"));
		
		boolean wdExist = false;
		
		Integer numArticles=0;
		for (String key : lenArticles.keySet())	{
//			System.out.println(numArticles + "-" + lenArticles.get(key));
			WikiArticle wa = cardinalStatements.get(key);
			
			if (numArticles<threshold) {
//			if (wa.getLength() < 10000) {
			
				if (wa.getSentences().size() == 1) {
					
//					if (listOfHumansPerOccupation.contains(wa.getWikidataID())) {
//					if (!listOfDeadHumans.contains(wa.getWikidataID())) {
					
						numChild = wa.getNumChild();
						numMiss = wa.getNumChild();
						
						if (numOfChildren.containsKey(wa.getWikidataID())) {
							numKnown ++;
							numC = numOfChildren.get(wa.getWikidataID());
							if (numC == numChild) numMatched ++;
							else numMiss = numC;
							overlapped.add(wa.getWikidataID());
							wdExist = true;
						} else {
							numUnknown ++;
							wdExist = false;
						}
						
						if (countOfChildren.containsKey(wa.getWikidataID())) countChild = countOfChildren.get(wa.getWikidataID());
						else countChild = 0;
						
						if (numChild == countChild) {
							if (numChild < 6) {
								numbers[numChild][countChild] ++;
							} else {
								numbers[6][6] ++;
							}
							match ++;
							completeChild += countChild;
							if (!wdExist) complete.write(wa.getWikidataID() + "\n");
							
						} else if (numChild == 0 && countChild > 0) {
							if (countChild < 6) {
								numbers[numChild][countChild] ++;
							} else {
								numbers[numChild][6] ++;
							} 
							numZero ++;
						} else if (numChild > 0 && countChild == 0) {
							if (numChild < 6) {
								numbers[numChild][countChild] ++;
							} else {
								numbers[6][countChild] ++;
							} 
							countZero ++;
							missingChild += (numChild - countChild);
						} else if (numChild < countChild){
							if (numChild < 6 && countChild < 6) {
								numbers[numChild][countChild] ++;
//								if (numChild == 1 && countChild == 2)
//									System.err.println(numChild + "\t" + countChild + "\t" + wa.toString());
							} else if (numChild > 6 && countChild < 6) {
								numbers[6][countChild] ++;
							} else if (numChild < 6 && countChild > 6) {
								numbers[numChild][6] ++;
							}
							numLess ++;
						} else if (countChild < numChild) {
							if (numChild < 6 && countChild < 6) {
								numbers[numChild][countChild] ++;
							} else if (numChild > 6 && countChild < 6) {
								numbers[6][countChild] ++;
							} else if (numChild < 6 && countChild > 6) {
								numbers[numChild][6] ++;
							}
							countLess ++;
							missingChild += (numMiss - countChild);
						}		
						
						statements.write("<http://www.wikidata.org/entity/" + wa.getWikidataID() + ">" + " <http://www.wikidata.org/prop/direct/P1971> \"" 
								+ wa.getNumChild() + "\"^^<http://www.w3.org/2001/XMLSchema#decimal> .\n");
					
//					}
				}
					
			} else {
				break;
			}
			numArticles ++;
		}
		
		statements.close();
		complete.close();
		
		for (String id : numOfChildren.keySet()) {
			
//			if (listOfHumansPerOccupation.contains(id)) {
//			if (!listOfDeadHumans.contains(id)) {
				
				if (!overlapped.contains(id)) {
					if (countOfChildren.containsKey(id)) countChild = countOfChildren.get(id);
					else countChild = 0;
					if (countChild == numOfChildren.get(id)) {
						wdCompleteChild += countChild;
					} else if (countChild < numOfChildren.get(id)) {
						wdMissingChild += (numOfChildren.get(id) - countChild);
					}
				}
//			}
		}
//		System.out.println("Num known: " + numKnown + " Num matched: " + numMatched + " Num unknown: " + numUnknown);
			
//		for (int i=0; i<numbers.length; i++) {
//			for (int j=0; j<numbers[i].length; j++) {
//				System.out.print(numbers[i][j] + "\t");
//			}
//			System.out.println();
//		}
		
//		System.out.println("Number-cardinal and Count match: " + match
////				+ "\nNumber-cardinal is zero but Count is non-zero: " + numZero
////				+ "\nCount is zero but Number-cardinal is non-zero: " + countZero
////				+ "\nNumber-cardinal is less than Count: " + numLess
////				+ "\nCount is less than Number-cardinal: " + countLess
//				+ "\nNumber-cardinal is less than Count: =" + numZero + "+" + numLess
//				+ "\nCount is less than Number-cardinal: =" + countZero + "+" + countLess
//				+ "\nMissing children: " + missingChild
//				+ " Complete children: " + completeChild
//				+ "\nMissing children (wd): " + wdMissingChild
//				+ " Complete children (wd): " + wdCompleteChild);
		
		System.out.println(listOfHumans.size()
				+ "\t" + match
				+ "\t=" + numZero + "+" + numLess
				+ "\t=" + countZero + "+" + countLess
				+ "\t" + missingChild
				+ "\t" + completeChild
				+ "\t" + numMatched
				+ "\t" + numKnown
				+ "\t" + wdMissingChild
				+ "\t" + wdCompleteChild);
	}
}
