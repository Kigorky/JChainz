
#!/bin/bash

if [ "$#" -ne 1 ]; then
	echo "Illegal arguments: <libraries folder>"
	exit 1
fi

# run the decompiler first! (the libs folder contains the entry points)
LIBS_FOLDER=$1

OUTPUT_FILE_PREFIX=$(basename $LIBS_FOLDER)

if [ ! -d output ]; then
	mkdir output
fi

# get entry point number from serializable classes
find $LIBS_FOLDER -printf "%f\n" | grep ".jar" | xargs -I{} sh -c "cat "$LIBS_FOLDER"/{} | grep ".java" | wc -l" > output/$OUTPUT_FILE_PREFIX"_stats.txt"

# compute mean
MEAN=$(cat "output/"$OUTPUT_FILE_PREFIX"_stats.txt" | awk 'BEGIN{ s=0 }; {s+=$1}; END{print s}')
MEAN=$(echo $MEAN"/"$(wc -l < "output/"$OUTPUT_FILE_PREFIX"_stats.txt") | bc -l)

echo "mean: "$MEAN

#compute variance
VARIANCE=0
for n in $(cat "output/"$OUTPUT_FILE_PREFIX"_stats.txt"); do
	VARIANCE=$(echo $VARIANCE" + ("$n" - "$MEAN")^2" | bc -l)
done
VARIANCE=$(echo $VARIANCE" / "$(wc -l < "output/"$OUTPUT_FILE_PREFIX"_stats.txt") | bc -l)
echo "variance: "$VARIANCE
