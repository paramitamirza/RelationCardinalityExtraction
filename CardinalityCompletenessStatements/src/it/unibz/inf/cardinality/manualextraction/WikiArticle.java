package it.unibz.inf.cardinality.manualextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.simple.*;

public class WikiArticle {
	
	private String title;
	private String wikidataID;
	private Integer numChild;
	private List<String> sentences;
	private Integer length;
	
	public WikiArticle() {
		setNumChild(0);
		setSentences(new ArrayList<String>());
	}
	
	public WikiArticle(String wiki) {
		this();
		String[] cols = wiki.split("\t");
		setTitle(cols[0]);
		setWikidataID(cols[1]);
		setLength(Integer.parseInt(cols[2]));
		setNumChild(Integer.parseInt(cols[3]));
		for (String s : cols[4].split("#######")) {
			if (!s.isEmpty()) {
				getSentences().add(s);
			}
		}
	}
	
	public String toString() {
		String str = title + "\t" + wikidataID + "\t" + length + "\t" + numChild + "\t";
		for (String sent : sentences) {
			str += sent + "#######";
		}
		return str;
	}
	
	public static void main(String[] args) throws IOException {
	
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getSentences() {
		return sentences;
	}

	public void setSentences(List<String> sentences) {
		this.sentences = sentences;
	}

	public String getWikidataID() {
		return wikidataID;
	}

	public void setWikidataID(String wikidataID) {
		this.wikidataID = wikidataID;
	}

	public Integer getNumChild() {
		return numChild;
	}

	public void setNumChild(Integer numChild) {
		this.numChild = numChild;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

}
