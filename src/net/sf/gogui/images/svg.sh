#!/bin/bash

for i in 16 24 32 48 64
do
	for file in *.svg
	do
		echo "conversion svg> pgn de " $file
		inkscape --file="$file" --export-png=$(basename $file .svg)"-${i}x${i}.png" --export-width=$i
	done
done

for file in "gogui-black.svg" "gogui-white.svg" "gogui-setup.svg"
do
	inkscape --file="$file" --export-png=$(basename $file .svg)"-8x8.png" --export-width=8
done
