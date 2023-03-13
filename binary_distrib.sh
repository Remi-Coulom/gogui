#!/bin/bash
ant
cd ..
rm -vf gogui-bin.zip
zip -r gogui-bin gogui -x 'gogui/.git*' -x 'gogui/build*'
ls -l gogui-bin.zip
