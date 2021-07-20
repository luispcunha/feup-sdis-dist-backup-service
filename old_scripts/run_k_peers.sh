#!/bin/bash

if [ $# -eq 1 ]
then
    for i in $(seq 1 $1);
        do gnome-terminal --tab -- ./run_peer.sh $i ap$i &> /dev/null;
    done
fi

if [ $# -eq 2 ]
then
    for i in $(seq 1 $1);
        do gnome-terminal --tab -- ./run_peer.sh $i ap$i $2 &> /dev/null;
    done
fi

if [ $# -eq 3 ]
then
    for i in $(seq 1 $1);
        do gnome-terminal --tab -- ./run_peer.sh $(($i - 1 + $3)) ap$(($i - 1 + $3)) $2 &> /dev/null;
    done
fi
