#!/bin/sh


if test "$1" == ""  ; then
	SPLIT=2
else
	SPLIT=$1
fi
BASEDIR=German_Data/
TRAIN=$BASEDIR/Tiger_300/train300.German.gold.ptb.nomorph+paran
WORDVEC=$BASEDIR/German_word2vec/wiki_words.txt
GRAMMARPREF=${TRAIN}__wiki_words.txt-
GRAMMARFINAL=$GRAMMARPREF-final.gr

export DENSEPARSER_VECTOR_FILE=$WORDVEC
TOBEPARSED=German_Data/Tiger_300/dev.German.gold.ptb.nomorph.tokens.tobeparsed
GOLD=German_Data/Tiger_300/dev.German.gold.ptb.nomorph

lines 1 501 < $TOBEPARSED > $TOBEPARSED.500


echo "running: ./train_dense.sh $TRAIN $WORDVEC $GRAMMARPREF $SPLIT"
./train_dense.sh $TRAIN $WORDVEC $GRAMMARPREF $SPLIT

#exit
SUF=".baseline.s$SPLIT.wiki_txt.train300"
./parse_dense.sh $GRAMMARFINAL $TOBEPARSED.500 $SUF
evalb_spmrl -K 500 -p spmrl.prm -L $GOLD $TOBEPARSED.500.parsed$SUF| grep 'F1:'| tee $TOBEPARSED.500.parsed$SUF.res

