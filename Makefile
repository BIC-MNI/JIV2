
# $Id: Makefile,v 1.1 2001-04-08 00:04:18 cc Exp $

all: prod jar

devel:
	jikes -depend +P -deprecation -O jiv/Main.java

devel-jdk: clean
	javac -depend -deprecation jiv/Main.java

debug: clean
	jikes -depend +P -deprecation -g jiv/Main.java

prod: clean
	javac -depend -deprecation -O jiv/Main.java

jar: 
	jar cf jiv/jiv.jar COPYING jiv/*.class
#	mv jiv/jiv.jar ../../../www/work/jiv/

clean:
	rm -f jiv/*.class jiv/*.jar

