#!/bin/bash

MC_ADDRESS=239.255.255.255
MC_PORT=4000
MDB_ADDRESS=239.255.255.254
MDB_PORT=4000
MDR_ADDRESS=239.255.255.253
MDR_PORT=4000

DEFAULT_VERSION=1.0

if [ $# -eq 2 ]
then
    java -cp build PeerApp $DEFAULT_VERSION $1 $2 $MC_ADDRESS $MC_PORT $MDB_ADDRESS $MDB_PORT $MDR_ADDRESS $MDR_PORT
fi

if [ $# -eq 3 ]
then
    java -cp build PeerApp $3 $1 $2 $MC_ADDRESS $MC_PORT $MDB_ADDRESS $MDB_PORT $MDR_ADDRESS $MDR_PORT
fi

if [ $# -eq 9 ]
then
    java -cp build PeerApp $@
fi
