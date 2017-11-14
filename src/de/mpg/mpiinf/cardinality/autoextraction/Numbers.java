package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.simple.Sentence;

public class Numbers {
	
	private static String[] digitsArr = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
	private static String[] tensArr = {"", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
	private static String[] ordinalsArr = {"", "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", 
			"eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth", "twentieth"};
	private static String[] tenOrdinalsArr = {"", "tenth", "twentieth", "thirtieth", "fortieth", "fiftieth", "sixtieth", "seventieth", "eightieth", "ninetieth"};
	
	public static List<String> digits = Arrays.asList(digitsArr);
	public static List<String> tens = Arrays.asList(tensArr);
	public static List<String> ordinals = Arrays.asList(ordinalsArr);
	public static List<String> tenOrdinals = Arrays.asList(tenOrdinalsArr);
	
	public static Map<String, Integer> hundreds = new HashMap<String, Integer>();
	
	public static Long getInteger(String numStr) {
		hundreds.put("hundred", 100);
		hundreds.put("thousand", 1000);
		hundreds.put("million", 1000000);
		
		long number = -999; 
		if (numStr.contains(",")) numStr = numStr.replace(",", "");
		if (numStr.contains("-")) numStr = numStr.replace("-", "_");
		if (numStr.contains("and_")) numStr = numStr.replace("and_", "_");
		String[] words = numStr.split("_");
		
		if (words.length == 4) {
			if (digits.contains(words[0]) && hundreds.containsKey(words[1])
					&& tens.contains(words[2]) && digits.contains(words[3])) {
				number = (digits.indexOf(words[0]) * hundreds.get(words[1])) + (tens.indexOf(words[2]) * 10) + digits.indexOf(words[3]);
			} else if (digits.contains(words[0]) && hundreds.containsKey(words[1])
					&& tens.contains(words[2]) && ordinals.contains(words[3])) {
				number = (digits.indexOf(words[0]) * hundreds.get(words[1])) + (tens.indexOf(words[2]) * 10) + ordinals.indexOf(words[3]);
			}
		} else if (words.length == 3) {
			if (hundreds.containsKey(words[0])
					&& tens.contains(words[1]) && digits.contains(words[2])) {
				number = (1 * hundreds.get(words[0])) + (tens.indexOf(words[1]) * 10) + digits.indexOf(words[2]);
			} else if (hundreds.containsKey(words[0])
					&& tens.contains(words[1]) && ordinals.contains(words[2])) {
				number = (1 * hundreds.get(words[0])) + (tens.indexOf(words[1]) * 10) + ordinals.indexOf(words[2]);
			}
		} else if (words.length == 2) {
			if (tens.contains(words[0]) && digits.contains(words[1])) {
				number = (tens.indexOf(words[0]) * 10) + digits.indexOf(words[1]);
			} else if (tens.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = (tens.indexOf(words[0]) * 10) * hundreds.get(words[1]);
			} else if (digits.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = digits.indexOf(words[0]) * hundreds.get(words[1]);
			} else if (words[0].matches("^-?\\d+\\.?\\d*$") && hundreds.containsKey(words[1])) {
				number = new Float(Float.parseFloat(words[0]) * hundreds.get(words[1])).longValue();
			} else if (tens.contains(words[0]) && ordinals.contains(words[1])) {
				number = (tens.indexOf(words[0]) * 10) + ordinals.indexOf(words[1]);
			}
		} else {
			if (tens.contains(numStr)) number = tens.indexOf(numStr) * 10;
			else if (digits.contains(numStr)) number = digits.indexOf(numStr);
			else if (hundreds.containsKey(numStr)) number = hundreds.get(numStr);
			else if (numStr.matches("^-?\\d+$")) number = new Long(Long.parseLong(numStr));
			else if (ordinals.contains(numStr)) number = ordinals.indexOf(numStr);
			else if (tenOrdinals.contains(numStr)) number = tenOrdinals.indexOf(numStr) * 10;
			else if (numStr.matches("^-?\\d+st$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
			else if (numStr.matches("^-?\\d+nd$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
			else if (numStr.matches("^-?\\d+rd$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
			else if (numStr.matches("^-?\\d+th$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
		}
		
		return number;
	}
	
	public static int getLGInteger(String latinGreekPrefix) throws IOException {
		Transform transform = new Transform();
		return transform.prefixLatinGreek.get(latinGreekPrefix);
	}
	
	public static boolean properUnLessAdj(String lemma, String pos) {
		if (pos.equals("JJ")
				&& (lemma.startsWith("un") || lemma.endsWith("less"))) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properNumber(String pos, String ner) {
		if (pos.equals("CD")
				&& ner.equals("NUMBER")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properConjNumber(String pos, String ner) {
		if (pos.equals("CC")
				&& ner.equals("NUMBER")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properOrdinal(String pos, String ner) {
		if ((pos.equals("JJ"))
				&& ner.equals("ORDINAL")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properNumOrdinal(String pos, String ner) {
		if (pos.equals("CD")
				&& ner.equals("ORDINAL")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properConjOrdinal(String pos, String ner) {
		if (pos.equals("CC")
				&& ner.equals("ORDINAL")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properNumberAndOrdinal(String pos, String ner) {
		if (pos.equals("CD")
				&& !ner.equals("MONEY")
				&& !ner.equals("PERCENT")
				&& !ner.equals("DATE")
				&& !ner.equals("TIME")
				&& !ner.equals("DURATION")
				&& !ner.equals("SET")
				) {
			return true;
		} else if (pos.equals("JJ")
				&& ner.equals("ORDINAL")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properName(String pos, String ner) {
		if (pos.equals("NNP")
				&& ner.equals("PERSON")
				) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properNoun(String pos) {
		if (pos.equals("NNP")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean personalPronoun(String pos) {
		if (pos.equals("PRP")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean possessivePronoun(String pos) {
		if (pos.equals("PRP$")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean properArticle(String word, String pos, String deprel) {
		if ((word.equals("a") || word.equals("an"))
				&& pos.equals("DT")
				&& deprel.equals("det")) {
			return true;
		} else {
			return false;
		}		
	}
	
	public static boolean properNo(String word, String pos) {
		if (word.equals("no")
				&& pos.equals("DT")) {
			return true;
		} else {
			return false;
		}		
	}
	
	public static boolean properNegation(String word, String pos) {
		if ((word.equals("no") && pos.equals("DT"))
				|| word.equals("any")
				|| word.equals("without")
				|| word.equals("never")
				) {
			return true;
		} else {
			return false;
		}		
	}
	
	public static boolean properCountableQuantifier(String word, String pos, String deprel) {
		if ((word.equals("both") && pos.equals("DT") && deprel.equals("det"))
				|| (word.equals("some") && pos.equals("DT") && deprel.equals("det"))
				|| (word.equals("few") && pos.equals("JJ") && deprel.equals("amod"))
				|| (word.equals("many") && pos.equals("JJ") && deprel.equals("amod"))
				|| (word.equals("several") && pos.equals("JJ") && deprel.equals("amod"))
				) {
			return true;
		} else {
			return false;
		}		
	}
	
	public static boolean containsLatinGreek(String sentence) {
		if (sentence.contains("LatinGreek_")) return true;
		else return false;
	}
	
	public static boolean containsNumerals(Sentence sent, boolean ordinal) {
		String pos, ner;
		
		for (int i=0; i<sent.words().size(); i++) {
//			System.err.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
			pos = sent.posTag(i);
			ner = sent.nerTag(i);
			
			if (properNumber(pos, ner)) {
				return true;				
			} else if (ordinal && properOrdinal(pos, ner)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean containsArticles(Sentence sent) {
		String word, pos, deprel;
		
		for (int i=0; i<sent.words().size(); i++) {
//			System.err.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
			word = sent.word(i);
			pos = sent.posTag(i);
			deprel = "O"; 
			if (sent.incomingDependencyLabel(i).isPresent()) {
				deprel = sent.incomingDependencyLabel(i).get();
			}
			
			if (properArticle(word, pos, deprel)) {
				return true;
				
			}
		}
		return false;
	}
	
	public static boolean containsNo(Sentence sent) {
		String word, pos, deprel;
		
		for (int i=0; i<sent.words().size(); i++) {
//			System.err.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
			word = sent.word(i);
			pos = sent.posTag(i);
			
			if (properNo(word, pos)) {
				return true;
				
			}
		}
		return false;
	}
	
	public static boolean containsNegation(Sentence sent) {
		String word, pos, deprel;
		
		for (int i=0; i<sent.words().size(); i++) {
//			System.err.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
			word = sent.word(i);
			pos = sent.posTag(i);
			
			if (properNegation(word, pos)) {
				return true;
				
			}
		}
		return false;
	}
	
	public static boolean containsNegatives(String sentence) {
		if (sentence.contains(" no ")
				|| ((sentence.contains(" not ") || sentence.contains(" n't ")) 
						&& sentence.contains(" any "))
				|| sentence.contains(" never ")
				)
			return true;
		else 
			return false;
	}
	
	public static boolean containsCountableQuantifiers(String sentence) {
		if (sentence.contains(" both ")
				|| sentence.contains(" few ")
				|| sentence.contains(" a few ")
				|| sentence.contains(" lots of ")
				|| sentence.contains(" a lot of ")
				|| sentence.contains(" plenty of ")
				|| sentence.contains(" many ")
				|| sentence.contains(" several ")
				|| sentence.contains(" some ")
				|| sentence.contains(" a number of ")
				)
			return true;
		else
			return false;
	}
	
	public static boolean containsPersonalPronouns(Sentence sent) {
		String pos;
		
		for (int i=0; i<sent.words().size(); i++) {
//			System.err.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
			pos = sent.posTag(i);
			if (pos.equals("PRP$")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean containNumbers(String transformed, Sentence sent, 
			boolean ordinal, boolean namedEntity) {
		
		String pos, ner;
		
		if (transformed.contains("LatinGreek_")) {
			return true;
			
		} else {
			boolean entityFound = false, numberFound = false, ordinalFound = false;
			for (int i=0; i<sent.words().size(); i++) {
//				System.err.println(sent.word(i) + "\t" + sent.posTag(i) + "\t" + sent.nerTag(i));
				pos = sent.posTag(i);
				ner = sent.nerTag(i);
				if (sent.posTag(i).equals("CD")
						&& !sent.word(i).contains("=")
						&& !sent.nerTag(i).equals("MONEY")
						&& !sent.nerTag(i).equals("PERCENT")
						&& !sent.nerTag(i).equals("DATE")
						&& !sent.nerTag(i).equals("TIME")
						&& !sent.nerTag(i).equals("DURATION")
						&& !sent.nerTag(i).equals("SET")) {
					numberFound = true;
					break;
				} else if (ordinal && sent.posTag(i).equals("JJ")
						&& sent.nerTag(i).equals("ORDINAL")) {
					ordinalFound = true;
					break;
				} else if (namedEntity && sent.posTag(i).equals("NNP")
						&& (sent.nerTag(i).equals("PERSON")
								|| sent.nerTag(i).equals("LOCATION")
								|| sent.nerTag(i).equals("ORGANIZATION"))) {
					entityFound = true;
					break;
				}
			}
			if (ordinal && (numberFound || ordinalFound)) {
				return true;
			} else if (namedEntity && (numberFound || entityFound)) {
				return true;
			} else if (numberFound) {
				return true;
			}
		}
		return false;
	}

}
