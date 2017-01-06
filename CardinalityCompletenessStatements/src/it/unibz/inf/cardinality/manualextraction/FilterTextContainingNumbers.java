package it.unibz.inf.cardinality.manualextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterTextContainingNumbers {
	
	private String inputFile;
	
	public FilterTextContainingNumbers() {
		
	}
	
	public FilterTextContainingNumbers(String filename) {
		setInputFile(filename);
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
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
	
	private void filterText() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader (
		new FileInputStream(inputFile), "UTF-16"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
		new FileOutputStream(inputFile.replace(".txt", "_only_contain_numbers.txt")), "UTF-16"));
		PrintWriter writer = new PrintWriter(bw);
		
		String line;
		int i=0;
		while ((line = br.readLine()) != null) {
			if (i%100 == 0) System.out.println(i);
			if (containNumber(line)) {
				writer.println(line);
			}
	    }
		br.close();
		bw.close();
	}
	
	public static void main(String[] args) {
		FilterTextContainingNumbers text = new FilterTextContainingNumbers("D://Wikipedia//wiki_articles_in_plain_text.txt");
		try {
			text.filterText();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
