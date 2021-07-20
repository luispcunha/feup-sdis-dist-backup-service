Distributed backup service for a LAN, using several techniques to maintain fault tolerance (replication of files, ...) and scalability (thread pools, ...). Files are stored in chunks (different chunks can be stored in different peers) and peers communicate using multicast.

## Instructions

Distributed Backup Service

We have compiled and run our project successfully using Java 11 and Java 13.

1. Compilation

In order to compile the project we have created a script named 'compile.sh', located in the root of the project.
To run the script just open a terminal on the project's root folder and run './compile.sh' (given that the script has the necessary permissions).


2. Run

After compilation a build folder is created, which contains all the .class files.
To be able to test the project rmiregistry must be running in the build folder, by executing the command "rmiregistry &".
To facilitate running and testing the peers and the TestApp we have created a few scripts:

    - run_test_app.sh
        - runs the TestApp
        - usage: ./run_test_app.sh <rmi_ap> <operation> <opnd_1> <opnd_2>

    - run_peer.sh
        - runs a Peer
        - usage: ./run_peer.sh <peer_id> <rmi_ap> (runs the peer in version 1.0, using a default MC, MDR and MDB port and address)
                 ./run_peer.sh <peer_id> <rmi_ap> <version> (runs in the specified version, using a default MC, MDR and MDB port and address))
                 ./run_peer.sh <version> <peer_id> <rmi_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> (runs the peer using the provided arguments)


    - run_k_peers.sh
        - runs the specified number of peers in different tabs of the terminal (only works in gnome terminal) with IDs from 1 to k and RMI access points from ap1 to apk
        - usage: ./run_k_peers.sh <num_peers> (runs peers of version 1.0)
        - usage: ./run_k_peers.sh <num_peers> <version> (runs peers with the specified version)
        - usage: ./run_k_peers.sh <num_peers> <version> <starting_id> (runs peers with the specified version with IDs starting in <starting_id>. useful to launch peers of different versions without conflicting IDs)

    - kill_peers.sh
        - kills all currently running peers
        - usage: ./kill_peers.sh

    - purge_peer_files.sh
        - deletes a peer's file system
        - usage: ./purge_peer_files -a (to delete all peers' file systems)
                 ./purge_peer_files <peer_id> (to delete a single peer's file system)

Alternatively the Peers and TestApp may be run in the following way, respectively:
    - java PeerApp <version> <peer_id> <rmi_ap> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>
    - java TestApp <rmi_ap> <operation> <opnd_1> <opnd_2>


3. Versions

There are only two available versions:
    - 1.0 - all default implementations of the protocols
    - 2.0 - all enhanced implementations of the protocols


Bernardo Santos - up201706534
Luís Cunha - up201706746
