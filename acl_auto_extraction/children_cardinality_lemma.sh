#!/bin/sh

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/children_yes_train_cardinality_count.data ./models/children_lemma_yes_count.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/children_lemma_yes_count.model ./data/children_yes_test_cardinality.data > ./output/children_cardinality_lemma_yes_count.out

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/children_yes_train_cardinality_num.data ./models/children_lemma_yes_num.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/children_lemma_yes_num.model ./data/children_yes_test_cardinality.data > ./output/children_cardinality_lemma_yes_num.out

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/children_yes_train_cardinality_count_nummod.data ./models/children_lemma_yes_count_nummod.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/children_lemma_yes_count_nummod.model ./data/children_yes_test_cardinality.data > ./output/children_cardinality_lemma_yes_count_nummod.out

./tools/CRF++-0.58/crf_learn -p 4 ./templates/template_lemma.txt ./data/children_yes_train_cardinality_num_nummod.data ./models/children_lemma_yes_num_nummod.model
./tools/CRF++-0.58/crf_test -v2 -m ./models/children_lemma_yes_num_nummod.model ./data/children_yes_test_cardinality.data > ./output/children_cardinality_lemma_yes_num_nummod.out

