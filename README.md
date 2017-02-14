# RelationCardinalityExtraction

###Requirements
* Java Runtime Environment (JRE) 1.7.x or higher

#####Text processing tools:
* [Stanford CoreNLP 3.7.x](http://stanfordnlp.github.io/CoreNLP/) or higher -- a suite of core NLP tools. The .jar file should be included in the classpath.

#####Other libraries:
* [JSON-java](https://mvnrepository.com/artifact/org.json/json) - JSON for Java
* [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) - an API for parsing command line options passed to programs.

###Usage
_! The input file (per property/relation) must be in the comma-separated values (CSV) format: WikidataID,tripleCount (per line) !_
```
usage: Preprocessing
 -i,--input <arg>       Input file (.csv) path
 -p,--relname <arg>     Property/relation name

 -l,--links             Add Wikipedia title page for WikiURL, 
                        input file will be replaced with new file with: 
                        WikidataID,WikipediaTitle,tripleCount (per line)
 -n,--randomize <arg>   Generate n random instances for testing,
                        saved in [input file]_random[n].csv 
                        
 -s,--sentences         Extract Wikipedia sentences (containing numbers)
                        per WikidataID entity, saved in [input file].jsonl.gz                          
  
 -f,--features          Generate feature file (in column format) for CRF++
 -r,--random <arg>      Input random file (.csv) path for testing 
 -o,--output <arg>      Output directory of feature files (in column
                        format) for CRF++  
```   
The output will be two files as input for CRF++:
* `[relname]_train_cardinality.data`
* `[relname]_test_cardinality.data`

Example:
* `Preprocessing -i data/auto_extraction/wikidata_sample.csv -p sample -s` --> will generate `data/auto_extraction/wikidata_sample.jsonl.gz`
*  

How to train and predict (with) a CRF model? See example at `data/auto_extraction/CRF/sample_cardinality_lemma.sh`. Don't forget to download and install [CRF++](https://taku910.github.io/crfpp/), and set the `$CRFPATH`. The sample output of the CRF model can be seen in 
