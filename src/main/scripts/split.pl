#!/usr/bin/perl
my $fn = shift;
my $prefix = shift;

##################
# This script takes all of Kyle's geotiffs for a given source and
# (a) merges them into a single file
# (b) splits out each band into a file
# (c) colorizes the files (ramp.txt)
# (d) creates the tiles for the web

for (my $i =1; $i <= 2000 ; $i++ ) {
 my $tf = "$prefix-$i.tif";
 my $of = "$prefix-$i-color.tif";

my $cmd = "gdal_translate -b $i $fn $tf";
print $cmd ."\n";

system ($cmd);
my $cmd2 = "gdaldem color-relief $tf ../ramp.txt $of";
print $cmd2."\n";
system ($cmd2);

my $cmd3 = "python ~ubuntu/gdal2tiles_parallel.py -z 0-12 $of";
system($cmd3);

# `rm $tf`

}

