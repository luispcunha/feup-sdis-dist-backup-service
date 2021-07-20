#!/bin/bash

if [ $# -ge 3 ]
then
    java -cp build TestApp $1 $2 "$3" $4
else
    java -cp build TestApp $1 $2
fi
