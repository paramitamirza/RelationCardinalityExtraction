#!/bin/sh

for n in one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty thirty forty fifty sixty seventy eighty ninety hundred thousand million
do
    echo $n
    zgrep /c/en/$n/ conceptnet-assertions-5.5.0.csv.gz > conceptnet_number_related/$n.txt
done
