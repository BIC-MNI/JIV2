#
# $Header: /private-cvsroot/visualization/jiv/doc/Attic/Makefile,v 1.2 2000-04-11 22:22:48 cc Exp $
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
html : manual help
	latex2html -split 4 manual.tex
	latex2html -split 4 help.tex

clean : 
	rm -f *.aux *.log *.dvi _region_.*

