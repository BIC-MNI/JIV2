#!/usr/local/bin/perl5 -w
#
# $Id: jiv.pl,v 1.1 2001-04-08 00:24:13 cc Exp $
#
# authors: {stever,crisco}@bic.mni.mcgill.ca
#

use strict;

use Getopt::Tabular;
use MNI::Startup qw/ nocputimes/;
use MNI::FileUtilities;
use MNI::PathUtilities;
use MNI::Spawn;
use MNI::MincUtilities qw(:geometry);
use MNI::MiscUtilities qw(:all);

# this _needs_ to be defined
my $JIVCode = '/data/web/users/crisco/jiv/prod/jiv.jar';

# optional (if not defined, a JVM named "java" will be searched for 
# in the default path) 
#my $JavaVM= '/usr/local/jdk_3.1.1/usr/java/bin/java';
#my $JavaVM = '/data/web/tools/java/Linux/jdk118/bin/java';

# optional (define only if required by the JVM...)
#my $JavaLib= '/usr/local/jdk_3.1.1/usr/java/lib/classes.zip';
#my $JavaLib = '/data/web/tools/java/Linux/jdk118/lib/classes.zip';

# optional...
my $JVMOptions= '-mx90m';



MNI::Spawn::RegisterPrograms( [qw/minctoraw/] )
    or exit 1;
$JavaVM ||= 'java';
$JavaLib ||= '';
{
    my( $dir, $base, $ext)= MNI::PathUtilities::split_path( $JavaVM);
    my $jvm_fully_qualified= $dir ? 
	$JavaVM : MNI::FileUtilities::find_program( $JavaVM);
    exit 1 unless $jvm_fully_qualified;
    MNI::Spawn::RegisterPrograms( { 'jvm' => $jvm_fully_qualified })
	or exit 1;
}


# --- set the help & usage strings ---
my $help = <<HELP;

A simple wrapper around "JIV": it runs the Java viewer on all 
the minc volumes given as arguments.

Restrictions:
 - all volumes must have the dimensions: 181x217x181
 - all volumes must have the same sampling; if this is not the
   standard MNI-ICBM sampling, then world coordinates will not
   be available in JIV (only voxel coordinates)
HELP

my $usage = <<USAGE;
usage:  $ProgramName [options] mincfile1 [ mincfile2 ...]
     or $ProgramName [options] mincfile1:alias1 [ mincfile2:alias2 ...]
        $ProgramName -help to list options
USAGE

Getopt::Tabular::SetHelp( $help, $usage );


# --- process options ---
my $sync = 0;
my $view = 1;
my @options = 
  ( @DefaultArgs,     # from MNI::Startup
    ['-sync', 'boolean', 0, \$sync,
     "start with all volume cursors synchronized [default: $sync]"],
    ['-view', 'boolean', 0, \$view,
     "launch viewer [default: $view]"],
  );

GetOptions( \@options, \@ARGV ) 
  or exit 1;
die "$usage\n" unless @ARGV > 0;


# --- process the input files ---
my $config;
$config .= "jiv.sync = true\n" if $sync;

MNI::FileUtilities::check_output_path("${TmpDir}/raw/") 
  or exit 1;

my $panel = 0;
my( @common_start, @common_step, @common_dir_cosines)= ( (), (), () );
foreach (@ARGV) {
    my ($in_mnc,$alias) = split(/:/);

    ($in_mnc) = MNI::FileUtilities::check_files( [$in_mnc], 1 ); 
    die unless defined $in_mnc;

    my( $dir, $base, $ext) = 
        MNI::PathUtilities::split_path( $in_mnc, 'last', [qw(gz z Z bz2)]);
    $alias = find_unused_alias($alias || $base);

    my( @start, @step, @length, @dir_cosines)= ( (), (), (), ());
    volume_params( $in_mnc, \@start, \@step, \@length, \@dir_cosines, undef);

    unless( nlist_equal( \@length, [ 181, 217, 181] ) ) {
	die "$in_mnc : wrong dimensions (should be 181x217x181)!\n";
    }
    my $reshape_args= '';
    $reshape_args .= " +xdirection" if( $step[ 0] < 0);
    $reshape_args .= " +ydirection" if( $step[ 1] < 0);
    $reshape_args .= " +zdirection" if( $step[ 2] < 0);
    my( $order, $permutation)= get_dimension_order( $in_mnc);
    $reshape_args .= " -transverse" 
	unless( nlist_equal( $order, [ 2, 1, 0]) );
    if( $reshape_args) {
	my $tmp_mnc= "${TmpDir}/mnc/$dir/${base}.mnc";
        MNI::FileUtilities::check_output_path($tmp_mnc) or exit 1;
	Spawn( "mincreshape ${reshape_args} $in_mnc $tmp_mnc",
	       clobber => $Clobber)
	    unless( (-e $tmp_mnc) && !$Clobber);  
	$in_mnc= $tmp_mnc;
	# @start and @step might have changed...
	volume_params( $in_mnc, \@start, \@step, undef, undef, undef);
    }
    if( $panel == 0) {
	@common_start= @start;
	@common_step= @step;
	@common_dir_cosines= @dir_cosines; 
	unless( $step[ 0] == $step[ 1] && $step[ 1] == $step[ 2] ) {
	    die "$in_mnc : sampling steps need be the same on all 3 axes!\n";
	}
	unless( nlist_equal( \@start, [ -90, -126, -72] ) &&
		nlist_equal( \@step, [ 1, 1, 1] )  &&
		nlist_equal( \@dir_cosines, [ 1,0,0, 0,1,0, 0,0,1 ])  ) {
	    warn "NOTE: non-ICBM sampling detected; " . 
		"world coordinates won't be available...\n";
	    $config .= "jiv.world_coords = false\n";
	}
    }
    else {
	unless( nlist_equal( \@start, \@common_start ) &&
		nlist_equal( \@step, \@common_step ) &&
		nlist_equal( \@dir_cosines, \@common_dir_cosines ) ) {
	    die "$in_mnc : " . 
		"has different sampling than previous volume(s)!\n";
	}
    }

    my $out_raw = "${TmpDir}/raw/$dir/$base";
    MNI::FileUtilities::check_output_path($out_raw) or exit 1;
    Spawn( "minctoraw -norm -byte -range 0 255 $in_mnc",
	   stdout => $out_raw,
	   clobber => $Clobber)
      unless( (-e $out_raw) && !$Clobber);
    
    $config .= "jiv.panel.$panel = $alias\n";
    $config .= "$alias = $out_raw\n";
    ++$panel;
}


# --- set up config and html files then run appletviewer ---

write_file( "${TmpDir}/jiv.conf", $config );

Spawn( "jvm $JVMOptions -classpath ${JIVCode}:$JavaLib " . 
       " jiv.Main ${TmpDir}/jiv.conf" )
    if $view;

# --- end of script ! ---



sub write_file {
    my( $name, $text ) = @_;
    open( OUT, ">$name" )
      or die "error creating `$name' ($!)\n";
    print OUT $text;
    close( OUT )
      or die "error closing file `$name' ($!)\n";
}



my %used_aliases;
sub find_unused_alias {
    my( $base, $count ) = @_;

    my $alias;
    if (defined $count) {
	$alias .= "$base-$count";
    } else {
	$alias = $base;
	$count = 0;
    }

    if ( exists $used_aliases{$alias} ) {
	return find_unused_alias( $base, $count + 1 );
    } else {
	$used_aliases{$alias} = 1;
	return $alias;
    }
}
	
