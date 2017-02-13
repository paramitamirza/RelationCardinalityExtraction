#!/bin/sh

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/division_yes_train_cardinality.data ./models/division_lemma_yes.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/division_lemma_yes.model ./data/division_yes_test_cardinality.data > ./output/division_cardinality_lemma_yes.out

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/division_yes_train_cardinality_nummod.data ./models/division_lemma_yes_nummod.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/division_lemma_yes_nummod.model ./data/division_yes_test_cardinality.data > ./output/division_cardinality_lemma_yes_nummod.out

