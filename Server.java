import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * @author Shiva
 * 
 */

class ServerThread extends SocketHelper {

    protected int clientId = -1;                                   // initialize to invalid value
    
    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("[Server] Server started! "); 
                String message = (String) this.in.readObject();
                System.out.println("[Server] Received message : \"" + message + "\" from client" + clientId);
                
                int index;
                switch (message) {        

                    case "registerPeer":
                        int peerId = this.generatePeerId();         // generate peerid for this client and aend it's port also
                        int port = Server.peerList.get(peerId);
                        send(peerId);
                        send(port);
                        break;

                    case "getFilename":                             // name of the file being distributed
                        send((Object) this.filename);
                        break;

                    case "getPeerMap":                              //send peermap
                        System.out.print("[Server] peer list:");
                        int mapId = this.in.readInt();
                        for(int peer: Server.peerList.keySet()) {
                            System.out.print(peer + " ");
                        }
                        System.out.println();
                        send(Server.peerList);
                        send((Object)(Server.peerConfiguration.get(mapId)).get(1));             // send downloadPeer
                        send((Object)(Server.peerConfiguration.get(mapId)).get(2));             // send uploadPeer
                        break;

                    case "getPacketList":                           // Send packet list (only indices,not object)
                        ArrayList<Integer> packetIndicesList = new ArrayList<Integer>(this.currentPacketList.size());
                        for(Integer key: this.currentPacketList.keySet()) {
                                packetIndicesList.add(key);
                        }
                        send(packetIndicesList);
                        break;

                    case "getPacketInfo":
                        index = this.in.readInt();                  // first int is packet number
                        send(index);                                // send that packet
                        send(this.currentPacketList.get(index));    // send data 
                        break;

                    case "store":                                   // store this object 
                        index = this.in.readInt();
                        Object packet = (Object) this.in.readObject();
                        break;

                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("[" + this.getName() + "]: session ended.");
                Server.peerList.remove(clientId);
                return;
            }
        }
    }

    private int generatePeerId() {                                 // generate peerid for the connected client
        // TODO Auto-generated method stub
        int id = 0;
        while(id<50){                                              // so that it reads all numbers in configuration 

            if(Server.peerConfiguration.containsKey(id) ){
                if( !Server.peerList.containsKey(id) ) {
                    Server.peerList.put(id, (Server.peerConfiguration.get(id)).get(0));
                    return id;
                }
            } 
            id++;
        }
        return -1;                                                 //when config is not proper                   
    }
}

public class Server extends Peer{                                  //fileowner

    private String peerName = "Server";
    private String filePath = "onwritingwell.pdf";
    private static int port = 50000;

    private ServerSocket serverSocket;
    public static HashMap<Integer, Integer> peerList;
    public static HashMap<Integer, ArrayList<Integer>> peerConfiguration = new HashMap<Integer, ArrayList<Integer>>();

    static{                                                        // initiate if peerlist is null
        if (peerList == null) {
            peerList = new HashMap<Integer, Integer>();
        }
    }

    public static void main(String[] args) {
        configure();
        String file = "onwritingwell.pdf";

        if(args.length > 0) {
            file = args[0];
        }

        new Server(file, port).Start();
    }

    private static void configure() {
        // TODO Auto-generated method stub
        try {
            Scanner scanner = new Scanner(new FileInputStream("configuration.txt"));
            int serverId = scanner.nextInt();
            port = scanner.nextInt();

            while(scanner.hasNext()) {
                int peerId = scanner.nextInt();
                ArrayList<Integer> peerTuples = new ArrayList<Integer>();
                peerTuples.add(scanner.nextInt());
                peerTuples.add(scanner.nextInt());
                peerTuples.add(scanner.nextInt());
                peerConfiguration.put(peerId, peerTuples);
            }
            scanner.close();
        } catch (Exception e) {
            port = 50000;
            System.out.println("error in loading configuration, set default port ");
        }
    }

    public Server(String filePath, int port) {                    // server constructor
        if(filePath!=null){
            if(new File(filePath).exists()){
                this.filePath = filePath;
            }
        }
        this.port = port;

        try {
            serverSocket = new ServerSocket(this.port);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } 
        this.splitFile();
    }

    protected void splitFile() {
        // TODO Auto-generated method stub
        try {
            int size;
			File   file       =  new File(this.filePath);
			String fileName   =  file.getName();

			String[] array    =  fileName.split("\\.");
			String   name     =  array[0];
			String   type     =  array[1];
            byte[]   packet   =  new byte[this.packetSize];
            int      index    =  0;
            Object   ob       =  null;
            FileInputStream filestream = new FileInputStream(this.filePath);

            while ((size = filestream.read(packet)) != -1) {
				
                File newFile = new File(file.getParent(), name + "-"+ String.format("%d", index+1) + "." + type); 
                FileOutputStream out = new FileOutputStream(newFile);
                out.write(packet, 0, size);
                out.close();
                ob = (Object) newFile;
                packetList.put(index,ob);
                index++;
            }
            System.out.println("[Server] Total number of packets : " + index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Start() {
        // TODO Auto-generated method stub
        try {
            while(true) {
                System.out.println(this.peerName + " accepts connections...");
                Socket socket = serverSocket.accept();

                ServerThread serverThread = new ServerThread();
                serverThread.setPacket(packetList);                // method in socketthread
                serverThread.setFilename(this.filePath);           // set given file name 
                serverThread.setSocket(socket);                    // method in socketthread
                serverThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
