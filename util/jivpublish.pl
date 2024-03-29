#! xPERLx
#
# Script to publish a MINC file for viewing with JIV.  Basically
# a fancy wrapper around Chris Cocosco's minc2jiv.pl
#
# author: bert@bic.mni.mcgill.ca
#
# $Id: jivpublish.pl,v 1.3 2005-08-04 15:13:36 jharlap Exp $
#

use warnings "all";

use Getopt::Tabular;
use MNI::Startup qw/ nocputimes/;
use File::Basename;
use File::Copy;


my $usage = <<USAGE;
usage:  $ProgramName [options] mincfile1 [ mincfile2 ...]
        $ProgramName -help to list options

This perl script will convert the given minc files to JIV format,
placing them in the user's public Web directory.  It also creates the
necessary HTML and configuration files to allow remote viewing of the
files.  All of the files given will be configured to be viewed in a
single JIV panel.
USAGE

#' closes the quote above, solely for syntax highlighting purposes...

Getopt::Tabular::SetHelp( undef, $usage );

my $force = 0;
my $coding = "gray";
my $mode = "hybrid";
my $title = undef;
my $verbose = 0;
my $clobber = 0;

my @options = 
  (
    ['-verbose|-quiet', 'boolean', 0, \$verbose, 
       "print status information and command lines of subprograms [default: -quiet]" ],
    ['-clobber', 'boolean', 0, \$clobber,
     "overwrite any existing files (and allow subprograms to do so as well) [default: -noclobber]" ],
    ['-force', 'boolean', 0, \$force, "accept non-standard direction cosines (rotated coordinate axes) [default: $force]"],
    ['-coding', 'string', 1, \$coding, "select coding type (gray, hotmetal, or spectral) [default: $coding]"],
    ['-mode', 'string', 1, \$mode, "select file transfer mode (hybrid, upfront, or on_demand) [default: $mode]"],
    ['-title', 'string', 1, \$title, "set title for web page and JIV window [default: first filename, minus extensions ]"],
  );



GetOptions( \@options, \@ARGV ) 
  or exit 1;
die "$usage\n" unless @ARGV > 0;

$WebRootPath = "xWEBROOTPATHx";

#
# Get the user's login name, either from the environment or from getpwuid().
#
$username = $ENV{"LOGNAME"} || (getpwuid($<))[0];

#
# Make certain we really got a username. 
#
die "Can't determine your username." unless $username;

my $file_count = scalar(@ARGV);

$jivdir = $WebRootPath . "/users/" . $username . "/JIV";

#
# Create the standard JIV subdirectory.
#
mkdir("$jivdir");

#
# Make certain the directory really exists.
#
die "Can't create directory $jivdir" unless (-d $jivdir);

#
# Initialize the argument list we will pass to minc2jiv.pl
#
my @args = ();

push @args, ("-clobber") if ($clobber);
push @args, ("-force") if ($force);
push @args, ("-output_path", "$jivdir");
#
# Run minc2jiv quietly, it's really annoying otherwise.
#
push @args, ("-quiet") unless ($verbose);

if ($mode eq "hybrid") {
    push @args, ("-slices");
}
elsif ($mode eq "on_demand") {
    push @args, ("-slices", "-novolume");
}
elsif ($mode eq "upfront") {
    # Do nothing
}
else {
    die "Unrecognized mode value: $mode\n";
}

push @args, @ARGV;

$firstfile = $ARGV[0];
# strips off all trailing compression formats, and the last extension.
my ($basename, $dir, undef) = fileparse($firstfile, "\.[^.]+(\.(gz|z|Z|bz2))?");

#
# If the user didn't explicitly select a title, use the base filename of
# the first file.
#
if (!$title) {
    $title = $basename;
}

$cfgfile = "$jivdir/$basename.cfg";
push @args, ("-config", $cfgfile);

$result = system("minc2jiv", @args);

die "Conversion process terminated abnormally." unless ($result == 0);

# copy the jiv.jar into the users jiv dir
copy("xJIVJARPATHx", "$jivdir/jiv.jar");

$htmlfile = "$jivdir/$basename.html";

open(HTMLFILE, ">$htmlfile");
print HTMLFILE "<html>\n<head>\n";
print HTMLFILE "<title>JIV view of $title</title>\n";
print HTMLFILE "<head>\n<body>\n";
print HTMLFILE "<h1>JIV view of $title:</h1>\n";
print HTMLFILE "<applet height=50 width=300 codebase=\"$jivdir\" archive=\"jiv.jar\" code=\"jiv/Main.class\" name=\"$title\">\n";
#
# Convert the $cfgfile path from an absolute path to a path relative to
# the web root directory.
#
$cfgrel = $cfgfile;
$cfgrel =~ s/$WebRootPath(.*)/$1/;
print HTMLFILE "<param name=\"config\" value=\"$cfgrel\">\n";
print HTMLFILE "<p><strong>\n";
print HTMLFILE "You must have enabled Java support in your browser to run this applet!\n";
print HTMLFILE "</strong></p>\n</applet>\n";
#
# This is a big chunk of text that has a bit of help info and credits
# Chris...  Kept separately for maintainability, but merged in by the
# build system.
#
my $ssi_message = <<ENDSSIMESSAGE;
xSSIMESSAGEx
ENDSSIMESSAGE

print HTMLFILE $ssi_message;

print HTMLFILE "<!-- hhmts start -->\n";
$now = localtime;
print HTMLFILE "<br><i>Last modified: $now</i>";
print HTMLFILE "<!-- hhmts end -->\n";
print HTMLFILE "</body>\n</html>\n";
close(HTMLFILE);

print "\nThis MINC file is now available for viewing at:\n";
$htmlrel = $htmlfile;
$htmlrel =~ s/$WebRootPath(.*)/$1/;
print "xWEBHOSTNAMEx$htmlrel\n";







