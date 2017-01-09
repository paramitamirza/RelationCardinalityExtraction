package it.unibz.inf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;

public class CardinalityEvaluation {
	
	public JSONArray readJSONArray(String filepath) throws IOException, JSONException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String everything;
		try {
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    everything = sb.toString();
		} finally {
		    br.close();
		}
		return new JSONArray(everything);
	}

	public static void main(String[] args) throws JSONException, IOException {
		
		String resultPath = "./data/out-cardinality.txt";
		CardinalityEvaluation eval = new CardinalityEvaluation();
		
	}

}
