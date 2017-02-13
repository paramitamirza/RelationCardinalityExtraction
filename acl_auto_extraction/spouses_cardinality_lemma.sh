#!/bin/sh

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/spouses_yes_train_cardinality.data ./models/spouses_lemma_yes.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/spouses_lemma_yes.model ./data/spouses_yes_test_cardinality.data > ./output/spouses_cardinality_lemma_yes.out

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/spouses_yes_train_cardinality_nummod.data ./models/spouses_lemma_yes_nummod.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/spouses_lemma_yes_nummod.model ./data/spouses_yes_test_cardinality.data > ./output/spouses_cardinality_lemma_yes_nummod.out


./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/spouses_more_yes_train_cardinality.data ./models/spouses_lemma_more_yes.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/spouses_lemma_more_yes.model ./data/spouses_yes_test_cardinality.data > ./output/spouses_cardinality_lemma_more_yes.out

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/spouses_more_yes_train_cardinality_nummod.data ./models/spouses_lemma_more_yes_nummod.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/spouses_lemma_more_yes_nummod.model ./data/spouses_yes_test_cardinality.data > ./output/spouses_cardinality_lemma_more_yes_nummod.out

