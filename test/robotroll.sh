#! /usr/bin/perl
#
# robotroll.sh - Generator prispevkov
# Pouzitie: ./robotroll.sh parametre
#
# treba mat nainstalovany curl aby to bezalo a pravo na zapis do adresara v ktorom bezime
# parametre[default]: domena [http://localhost:4567/id] user[ubik] password[test] node_id[]  pocet_prispevkov[3]
# pre proxy doplnit parameter -x h:p do oboch curl prikazov (man curl)
#
use strict;
use warnings;
use Time::HiRes qw(usleep nanosleep);

# argumenty:
my($url)       = $ARGV[0] ? $ARGV[0] : "http://localhost:4567/id";
my($user_id)   = $ARGV[1] ? $ARGV[1] : "ubik";
my($password)  = $ARGV[2] ? $ARGV[2] : "test";
my($par)       = $ARGV[3] ? $ARGV[3] : "4bee335541fe20a469a852d0";
my($num_nodes) = $ARGV[4] ? $ARGV[4] : 23;

my($headers)   = "kyb_headers.txt";

# nacitaj wordfile
open(WORDFILE,"<sku.dic");
my(@word_lines) = <WORDFILE>;
close(WORDFILE);
my($line_count) = $#word_lines;
chomp(@word_lines);


#TODO check headers exists, otherwise execute
# staci zavolat raz
sub login {
	system(qq{curl -F "username=$user_id" -F "password=$password" --dump-header $headers "http://localhost:4567/login"});
}

sub troll {
	my($parent)  = shift;
	my($event)   = "add";
	my($name)    = shift;
	my($content) = shift;
        print qq{curl -b $headers -F "content=$content" $url/$parent/action | grep -m 1 "node_chosen" > retvalue.txt };
#system(qq{curl -b $headers -F "content=$content" $url/$parent/action | grep -m 1 "node_chosen" > retvalue.txt });
        system(qq{curl -b $headers -F "content=$content" -F "name=$name" $url/$parent/action > /dev/null});
# tu preparsovat output, najst id noveho prispevku a zadat ho do zoznamu prispevkov odkial sa bude nahodne vyberat parent nasledujuceho
#	open(RETVALUE,"<retvalue.txt");
#	my @lines=<RETVALUE>;
#	my $line=$lines[0];
#	close(RETVALUE);
#	my $match = ($line =~ m/\d{7}/) ? "match" : "no match" ;
#	# print("matched: $& $match");
#	my $new_id=$&;
#	#print("result: $new_id");
#	return $new_id;
        return "";
}

sub create_content {
	my($length) = rand(shift());
	my($content) = "";
	for (my $j = 0; $j < $length; $j++) {
                my($w,$ww) = split('/',$word_lines[rand($line_count)]);
		$content .= " ".$w;
	}
	$content =~ s/\'//g;
	#print("content: $content");
	return $content . ".";
}

login();
my @ids;
my $child;
push(@ids,$par);

for (my $i = 0; $i < $num_nodes; $i++ ) {
	#$par = $ids[rand($#ids)];
	#$child = troll($par,create_content(3),create_content(42));
	#push(@ids,$child);
        my $pid = fork();
        if ($pid) {
            # parent
            usleep(3000);
        } else {
            # child
            troll($par,create_content(23),create_content(420));
            exit;
        }
}

# TODO a dalsi pre get req