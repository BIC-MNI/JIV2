
# $Id: Makefile,v 2.0 2010/02/26 16:42:13 bailey Exp $

all: prod jar clean

devel: clean
	jikes +P -O jiv2/Main.java

devel-jdk: clean
	javac jiv2/Main.java

debug: clean
	jikes +P -g jiv2/Main.java


prod: clean
	javac -O jiv2/Main.java


jar: 
	jar cf ../bin/jiv2.jar COPYING jiv2

clean:
	rm -f jiv2/*.class jiv2/*.jar

