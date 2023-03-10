#!/bin/bash
set -e

sudo apt install ant xsltproc docbook-xsl inkscape openjdk-11-jdk

cd src/net/sf/gogui/images
./svg.sh
cd -

ant
