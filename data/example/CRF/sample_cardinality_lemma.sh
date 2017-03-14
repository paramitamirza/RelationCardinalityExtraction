#!/bin/sh

CRFPATH=/home/paramita/CRF++-0.58

$CRFPATH/crf_learn -p 4 ./template_lemma.txt ../sample_train_cardinality.data ./models/sample_lemma.model
$CRFPATH/crf_test -v2 -m ./models/sample_lemma.model ../sample_train_cardinality.data > ../sample_cardinality_lemma.out

