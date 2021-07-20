#!/bin/bash

if [ $# -ne 1 ]
then
    echo "wrong usage"
fi


if [ "$1" == "-a" ]
then
    rm -rf peer_*
else
    rm -rf peer_$1
fi
