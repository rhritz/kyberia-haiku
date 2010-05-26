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
my($par)       = $ARGV[3] ? $ARGV[3] : "4bf80885b0be20a499daf9d6"; # startovaci prispevok
my($num_nodes) = $ARGV[4] ? $ARGV[4] : 1000;

my($headers)   = "kyb_headers.txt";

# nacitaj wordfile
open(WORDFILE,"<sku.dic");
my(@word_lines) = <WORDFILE>;
close(WORDFILE);
my($line_count) = $#word_lines;
chomp(@word_lines);

#login();
my @ids;
my %ids_uniq;
my $child;
push(@ids,$par);

#TODO check headers exists, otherwise execute
# staci zavolat raz
sub login {
	system(qq{curl -F "username=$user_id" -F "password=$password" --dump-header $headers "http://localhost:4567/login"});
}

sub troll {
	my($parent,$name,$content)  = @_;
        my @lines = qx^curl -b $headers -F "content=$content" -F "name=$name" $url/$parent/action | grep -o "node_link\\" href=\\"/id/\[\[\:alnum\:\]\]\\{24\\}"^;
        for my $line (@lines) {
            my $lline = substr($line,21);
            chop($lline);
            print $lline;
            if (!defined $ids_uniq{$lline}) {
                push(@ids,$lline);
                $ids_uniq{$lline} = 1;
            }
        }
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
	return $content;
}

for (my $j = 0; $j < 2; $j++) {
    fork();
}
for (my $i = 0; $i < $num_nodes; $i++ ) {
	$par = $ids[rand($#ids)];
        troll($par,create_content(3),join("\n",(create_content(2),
                        create_content(3),create_content(2))));
}

# TODO a dalsi pre get req