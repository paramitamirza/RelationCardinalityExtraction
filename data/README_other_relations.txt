prefix schema: <http://schema.org/>
PREFIX wikibase: <http://wikiba.se/ontology#>
PREFIX wd: <http://www.wikidata.org/entity/>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>

NOVEL
---
- novel-a.csv

SELECT ?mainObject ?article (COUNT(DISTINCT(?part)) AS ?countPart)
WHERE {
    ?mainObject wdt:P31 wd:Q1667921 .
    ?part wdt:P179 ?mainObject .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?mainObject ?article

- novel-b.csv

SELECT ?mainObject ?article (COUNT(DISTINCT(?part)) AS ?countPart)
WHERE {
    ?mainObject wdt:P31 wd:Q1667921 .
    ?mainObject wdt:P527 ?part .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?mainObject ?article

BOOK
---
- book-a.csv

SELECT ?mainObject ?article (COUNT(DISTINCT(?part)) AS ?countPart)
WHERE {
    ?mainObject wdt:P31 wd:Q277759 .
    ?part wdt:P179 ?mainObject .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?mainObject ?article

- book-b.csv

SELECT ?mainObject ?article (COUNT(DISTINCT(?part)) AS ?countPart)
WHERE {
    ?mainObject wdt:P31 wd:Q277759 .
    ?mainObject wdt:P527 ?part .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?mainObject ?article

FILM
---
- film-a.csv

SELECT ?mainObject ?article (COUNT(DISTINCT(?part)) AS ?countPart)
WHERE {
    ?mainObject wdt:P31 wd:Q24856 .
    ?part wdt:P179 ?mainObject .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?mainObject ?article

- film-b.csv

SELECT ?mainObject ?article (COUNT(DISTINCT(?part)) AS ?countPart)
WHERE {
    ?mainObject wdt:P31 wd:Q24856 .
    ?mainObject wdt:P527 ?part .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?mainObject ?article

COUNTRY DIVISION
---
country-div.csv

SELECT ?single ?article (COUNT(DISTINCT(?division)) AS ?countDivision)
WHERE {
    ?single wdt:P31 wd:Q6256 .
    ?single wdt:P150 ?division .
    OPTIONAL {
      ?article schema:about ?single .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?single ?article

ALL DIVISION (not only COUNTRY) --> query timeout
---
div.csv

SELECT ?single ?article (COUNT(DISTINCT(?division)) AS ?countDivision)
WHERE {
    ?single wdt:P150 ?division .
    OPTIONAL {
      ?article schema:about ?single .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?single ?article

SPOUSE
---

spouse.csv

SELECT ?person ?article (COUNT(DISTINCT(?o)) AS ?countSpouses) WHERE
{
    ?person p:P26 ?o .
    OPTIONAL {
      ?article schema:about ?person .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} GROUP BY ?person ?article

TV SERIES
---

seasons.csv

SELECT ?mainObject ?article ?part 
WHERE {
    ?mainObject wdt:P31 wd:Q5398426 .
    ?mainObject wdt:P2437 ?part .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} 

episodes.csv

SELECT ?mainObject ?article ?part
WHERE {
    ?mainObject wdt:P31 wd:Q5398426 .
    ?mainObject wdt:P1113 ?part .
    OPTIONAL {
      ?article schema:about ?mainObject .
      ?article schema:inLanguage "en" .
      FILTER (SUBSTR(str(?article), 1, 25) = "https://en.wikipedia.org/")
    }
} 