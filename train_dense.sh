#!/bin/sh


TRAIN_FILE=$1
BASEDIR=`dirname $TRAIN_FILE`
echo `realpath $TRAIN_FILE` > `realpath $BASEDIR`/train_files.txt
export DENSEPARSER_VECTOR_FILE=`realpath $2`  # very important, that env variable is called in the java code      
OUTGRMPREF=$3

if test "$4" == "" ; then
	SPLIT=2
else
	SPLIT=$4
fi


java -Xmx7000m -ss10m -cp lib:src:classes edu.umd.clip.parser.GrammarTrainer -train $BASEDIR/train_files.txt -out $OUTGRMPREF -jobs 8 -numSplits $SPLIT -lang english -seed 0 -rare 5
cp  "$OUTGRMPREF-$SPLIT.gr" "$OUTGRMPREF-final.gr"

echo "****** Final Grammar is here" 
echo "$OUTGRMPREF-final.gr"