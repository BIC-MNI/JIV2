#!/bin/sh
./clean_all.sh

javac -O jiv2/Main.java
jar cf ../bin/jiv2.jar COPYING jiv2

cp ../bin/jiv2.jar ~/www/Atlases/
cp ../bin/jiv2.jar ../demo_web/jiv2-bin/

./clean_all.sh

