#!/bin/bash

# check arguments
if [ "$#" -ne 2 ]; then
	echo "Illegal arguments: <libraries folder> <output folder>"
	exit 1
fi

# prepare folders
mkdir entry_points

# get libraries folder and output folder
LIB_FOLDER=$1
OUT_FOLDER=$2

# get every jar in LIB_FOLDER and decompile it
find $LIB_FOLDER -name "*.jar" -printf "%f\n" | xargs -I{} java -jar cfr_0_115.jar $LIB_FOLDER"/"{} --outputdir $OUT_FOLDER"/"{}

# search for every serializable entry point and store it
find $OUT_FOLDER -name "*.jar" -printf "%f\n" | xargs -I{} sh -c "python get_serializable_chain.py "$OUT_FOLDER"/{} > ./entry_points/{}.txt"
