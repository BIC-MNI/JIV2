#
# $Header: /private-cvsroot/visualization/jiv/doc/Attic/Makefile,v 1.3 2000-04-12 02:15:54 cc Exp $
#
# author: Chris Cocosco <crisco@bic.mni.mcgill.ca>
#

all : manual help

manual : manual.tex inputs.tex gui.tex
	latex manual
	latex manual

help : help.tex gui.tex
	latex help
	latex help

# NB: don't run this on shadow.bic (crashes), nor on bottom.bic (takes ages)
html : manual.tex help.tex inputs.tex gui.tex
	mkdir -p html
	latex2html -split 4 -dir html/manual manual.tex
	latex2html -split 0 -dir html/help help.tex

clean : 
	rm -f *.aux *.log *.dvi _region_.*

