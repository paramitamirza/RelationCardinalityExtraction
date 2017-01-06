package it.unibz.inf.cardinality.statistics;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Stack;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class DBPediaNumberOfChildren {
	
	public DBPediaNumberOfChildren() {
		
	}

	public static void main(String[] args) {
		try {
			DBPediaNumberOfChildren numOfChildren = new DBPediaNumberOfChildren();
			numOfChildren.matchNumberOfChildren();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void matchNumberOfChildren() throws JSONException {
		//Get people with number of chidren property
		String q = "SELECT ?parent ?numOfChildren ?countChildren WHERE {  {    ?parent " 
		+ SPARQLQuery.PROP_NUM_OF_CHILDREN 
		+ " ?numOfChildren .    {       SELECT ?parent (COUNT(?child) AS ?countChildren) WHERE {        ?parent " 
		+ SPARQLQuery.PROP_CHILD 
		+ " ?child .      } GROUP BY ?parent    }  }  UNION  {     { ?parent " 
		+ SPARQLQuery.PROP_NUM_OF_CHILDREN 
		+ " ?numOfChildren . }     MINUS     {      SELECT ?parent WHERE {        ?parent " 
		+ SPARQLQuery.PROP_CHILD + " ?child .      } GROUP BY ?parent    }   } }";
		
		JSONObject initialRes = SPARQLQuery.execute(q);
		JSONArray res = initialRes.getJSONObject ("results").getJSONArray("bindings");
		
		String entity = "", eid = "", qq;
		int numChild, countChild;
		int match = 0, numZero = 0, countZero = 0, numLess = 0, countLess = 0;
		int[][] numbers = new int[7][7];
		int missingChild = 0, listedChild = 0;
		for (int i=0; i<res.length(); i++) {
			entity = res.getJSONObject(i).getJSONObject("parent").getString("value");
			numChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("numOfChildren").getString("value"));
			if (res.getJSONObject(i).isNull("countChildren")) {
				countChild = 0;
			} else {
				countChild = Integer.parseInt(res.getJSONObject(i).getJSONObject("countChildren").getString("value"));
			}
			//System.out.println(entity + "\t" + numChild + "\t" + countChild);
			
			if (numChild == countChild) {
				if (numChild < 6) {
					numbers[numChild][countChild] ++;
				} else {
					numbers[6][6] ++;
				}
				match ++;
				listedChild += countChild;
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
				missingChild += (numChild - countChild);
			}
		}
		
		for (int i=0; i<numbers.length; i++) {
			for (int j=0; j<numbers[i].length; j++) {
				System.out.print(numbers[i][j] + "\t");
			}
			System.out.println();
		}
		
		System.out.println("Number-prop and Count match: " + match
				+ "\nNumber-prop is zero but Count is non-zero: " + numZero
				+ "\nCount is zero but Number-prop is non-zero: " + countZero
				+ "\nNumber-prop is less than Count: " + numLess
				+ "\nCount is less than Number-prop: " + countLess
				+ "\nMissing children: " + missingChild
				+ "\nListed children: " + listedChild);
	}
}
