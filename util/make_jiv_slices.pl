#!/usr/local/bin/perl5 -w

# $Id: make_jiv_slices.pl,v 1.1 2001-09-21 16:42:14 cc Exp $
#
# Description: this is a preprocessing script for generating the 
# individual slices of a MINC volume (necessary for the 
# 'download on demand' mode of JIV)
#
# Requires: mni-perllib (available from ftp.bic.mni.mcgill.ca)
#
# Inspired by: trislice.pl by John Sled
#
#
# Copyright (C) 2001 Chris Cocosco (crisco@bic.mni.mcgill.ca)
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
# License for more details.

# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software Foundation, 
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA, 
# or see http://www.gnu.org/copyleft/gpl.html


use strict;

use Getopt::Tabular;
use MNI::Startup qw/ nocputimes/;
use MNI::FileUtilities;
use MNI::PathUtilities;
use MNI::Spawn;
use MNI::MincUtilities qw(:geometry);


MNI::Spawn::RegisterPrograms( [qw/mincextract mincexpand/] )
    or exit 1;

my $usage = <<USAGE;
usage:  $ProgramName [options] mincfile1 [ mincfile2 ...]
        $ProgramName -help to list options
USAGE
Getopt::Tabular::SetHelp( undef, $usage );

my $output_path= '.';
my $gzip= 1;
my $labels = 0;
my @options = 
  ( @DefaultArgs,     # from MNI::Startup
    ['-output_path', 'string', 1, \$output_path, "output path [default: $output_path]"], 
    ['-gzip', 'boolean', 0, \$gzip, "gzip slices [default]"],
    ['-labels', 'boolean', 0, \$labels, "image data are labels (preserve the file\'s \"valid range\") [default: $labels]"],
  );
GetOptions( \@options, \@ARGV ) 
  or exit 1;
die "$usage\n" unless @ARGV > 0;


# NB: it's safer to '-norm' all the time (otherwise minctoraw
    # might decide to scale differently data from different slices --
    # e.g. if the volume has different min-max values for each
    # slice...)
my $norm_options= $labels ? "-norm" : "-norm -range 0 255";
my $compress= ($gzip ? "| gzip -c9 " : "") ;

my %base_names_seen;
MNI::FileUtilities::check_output_path("$TmpDir/") or exit 1;

foreach my $in_mnc (@ARGV) {

    ($in_mnc) = MNI::FileUtilities::check_files( [$in_mnc], 1 ); 
    die unless defined $in_mnc;
    my( $dir, $base, $ext) = 
      MNI::PathUtilities::split_path( $in_mnc, 'last', [qw(gz z Z bz2)]);

    croak "duplicate base name in $in_mnc \n" if $base_names_seen{ $base};
    $base_names_seen{ $base}= 1;

    Spawn( "mincexpand $in_mnc $TmpDir/${base}.mnc");
    $in_mnc= "$TmpDir/${base}.mnc";

    $dir= "$output_path/$base";
    foreach (qw/ t s c/) {
        MNI::FileUtilities::check_output_path("$dir/$_/") or exit 1;
    }
    $ext= ".raw_byte" . ($gzip ? ".gz" : "") ;

    my( @length)= ( ());
    volume_params( $in_mnc, undef, undef, \@length, undef, undef);
    my( $s, $out_raw);

    #fixme: this assumes a transverse volume (z y x), and positive steps !!
    #fixme: decompress the minc 1st ... (optimization)

    for( $s= 0 ; $s < $length[2]; ++$s) {
	$out_raw = "$dir/t/$s$ext";
	croak "$out_raw exists and -clobber not given" 
	    if (-e $out_raw) && !$Clobber;
	Spawn( "mincextract ${norm_options} -byte -start $s,0,0 -count 1,$length[1],$length[0] $in_mnc $compress >$out_raw");
    }
    for( $s= 0 ; $s < $length[0]; ++$s) {
	$out_raw = "$dir/s/$s$ext";
	croak "$out_raw exists and -clobber not given" 
	    if (-e $out_raw) && !$Clobber;
	Spawn( "mincextract ${norm_options} -byte -start 0,0,$s -count $length[2],$length[1],1 $in_mnc $compress >$out_raw");
    }
    for( $s= 0 ; $s < $length[1]; ++$s) {
	$out_raw = "$dir/c/$s$ext";
	croak "$out_raw exists and -clobber not given" 
	    if (-e $out_raw) && !$Clobber;
	Spawn( "mincextract ${norm_options} -byte -start 0,$s,0 -count $length[2],1,$length[0] $in_mnc $compress >$out_raw");
    }


}
