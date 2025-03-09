Shivdeep Nutheti

Files required:
Client1.java, Client2.java, Client3.java, Client4.java, Client5.java, Server.java, Peer.java, SocketHelper.java, configuration.txt, file to be distributed


Steps to execute:

1. Compile all the files,javac Server.java,Client1.java, Client2.java, Client3.java,Client4.java, Client5.java in 6 terminals.
2. Set the configuration in configuration.txt. (Sample configuration set)
3. Run server : java Server filename           (default: onwritingwell.pdf)
4. Run all clients  Eg: java Client1 50000
	else java Client1 ( it will serverport take from config file)
	else default  server port is 50000.
5. It generates summary files(order of packets received), merged files, Packets folders for all 5 peers .

Result:
1. Server divides the file into chunks with max size = 100 KB.
2. When a peer-i gets a file,it is saved into "peer-i packets" folder and it's index is stored in "[peer-i] summary.txt" file.
3. After a peer-i gets all the packets, it merges them to form "[peer-i] filename" file.







