package de.mpg.mpiinf.cardinality.autoextraction;

import java.util.Map;

public class ConceptNetNumbers {
	
	private String[] numbers = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", 
			"twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety", 
			"hundred", "thousand", "million"};
	
	private Map<String, Integer> prefixLatinGreek;	
	private Map<String, Integer> otherConcepts;
	
	public ConceptNetNumbers() {
		
		//Latin/Greek numeral prefix -- integer refers to index in 'numbers'
		
		//one
		prefixLatinGreek.put("uni", 1); prefixLatinGreek.put("mono", 1);
		
		//two
		prefixLatinGreek.put("bi", 2); prefixLatinGreek.put("du", 2);
		prefixLatinGreek.put("di", 2); prefixLatinGreek.put("dy", 2);
		
		//three
		prefixLatinGreek.put("tri", 3);	prefixLatinGreek.put("ter", 3);
		
		//four
		prefixLatinGreek.put("quadr", 4); prefixLatinGreek.put("quart", 4);
		prefixLatinGreek.put("quater", 4); prefixLatinGreek.put("tetra", 4);
		
		//five
		prefixLatinGreek.put("quint", 5); prefixLatinGreek.put("quinque", 5);
		prefixLatinGreek.put("penta", 5);
		
		//six
		prefixLatinGreek.put("sex", 6); prefixLatinGreek.put("sext", 6);
		prefixLatinGreek.put("se", 6); prefixLatinGreek.put("hexa", 6);
		
		//seven
		prefixLatinGreek.put("sept", 7); prefixLatinGreek.put("hepta", 7);
		
		//eight
		prefixLatinGreek.put("oct", 8); prefixLatinGreek.put("octa", 8);
		prefixLatinGreek.put("octo", 8);
		
		//nine
		prefixLatinGreek.put("non", 9); prefixLatinGreek.put("nov", 9);
		prefixLatinGreek.put("ennea", 9);
		
		//ten
		prefixLatinGreek.put("dec", 10); prefixLatinGreek.put("de", 10);
		prefixLatinGreek.put("deca", 10);
		
		//eleven
		prefixLatinGreek.put("undec", 11); prefixLatinGreek.put("unde", 11);
		prefixLatinGreek.put("hendeca", 11);
		
		//twelve
		prefixLatinGreek.put("duodec", 12); prefixLatinGreek.put("duode", 12);
		prefixLatinGreek.put("dodeca", 12);
		
		//thirteen
		prefixLatinGreek.put("tridec", 13); prefixLatinGreek.put("tride", 13);
		prefixLatinGreek.put("triskaideca", 13); prefixLatinGreek.put("trisdeca", 13); 
		prefixLatinGreek.put("trideca", 13);
		
		//fourteen
		prefixLatinGreek.put("quatuordec", 14); prefixLatinGreek.put("tetrakaideca", 14);
		prefixLatinGreek.put("tetradeca", 14); 
		
		//fifteen
		prefixLatinGreek.put("quindec", 15); prefixLatinGreek.put("quinde", 15);
		prefixLatinGreek.put("pendeca", 15);
		
		//sixteen
		prefixLatinGreek.put("sedec", 16); prefixLatinGreek.put("sede", 16);
		prefixLatinGreek.put("hexadeca", 16); prefixLatinGreek.put("hexadec", 16);
		
		//seventeen
		prefixLatinGreek.put("septendec", 17); prefixLatinGreek.put("septende", 17);
		prefixLatinGreek.put("heptadeca", 17); 
		
		//seventeen
		prefixLatinGreek.put("septendec", 17); prefixLatinGreek.put("septende", 17);
		prefixLatinGreek.put("heptadeca", 17); 
	}

}
