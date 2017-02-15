package de.mpg.mpiinf.cardinality.autoextraction;

import java.util.Map;

public class ConceptNetNumbers {
	
	private String[] numbers = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", 
			"twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety", 
			"hundred", "thousand", "million"};
	
	private Map<String, Integer> prefixLatinGreek;	
	private Map<String, Integer> otherConcepts;
	
	public ConceptNetNumbers() {
		
		//Latin numeral prefix
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("mono", 1);
		prefixLatinGreek.put("bi", 2);
		prefixLatinGreek.put("du", 2);
		prefixLatinGreek.put("di", 2);
		prefixLatinGreek.put("dy", 2);
		prefixLatinGreek.put("tri", 3);
		prefixLatinGreek.put("ter", 3);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		prefixLatinGreek.put("uni", 1);
		
		
	}

}
