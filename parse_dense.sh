#!/bin/sh

GRAMMAR=$1
FILEIN=$2
SUFFIX=$3

echo "here **"
echo $DENSEPARSER_VECTOR_FILE

java -Xmx7000m -ss10m -cp lib:src:classes edu.umd.clip.parser.SingleParser -gr $GRAMMAR -input $FILEIN -output $FILEIN.parsed$SUFFIX -jobs 8
                                                         
