#!/usr/local/bin/perl5 -w

# $Id: minc2jiv.pl,v 1.1 2001-10-02 01:27:23 cc Exp $ 
#
# Description: this is a preprocessing script for converting a
# MNI-MINC volume to a format that JIV can read; it can also
# generate the individual slices (necessary for the 
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
use MNI::MincUtilities qw( :geometry :range);
use MNI::MiscUtilities qw(:all);


MNI::Spawn::RegisterPrograms( [qw/ mincreshape mincextract /] )
    or exit 1;

my $usage = <<USAGE;
usage:  $ProgramName [options] mincfile1 [ mincfile2 ...]
        $ProgramName -help to list options
USAGE
Getopt::Tabular::SetHelp( undef, $usage );

my $output_path= '.';
my $gzip= 1;
my $slices = 0;
my $volume = 1;
my @options = 
  ( @DefaultArgs,     # from MNI::Startup
    ['-output_path', 'string', 1, \$output_path, "output path [default: $output_path]"], 
    ['-gzip', 'boolean', 0, \$gzip, "gzip output [default: $gzip]"],
    ['-slices', 'boolean', 0, \$slices, "produce slices (for \"download on demand\") [default: $slices]"],
    ['-volume', 'boolean', 0, \$volume, "produce volume file [default: $volume]"],
  );
GetOptions( \@options, \@ARGV ) 
  or exit 1;
die "$usage\n" unless @ARGV > 0;


my $norm_options= "-norm -range 0 255";
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

    $ext= ".raw_byte" . ($gzip ? ".gz" : "") ;

    my( @start, @step, @length, @dir_cosines, @dimorder)= 
	( (), (), (), (), ());
    volume_params( $in_mnc, \@start, \@step, \@length, \@dir_cosines, undef);

    my( @irange)= volume_minmax( $in_mnc);

    my( $order, $perm)= get_dimension_order( $in_mnc);
    my( @dim_names)= qw/ x y z/;
    @dimorder= map { $dim_names[$_] } @$order;

    # TODO/FIXME: allow for some slop (+/- 5%) in the test ...
    # TODO/FIXME: add a -force option, to allow "dummy" world coordinates
    #     and 
    #  $config .= "jiv.world_coords = false\n";
    #
    unless( nlist_equal( \@dir_cosines, [ 1,0,0, 0,1,0, 0,0,1 ]) ) {
	die "$in_mnc : non-standard direction cosines (that is, rotated coordinate axes) are not supported!\n";
    }

    my $header= '';
    $header .= "size   :  @length\n";
    $header .= "start  :  @start\n";
    $header .= "step   :  @step\n";
    $header .= "order  :  @dimorder\n\n";
    $header .= "imagerange  :  @irange\n";

    $dir= $output_path;

    my $out_header = "$dir/${base}.header";
    MNI::FileUtilities::check_output_path( $out_header) or exit 1;
    write_file( $out_header, $header );

    ### VOLUME: ###

    if( $volume ) {

	my $out_volume = "$dir/$base$ext";
	croak "$out_volume exists and -clobber not given" 
	    if (-e $out_volume) && !$Clobber;
	# reorder the lengths in file order:
	my $counts= join ',', @length[ @$order];
	Spawn( "mincextract ${norm_options} -byte -start 0,0,0 -count $counts $in_mnc $compress >$out_volume");
    }

    next unless $slices;

    ### SLICES ###

    $dir= "$output_path/$base";
    foreach (qw/ t s c/) {
        MNI::FileUtilities::check_output_path("$dir/$_/") or exit 1;
    }

    # NB: the sign of the steps of the volume file is preserved, but
    # the dimensions are reordered in the "canonical" ordering (such
    # that a "transverse" is always y-x).

    # as a side benefit, this also gives a local & un-compressed version
    #
    Spawn( "mincreshape -transverse $in_mnc $TmpDir/${base}_reordered.mnc");
    $in_mnc= "$TmpDir/${base}_reordered.mnc";

    my( $s, $out_raw);

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

# --- end of script ! ---



sub write_file {
    my( $name, $text ) = @_;
    open( OUT, ">$name" )
      or die "error creating `$name' ($!)\n";
    print OUT $text;
    close( OUT )
      or die "error closing file `$name' ($!)\n";
}

