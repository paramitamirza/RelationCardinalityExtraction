# RelationCardinalityExtraction

###Requirements
* Java Runtime Environment (JRE) 1.7.x or higher

#####Text processing tools:
* [Stanford CoreNLP 3.7.x](http://stanfordnlp.github.io/CoreNLP/) or higher -- a suite of core NLP tools. The .jar file should be included in the classpath.

#####Other libraries:
* [JSON-java](https://mvnrepository.com/artifact/org.json/json) - JSON for Java
* [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) - an API for parsing command line options passed to programs.

###Preprocessing
_! The input file (per property/relation) must be in the comma-separated values (CSV) format: `WikidataID,tripleCount` (per line) !_
```
usage: Preprocessing
 -i,--input <arg>       Input file (.csv) path
 -p,--relname <arg>     Property/relation name

 -l,--links             Add Wikipedia title page for WikiURL, 
                        input file will be replaced with new file with: 
                        `WikidataID,WikipediaTitle,tripleCount` (per line)
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
```
Preprocessing -i data/auto_extraction/wikidata_sample.csv -p sample -s
```
will generate `data/auto_extraction/wikidata_sample.jsonl.gz`
```
Preprocessing -i data/auto_extraction/wikidata_sample.csv -p sample -f -r data/auto_extraction/wikidata_sample_random10.csv -o data/auto_extraction/
```
will generate `data/auto_extraction/sample_train_cardinality.data` and `data/auto_extraction/sample_test_cardinality.data` 

###Train and Predict (with) CRF++
How to train and predict (with) a CRF model? See example at `data/auto_extraction/CRF/sample_cardinality_lemma.sh`. 
_! Don't forget to download and install [CRF++](https://taku910.github.io/crfpp/), and set the `$CRFPATH`. !_
The sample output of predicting with a CRF model can be seen in `data/auto_extraction/sample_cardinality_lemma.out`.

###Evaluate
_! The input file (per property/relation) must be in the comma-separated values (CSV) format: WikidataID,WikipediaTitle,tripleCount (per line). The CRF++ output file is according to the explanation above. !_
```
usage: Evaluation
 -i,--input <arg>    Input evaluation file (.csv) path
 -c,--crfout <arg>   CRF++ output file (.out) path
 -o,--output <arg>   Output file (.csv) path

```
The output will be precision, recall and F1-score measures. 
If the output file is specified (-o [output file]), then the result is printed into file with the following format:
`WikidataID,WikipediaURL,tripleCount,predictedCardinality,probabilityCardinality,TextualEvidence` (per line)
as exemplified in `data/auto_extraction/predicted_sample_cardinality.csv`,

