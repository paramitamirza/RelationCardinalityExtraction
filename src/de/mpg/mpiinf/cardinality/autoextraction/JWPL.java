package de.mpg.mpiinf.cardinality.autoextraction;

import de.tudarmstadt.ukp.wikipedia.api.Category;
import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiPageNotFoundException;

public class JWPL implements WikiConstants {
	
	public static void main(String[] args) throws WikiApiException {
		
		// configure the database connection parameters
        DatabaseConfiguration dbConfig = new DatabaseConfiguration();
        dbConfig.setHost("nanook");
        dbConfig.setDatabase("paramita");
        dbConfig.setUser("paramita");
        dbConfig.setPassword("cat&bear16");
        dbConfig.setLanguage(Language.english);
        
        // Create a new English wikipedia.
        Wikipedia wiki = new Wikipedia(dbConfig);

        // Get the category "Säugetiere" (mammals)
        String title = "Säugetiere";
        Category cat;
        try {
            cat = wiki.getCategory(title);
        } catch (WikiPageNotFoundException e) {
            throw new WikiApiException("Category " + title + " does not exist");
        }
	}

}
