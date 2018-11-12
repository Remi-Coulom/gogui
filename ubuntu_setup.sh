#!/bin/bash
set -e

sudo apt install ant xsltproc docbook-xsl inkscape

cd /tmp
wget https://www.randelshofer.ch/quaqua/files/quaqua-8.0.nested.zip
unzip quaqua-8.0.nested.zip
unzip quaqua-8.0.zip
cd -

mkdir -p lib
cd lib
cp /tmp/Quaqua/dist/quaqua.jar .
cd -

cd src/net/sf/gogui/images
./svg.sh
cd -

ant
