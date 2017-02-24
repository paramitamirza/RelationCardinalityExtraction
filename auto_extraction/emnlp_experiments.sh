#!/bin/sh

CRFPATH=/local/home/paramita/CRF++-0.58

#nummod
$CRFPATH/crf_learn -p 4 ./templates/template_lemma.txt ./data/cardinality_compositional/children_count_train_cardinality.data ./models/children_count_lemma_nummod.model
$CRFPATH/crf_test -v2 -m ./models/children_count_lemma_nummod.model ./data/cardinality_compositional/children_count_test_cardinality.data > ./acl_output/children_count_lemma_nummod_cardinality.out

$CRFPATH/crf_learn -p 4 ./templates/template_lemma.txt ./data/cardinality_compositional/children_num_train_cardinality.data ./models/children_num_lemma_nummod.model
$CRFPATH/crf_test -v2 -m ./models/children_num_lemma_nummod.model ./data/cardinality_compositional/children_num_test_cardinality.data > ./acl_output/children_num_lemma_nummod_cardinality.out

$CRFPATH/crf_learn -p 4 ./templates/template_lemma.txt ./data/cardinality_compositional/spouses_train_cardinality.data ./models/spouses_lemma_nummod.model
$CRFPATH/crf_test -v2 -m ./models/spouses_lemma_nummod.model ./data/cardinality_compositional/spouses_test_cardinality.data > ./acl_output/spouses_lemma_nummod_cardinality.out

$CRFPATH/crf_learn -p 4 ./templates/template_lemma.txt ./data/cardinality_compositional/division_train_cardinality.data ./models/division_lemma_nummod.model
$CRFPATH/crf_test -v2 -m ./models/division_lemma_nummod.model ./data/cardinality_compositional/division_test_cardinality.data > ./acl_output/division_lemma_nummod_cardinality.out

$CRFPATH/crf_learn -p 4 ./templates/template_lemma.txt ./data/cardinality_compositional/series_train_cardinality.data ./models/series_lemma_nummod.model
$CRFPATH/crf_test -v2 -m ./models/series_lemma_nummod.model ./data/cardinality_compositional/series_test_cardinality.data > ./acl_output/series_lemma_nummod_cardinality.out