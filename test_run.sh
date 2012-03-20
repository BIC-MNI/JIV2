#!/bin/sh
./clean_all.sh

java -classpath ../bin/jiv2.jar -Xmx250m jiv2.Main ../demo/sample_cyno.CONFIG
