package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.simple.Sentence;

public class ConceptNetNumbers {
	
	private String[] nums = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", 
			"twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety", 
			"hundred", "thousand", "million"};
	
	private static String[] ords = {"", "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", 
			"eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth", 
			"twentieth", "thirtieth", "fortieth", "fiftieth", "sixtieth", "seventieth", "eightieth", "ninetieth",
			"hundredth", "thousandth", "millionth"};
	
	private List<String> numbers;
	private List<String> ordinals;
	private Map<String, Integer> prefixLatinGreek;
	private Map<String, List<String>> prefixLatinGreekInv;
	private Set<String> postPrefixLatinGreek;
	private Map<String, String> numberLatinGreek;
	private Set<String> postNumber;
	private Map<String, Integer> otherConceptsCount;
	private Map<String, String> otherConcepts;
//	private Map<String, String> otherConceptNouns;
	
	public Comparator<String> x = new Comparator<String>()
    {
        @Override
        public int compare(String o1, String o2) {
        	return -Integer.compare(o1.length(), o2.length());
        }
    };
	
	public ConceptNetNumbers() {
		numbers = Arrays.asList(nums);
		ordinals = Arrays.asList(ords);
		
		//Latin/Greek numeral prefix -- integer refers to index in 'numbers'
		setPrefixLatinGreek();
		setPrefixLatinGreekInverse();
		numberLatinGreek = new HashMap<String, String>();
		postPrefixLatinGreek = new HashSet<String>();
		postNumber = new HashSet<String>();
		otherConceptsCount = new HashMap<String, Integer>();
		otherConcepts = new HashMap<String, String>();
//		otherConceptNouns = new HashMap<String, String>();
	}
	
	public static void main(String[] args) throws IOException {
		
		ConceptNetNumbers conceptNum = new ConceptNetNumbers();		
		conceptNum.filterConcepts();
		
//		for (String key : conceptNum.prefixLatinGreek.keySet()) {
//			System.out.println(key + "\t" + conceptNum.numbers[conceptNum.prefixLatinGreek.get(key)]);
//		}
		
	}
	
	public void filterConcepts() throws IOException {
		String line, rel, c1, c2;
		Set<String> conceptNetNumbers = new HashSet<String>();
		Map<String, Integer> conceptNetPostPrefix = new HashMap<String, Integer>();
		String prefixFound = "", postPrefix = "";
		
		for (int i=1; i<numbers.size(); i++) {
			String inputFile = "./data/conceptnet_number_related/" + numbers.get(i) + ".txt";
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			
			line = br.readLine();
			while (line != null) {
				String[] cols = line.split("\t");
				if (cols[4].contains("/s/resource/wiktionary/en")
						&& (cols[1].contains("RelatedTo") || cols[1].contains("Synonym"))
						&& cols[2].contains("/c/en/") && cols[3].contains("/c/en/")) {
					
					rel = cols[1].replace("/r/", "");
					c1 = cols[2].split("/")[3];
					c2 = cols[3].split("/")[3];
					
//					if (c1.contains("_")) c1 = c1.substring(0, c1.indexOf("_"));
//					if (c2.contains("_")) c2 = c2.substring(0, c2.indexOf("_"));
					
					prefixFound = ""; postPrefix = "";
					if (numbers.contains(c1) && !numbers.contains(c2) && !ordinals.contains(c2)) {
						if (prefixLatinGreekInv.containsKey(c1)) {
							for (String pref : prefixLatinGreekInv.get(c1)) {
								if (c2.startsWith(pref)) {
									prefixFound = pref; 
									postPrefix = c2.replace(prefixFound, "");
									break;
								}
							}
						}
						if (!prefixFound.equals("") && !c2.equals(prefixFound)
								&& !postPrefix.contains("_")
								) {
							if (!conceptNetPostPrefix.containsKey(postPrefix))
								conceptNetPostPrefix.put(postPrefix, 0);
							conceptNetPostPrefix.put(postPrefix, conceptNetPostPrefix.get(postPrefix)+1);
						} else if (c2.equals(prefixFound)) {
							numberLatinGreek.put(c2, c1);
						} else if (c2.startsWith(c1)){
							postNumber.add(c2.replace(c1, ""));
						} else {
							conceptNetNumbers.add(rel + "\t" + c1 + "\t" + c2);
						}
					} else if (numbers.contains(c2) && !numbers.contains(c1) && !ordinals.contains(c1)) {
						if (prefixLatinGreekInv.containsKey(c2)) {
							for (String pref : prefixLatinGreekInv.get(c2)) {
								if (c1.startsWith(pref)) {
									prefixFound = pref; 
									postPrefix = c1.replace(prefixFound, "");
									break;
								}
							}
						}
						if (!prefixFound.equals("") && !c1.equals(prefixFound)
								&& !postPrefix.contains("_")
								) {
							if (!conceptNetPostPrefix.containsKey(postPrefix))
								conceptNetPostPrefix.put(postPrefix, 0);
							conceptNetPostPrefix.put(postPrefix, conceptNetPostPrefix.get(postPrefix)+1);
						} else if (c1.equals(prefixFound)) {
							numberLatinGreek.put(c1, c2);
						} else if (c1.startsWith(c2)){
							String postStr = c1.replace(c2, "");
							if (!postStr.startsWith("_")
									&& !numbers.contains(postStr) 
									&& !numbers.contains(postStr.substring(1))
									&& !ordinals.contains(postStr) 
									&& !ordinals.contains(postStr.substring(1)) 
									&& !ordinals.contains(postStr.substring(0, postStr.length()-1)) 
									&& !ordinals.contains(postStr.substring(1, postStr.length()-1))
									&& !postStr.matches("^-?\\d+\\.?\\d*$")
									&& !postStr.matches("^-?\\d+\\.?\\d*\\w{0,2}$")
									&& !numberLatinGreek.containsKey(postStr)
									) {
								postNumber.add(postStr);
							}
						} else {
							conceptNetNumbers.add(rel + "\t" + c2 + "\t" + c1);
						}
					} 
//					System.out.println(rel + "\t" + c1 + "\t" + c2);
				}
				line = br.readLine();
			}
			
			br.close();
		}
		
		postPrefixLatinGreek.addAll(conceptNetPostPrefix.keySet());
		postPrefixLatinGreek.add("llion");
		postPrefixLatinGreek.add("illion");
		
		List<String> prefixList = new ArrayList<String>();
		prefixList.addAll(prefixLatinGreek.keySet());
		Collections.sort(prefixList, x);
					
		String[] cols;
		String postPref = "";
		boolean latinGreek = false;
		for (String concept : conceptNetNumbers) {
//			System.out.println(concept);
			cols = concept.split("\t");
			
			latinGreek = false;
			for (String key : prefixList) {
				if (cols[1].startsWith(key)) {
					postPref = cols[1].replace(key, "");
					if (postPrefixLatinGreek.contains(postPref)) {
						latinGreek = true; 
						break;
					}
				}
			}
			
			if (!numbers.contains(cols[2]) 
					&& !ordinals.contains(cols[2]) 
					&& !cols[2].matches("^-?\\d+\\.?\\d*$")
					&& !cols[2].matches("^-?\\d+\\.?\\d*\\w{0,2}$")
					&& !numberLatinGreek.containsKey(cols[2])
					&& !latinGreek) {
				if (!otherConceptsCount.containsKey(cols[2]))
					otherConceptsCount.put(cols[2], 0);
				otherConceptsCount.put(cols[2], otherConceptsCount.get(cols[2])+1);
				otherConcepts.put(cols[2], cols[1]);
			}
		}
		
//		for (String key : otherConceptsCount.keySet()) {
//			if (otherConceptsCount.get(key) > 1) 
//				otherConcepts.remove(key);
//			else {
//				String noun = wiktionaryContainNumber(key, otherConcepts.get(key));
//				if (noun == null) otherConcepts.remove(key);
//				else otherConceptNouns.put(key, noun);
//			}	
//		}
//		
//		for (String key : otherConcepts.keySet()) {
//			System.out.println(key + " = " + otherConcepts.get(key) + " -- " + otherConceptNouns.get(key));
//		}
		
		List<String> postPrefixLatinGreekList = new ArrayList<String>();
		postPrefixLatinGreekList.addAll(postPrefixLatinGreek);
		Collections.sort(postPrefixLatinGreekList, x);
		BufferedWriter bw = new BufferedWriter(new FileWriter("./data/post_prefix_latin_greek.tsv"));
		for(String p : postPrefixLatinGreekList) {
			bw.write(p + "\n");
		}
		bw.close();
		
		List<String> prefixLatinGreekList = new ArrayList<String>();
		prefixLatinGreekList.addAll(prefixLatinGreek.keySet());
		Collections.sort(prefixLatinGreekList, x);
		bw = new BufferedWriter(new FileWriter("./data/prefix_latin_greek.tsv"));
		for(String p : prefixLatinGreekList) {
			bw.write(p + "\t" + Numbers.getInteger(numbers.get(prefixLatinGreek.get(p))) + "\n");
		}
		bw.close();
	}
	
	public String wiktionaryContainNumber(String concept, String number) throws IOException {
		URL wiki = new URL("http://www.igrec.ca/project-files/wikparser/wikparser.php?word="+concept+"&query=def");
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(wiki.openStream()));
		
		String desc = "";
		String inputLine;
        while ((inputLine = in.readLine()) != null)
            desc += inputLine;
        in.close();
        
        desc = desc.trim();
        
        String sentStr, deprel = "", noun = "", deprel2 = "";
        for (String sent : desc.split("\\|")) {
        	sentStr = sent.trim();
        	
        	if (!sentStr.equals("")) {
	        	sentStr = Character.toLowerCase(sentStr.charAt(0)) + sentStr.substring(1);
	        	
	        	if ((" " + sentStr + " ").contains(" " + number + " ")) {
	        		Sentence s = new Sentence(sentStr);
	        		for (int k=0; k<s.words().size(); k++) {
	        			if (s.word(k).equals(number)) {
	        				if (s.incomingDependencyLabel(k).isPresent()) deprel = s.incomingDependencyLabel(k).get();
							else deprel = "O";
							if (s.governor(k).isPresent() && !deprel.equals("root")) {
								noun = s.lemma(s.governor(k).get());
							
								if (s.incomingDependencyLabel(s.governor(k).get()).isPresent()) 
									deprel2 = s.incomingDependencyLabel(s.governor(k).get()).get();
								else deprel2 = "_O";
							}
							
							break;
	        			}
	        		}
	        		if ((deprel.equals("nummod") || deprel.equals("root"))
//	        				&& (deprel2.equals("nmod:of") 
//	        						|| deprel2.equals("nmod:to")
//	        						|| deprel2.equals("nmod:into")
//	        						|| deprel2.equals("nmod:in")
//	        						|| deprel2.equals("root")
//	        						)
	        				&& (!deprel2.equals("nmod:from")
	        						&& !deprel2.equals("nmod:with")
	        						&& !deprel2.equals("nmod:by")
	        						&& !deprel2.equals("nmod:poss")
	        						&& !deprel2.contains("subj")
	        						&& !deprel2.contains("obj"))
	        				) {
//		        		System.out.println(concept + "-" + number + "-" + deprel + "-" + noun + "-" + deprel2);
		        		return noun;
	        		}
	        	}
        	}
        }
        
		return null;
	}
	
	public void setPrefixLatinGreekInverse() {
		prefixLatinGreekInv = new HashMap<String, List<String>>();
		
		String num = "";
		for (String key : prefixLatinGreek.keySet()) {
			num = numbers.get(prefixLatinGreek.get(key));
			if (!prefixLatinGreekInv.containsKey(num)) {
				prefixLatinGreekInv.put(num, new ArrayList<String>());
			} 
			prefixLatinGreekInv.get(num).add(key);
		}

	    for (String key : prefixLatinGreekInv.keySet()) {
	    	Collections.sort(prefixLatinGreekInv.get(key), x);
		}
	}
	
	public void setPrefixLatinGreek() {
		prefixLatinGreek = new HashMap<String, Integer>();
		
		//one
		prefixLatinGreek.put("uni", 1); prefixLatinGreek.put("mono", 1);
		prefixLatinGreek.put("mona", 1);
		
		//two
		prefixLatinGreek.put("bi", 2); 
//		prefixLatinGreek.put("du", 2);
//		prefixLatinGreek.put("di", 2); 
		prefixLatinGreek.put("dya", 2);
		prefixLatinGreek.put("bin", 2); prefixLatinGreek.put("bien", 2);
//		prefixLatinGreek.put("dia", 2);
		
		//three
		prefixLatinGreek.put("tri", 3);	prefixLatinGreek.put("ter", 3);
		prefixLatinGreek.put("trin", 3); prefixLatinGreek.put("tern", 3);
		prefixLatinGreek.put("trien", 3); prefixLatinGreek.put("terti", 3);
		prefixLatinGreek.put("tria", 3);
		
		//four
		prefixLatinGreek.put("quadr", 4); prefixLatinGreek.put("quart", 4);
		prefixLatinGreek.put("quatern", 4); prefixLatinGreek.put("tetra", 4);
		prefixLatinGreek.put("quadrien", 4);
		
		//five
		prefixLatinGreek.put("quint", 5); prefixLatinGreek.put("quinque", 5);
		prefixLatinGreek.put("penta", 5); prefixLatinGreek.put("quin", 5); 
		prefixLatinGreek.put("quinquen", 5);
		
		//six
		prefixLatinGreek.put("sex", 6); prefixLatinGreek.put("sext", 6);
		prefixLatinGreek.put("sen", 6); prefixLatinGreek.put("hexa", 6);
		prefixLatinGreek.put("sexen", 6); prefixLatinGreek.put("hexen", 6);
		
		//seven
		prefixLatinGreek.put("sept", 7); prefixLatinGreek.put("hepta", 7);
		prefixLatinGreek.put("septen", 7); 
		
		//eight
		prefixLatinGreek.put("oct", 8); prefixLatinGreek.put("octo", 8);
		prefixLatinGreek.put("octon", 8); prefixLatinGreek.put("octen", 8);
		prefixLatinGreek.put("octav", 8); prefixLatinGreek.put("octa", 8);
		
		//nine
		prefixLatinGreek.put("non", 9); prefixLatinGreek.put("nov", 9);
		prefixLatinGreek.put("ennea", 9); prefixLatinGreek.put("noven", 9);
		
		//ten
		prefixLatinGreek.put("dec", 10); prefixLatinGreek.put("den", 10);
		prefixLatinGreek.put("decen", 10); prefixLatinGreek.put("deca", 10);
		
		//eleven
		prefixLatinGreek.put("undec", 11); prefixLatinGreek.put("unden", 11);
		prefixLatinGreek.put("hendeca", 11); prefixLatinGreek.put("undecen", 11);
		prefixLatinGreek.put("undeca", 11);
		
		//twelve
		prefixLatinGreek.put("duodec", 12); prefixLatinGreek.put("duode", 12);
		prefixLatinGreek.put("dodeca", 12); prefixLatinGreek.put("duodecen", 12);
		
		//thirteen
		prefixLatinGreek.put("tridec", 13); prefixLatinGreek.put("tride", 13);
		prefixLatinGreek.put("triskaideca", 13); prefixLatinGreek.put("trisdeca", 13); 
		prefixLatinGreek.put("trideca", 13); prefixLatinGreek.put("triskaideka", 13);
		
		//fourteen
		prefixLatinGreek.put("quatuordec", 14); prefixLatinGreek.put("tetrakaideca", 14);
		prefixLatinGreek.put("tetradeca", 14); 
		
		//fifteen
		prefixLatinGreek.put("quindec", 15); prefixLatinGreek.put("quinde", 15);
		prefixLatinGreek.put("pendeca", 15); prefixLatinGreek.put("pentakaideca", 15);
		prefixLatinGreek.put("pentadeca", 15);
		
		//sixteen
		prefixLatinGreek.put("sedec", 16); prefixLatinGreek.put("sede", 16);
		prefixLatinGreek.put("hexadeca", 16); prefixLatinGreek.put("hexakaideca", 16);
		
		//seventeen
		prefixLatinGreek.put("septendec", 17); prefixLatinGreek.put("septende", 17);
		prefixLatinGreek.put("heptadeca", 17); prefixLatinGreek.put("heptakaideca", 17); 
		
		//eighteen
		prefixLatinGreek.put("decennoct", 18); prefixLatinGreek.put("octodeca", 18);
		prefixLatinGreek.put("octakaideca", 18); 
		
		//nineteen
		prefixLatinGreek.put("decennonv", 19); prefixLatinGreek.put("decenno", 19);
		prefixLatinGreek.put("enneadeca", 19); prefixLatinGreek.put("enneakaideca", 19);
		
		//twenty
		prefixLatinGreek.put("vige", 20); prefixLatinGreek.put("vice", 20);
		prefixLatinGreek.put("icosa", 20);
		
		//thirty
		prefixLatinGreek.put("trige", 21); prefixLatinGreek.put("trice", 21);
		prefixLatinGreek.put("triconta", 21); prefixLatinGreek.put("triaconta", 21);
		
		//forty
		prefixLatinGreek.put("quadrage", 22); prefixLatinGreek.put("tetraconta", 22);
		
		//fifty
		prefixLatinGreek.put("quinquage", 23); prefixLatinGreek.put("pentaconta", 23);
		
		//sixty
		prefixLatinGreek.put("sexage", 24); prefixLatinGreek.put("hexeconta", 24);
		prefixLatinGreek.put("hexaconta", 24); prefixLatinGreek.put("hexage", 24); 
		
		//seventy
		prefixLatinGreek.put("septuage", 25); prefixLatinGreek.put("heptaconta", 25);
		
		//eighty
		prefixLatinGreek.put("octage", 26); prefixLatinGreek.put("octoge", 26);
		prefixLatinGreek.put("octaconta", 26);
		
		//ninetey
		prefixLatinGreek.put("nonage", 27); prefixLatinGreek.put("enneaconta", 27);
		
		//hundred
		prefixLatinGreek.put("cente", 28); prefixLatinGreek.put("cent", 28); 
		prefixLatinGreek.put("hecato", 28); prefixLatinGreek.put("hecto", 28);
		
		//thousand
		prefixLatinGreek.put("mille", 29); prefixLatinGreek.put("chilia", 29);
		
//		prefixLatinGreek.put("kilo", 29);
//		prefixLatinGreek.put("mega", 30);
	}

}
