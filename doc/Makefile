#
# $Id: Makefile,v 1.5 2001-10-09 02:45:50 cc Exp $
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
	mkdir -p html/manual html/help
	latex2html -split 4 -dir html/manual manual.tex
	latex2html -split 0 -dir html/help help.tex

clean : 
	rm -rf *.aux *.log *.dvi _region_.* html/

