#
# $Header: /private-cvsroot/visualization/jiv/doc/Attic/Makefile,v 1.1 2000-04-11 20:40:11 cc Exp $
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

clean : 
	rm -f *.aux *.log *.dvi _region_.*

