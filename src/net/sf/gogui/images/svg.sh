#!/bin/bash

for i in 8 16 24 32
do
	for file in *.svg
	do
		echo "conversion svg> pgn de " $file
		inkscape --file="$file" --export-png=$(basename $file .svg)"-${i}x${i}.png" --export-width=$i
	done
done
