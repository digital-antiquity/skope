#!/usr/bin/perl

##################################################################################################################
#
# This script takes the input files from Kyle and follows a few processes:
# 1. it merges them into latitude based groups so there are fewer files to manage
# 2. it then extracts each 'band' of data from the geotiff into its own file
# 3. finally, it recombines the latitude based groups into a single tile per band
#
#
# This script is dependent on GDAL being installed on your local machine

use threads;

my $GDAL_MERGE = "/Library/Frameworks/GDAL.framework/Programs/gdal_merge.py";
my $GDAL_TRANSLATE = "gdal_translate";

mkdir("out");
mkdir("out/tmp");
mkdir("out/comb");
mkdir("in");

my @prefixes = ();

my $cmd="mv *.recon.* in/";
system($cmd) or print "$?";

print ">> PROCESSING INPUTS AND CREATING LATITUDE BASED PASSES \n\n";

my @seq = (103..115);
my @prefixes = ();
my @threads = ();
# creates a "strip" based on latitude of all data for that strip across 2000 years (useful for getting all data in one place)
foreach my $i (@seq) {

	my $prefix = $i."W_comb";
	my $outfile = "out/comb/".$prefix.".tif";
	push @prefixes, $prefix;
	if (! -e $outfile) {
		my $cmd = "$GDAL_MERGE -o $outfile in/".$i."W*.recon.tif";
	    my $thr = async(sub{
			print $cmd." --> ";
			system($cmd) or print STDERR "$?";
			print "\r\n";
	    } ,cmd);
        push(@threads, $thr);
	}
}

foreach my $thr (@threads) {
    $thr->join();
}


print "\n>> PROCESSING LATITUDE BASED PASSES EXTRACTING BANDS AND CREATING UNIFIED BANDS\n\n";

# taking each "strip" and pulling out each "year's" worth of data
foreach my $i (1..2000) {
	@threads = ();
	print ">>> PROCESSING BAND $i \n\n";	
	foreach my $prefix (@prefixes) {
	    my $thr = async(sub{
			my $filename="out/comb/".$prefix.".tif";
			my $outfile="out/tmp/".$prefix."_".$i.".tif";
			my $cmd = "$GDAL_TRANSLATE -of GTiff -b $i $filename $outfile";
			print " $i $prefix >> " . $cmd." --> ". (system($cmd) or print STDERR "$?"). "\r\n";	    	
	    } ,$prefix);
        push(@threads, $thr);
	}

	foreach my $thr (@threads) {
	    $thr->join();
	}

	# taking each "year" and creating a combined tiff
	my $outfile = "out/merge_".$i.".tif";
	my $input = "out/tmp/*_".$i.".tif";
	my $cmd = "$GDAL_MERGE -o $outfile $input";
	print "\n  ===> " . $cmd . " --> ";
	system($cmd) or print STDERR "$?";
}

