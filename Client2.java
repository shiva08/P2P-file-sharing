import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Shiva
 * 
 */
class Client2Socket extends SocketHelper {

    protected int peerId;

    public void setPeerId(int id) {
        this.peerId = id;
    }

    public void run() {
        while (true) {
            try {

                String message = (String) this.in.readObject();        // incoming object
                System.out.println("[" + this.peerName + "] received message \"" + message + "\"");
                int index;
                Object packet;

                switch (message) {

                    case "getPacketList":                               // Send packet list (only indices,not objects)
                        ArrayList<Integer> packetIndicesList = new ArrayList<Integer>(this.currentPacketList.size());
                        for (Integer key : this.currentPacketList.keySet()) {
                            packetIndicesList.add(key);
                        }
                        send(packetIndicesList);
                        break;

                    case "doYouHave":                                    // reply 1 if I have that packet,else 0
                        index = this.in.readInt();
                        if (this.currentPacketList.containsKey(index)) {
                            send(1);
                        } else {
                            send(0);
                        }
                        break;

                    case "getPacketInfo":
                        index = this.in.readInt();                       // first int is packet number
                        send(index);                                     // send that packet
                        send(this.currentPacketList.get(index));         // send data 
                        break;

                    case "store":                                        // received from my neighbour
                        index  = this.in.readInt();                      // packet number
                        packet = (Object) this.in.readObject();          // Save received data

                        if (!this.currentPacketList.containsKey(index)) {
                            currentPacketList.put(index, packet);
                            this.mylist.add(index);
                            savePackets(index,packet);
                            System.out.println("[" + this.peerName + "] received Packet " + index);
                        } else {
                            System.out.println("[" + this.peerName + "] received Packet " + index + " : duplicate");
                        }
                        break;

                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("[" + this.getName() + "]: session ended");
                return;
            }
        }
    }

    private void savePackets(int n, Object packet) {
        // TODO Auto-generated method stub
        try {


            FileOutputStream fos = new FileOutputStream("peer-" + this.peerId + " packets/" +Integer.toString(n+1), false);

            File f = (File) packet;
            FileInputStream fis = new FileInputStream(f);
            byte[] fileBytes = new byte[(int) f.length()];
            int bytesRead = 0;

            bytesRead = fis.read(fileBytes, 0,(int)  f.length());
            assert(bytesRead == fileBytes.length);
            assert(bytesRead == (int) f.length());
                

            fos.write(fileBytes);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class Client2 extends Peer implements Runnable {

    private ServerSocket clientServerSocket;              // server socket for this client

    private final int totalPeers = 5;                     // total peers in this program
    public int serverPort        = 50000;                 // default server port

    private static int port      = -1;                    // initialize with invalid values
    static  int peerId           = -1;                    // initialize with invalid values
    private int downloadPort     = -1, downloadPeer = -1; // initialize improper ports
    private int uploadPort       = -1, uploadPeer   = -1;


    
    public static HashMap<Integer, Integer> peerList = new HashMap<Integer, Integer>();
    public static String getFilename = "a.pdf";            // getFilename - name of file, get from server, default = a.pdf 

    public static void main(String[] args) {
        int serverListeningPort ; 
        if(args.length > 0) {
            serverListeningPort = Integer.parseInt(args[0]);
        } else{
            serverListeningPort = getServerPortFromConfig();
        }
        new Client2(serverListeningPort).Start();
    }

    public Client2(int serverPort) {                       // client constructors
        // TODO Auto-generated method stub
        this.serverPort    = serverPort;        
    }

    private static int getServerPortFromConfig() {
        // TODO Auto-generated method stub
        try {
            Scanner scanner =  new Scanner(new FileInputStream("configuration.txt"));
            int serverId    =  scanner.nextInt();
            port            =  scanner.nextInt();
            scanner.close();
            return port;

        } catch (Exception e) {
            System.out.println("error loading configuration,use default port");
        }
        return port = 50000;
    }

    public static void post(ObjectOutputStream outStream, int value) throws Exception { // helper methods for passing int
        // TODO Auto-generated method stub
        outStream.writeInt(value);
        outStream.flush();
    }

    public static void postData(ObjectOutputStream outStream, Object data) throws Exception { //helper method for passing object
        // TODO Auto-generated method stub
        outStream.writeObject(data);
        outStream.flush();
        outStream.reset();
    }

    public void mergePackets(){
        // TODO Auto-generated method stub
        byte[] fileBytes ;
        int bytesRead = 0;
        File f;
        FileInputStream fis;
        try {

            File fileOut   = new File("["+ peerName + "] " + getFilename);
            if(fileOut.exists())
            {
                fileOut.delete();                             // delete if it exists
            }

            FileOutputStream fileOutputStream = new FileOutputStream(fileOut);
            for(int i = 0; i < packetIndex.size(); i++) {

                f = (File) packetList.get(packetIndex.get(i));
                fis = new FileInputStream(f);
                fileBytes = new byte[(int) f.length()];

                bytesRead = fis.read(fileBytes, 0,(int)  f.length());
                assert(bytesRead == fileBytes.length);
                assert(bytesRead == (int) f.length());
                fileOutputStream.write(fileBytes);
                
                fileBytes = null;
                fis.close();
                fis = null;

            }
            fileOutputStream.flush();
            fileOutputStream.close();

            System.out.println("[" + peerName + "] merged packets.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean allPacketsReceived() {               // return if all packets were received.
        // TODO Auto-generated method stub
        for(int key: packetIndex) {
            if(!packetList.containsKey(key))
            {
                return false;
            }
        }
        mergePackets();                                 // merge all packets 
        summary();                                      // summary file for me

        return true;
    }

    public void Start() {
        // TODO Auto-generated method stub
        try {

            //registerPeer    - get peerId, port
            Socket serverSocket = new Socket("localhost", serverPort);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(serverSocket.getOutputStream());
            postData(objectOutputStream, (Object) "registerPeer");

            ObjectInputStream objectInputStream = new ObjectInputStream(serverSocket.getInputStream());
            peerId   = objectInputStream.readInt();
            port     = objectInputStream.readInt();

            peerName = "peer-" + peerId;
            System.out.println( "I am " + peerName);



            File peerFolder = new File(peerName + " packets");                  // peer folder for saving packets.  
            if(!peerFolder.exists()) {
                peerFolder.mkdir();
            }

            postData(objectOutputStream, (Object) "getFilename");               // what is the name of the file?
            getFilename = (String) objectInputStream.readObject();

            System.out.println(getFilename);

            postData(objectOutputStream, (Object) "getPacketList");             //getPacketList
            packetIndex = (ArrayList<Integer>) objectInputStream.readObject();
            
            int mod = peerId % totalPeers;                                      //initial packets from server

            double fairShare = 1.0 * packetIndex.size() / totalPeers ;          // divide packets fairly

            int start   = (int) (    mod  * fairShare);
            int end     = (int) ( (mod+1) * fairShare);

            while(start<end){

                postData(objectOutputStream, (Object) "getPacketInfo");         // get packet info for this packet
                post(objectOutputStream, packetIndex.get(start));               // for this packet number
                int x = objectInputStream.readInt();
                Object packet = (Object) objectInputStream.readObject();
                packetList.put(x, packet);
                savePackets(x, packet);
                this.mylist.add(x);                              
                System.out.println("Received packet " + packetIndex.get(start) + " from server");
                start++;
            }
            

            while(true){                                                        //get uploadPeer,downloadPeer, their ports

                postData(objectOutputStream, (Object) "getPeerMap");
                post(objectOutputStream, peerId);
                peerList = (HashMap<Integer, Integer>) objectInputStream.readObject();
                System.out.print("[" + peerName + "] Get peer list(" + peerList.size() + "):");
                for(int peer: peerList.keySet()) {
                    System.out.print(peer + " ");
                }
                System.out.println();
                
                downloadPeer = (int) objectInputStream.readObject();
                uploadPeer   = (int) objectInputStream.readObject();

                if(peerList.containsKey(downloadPeer)){
                    downloadPort = peerList.get(downloadPeer);
                }else{
                    downloadPort = 0;
                }

                if(peerList.containsKey(uploadPeer)){
                    uploadPort   = peerList.get(uploadPeer);
                }else{
                    uploadPort   = 0;
                }

                Thread.sleep(1000);

                if(this.downloadPort>0 && this.uploadPort>0){          // if both uploadport,downloadport get valid numbers
                    break;
                }
            } 
            
            System.out.println("[" + peerName + "] uploading to peer-"     + uploadPeer   + " at port " + uploadPort);
            System.out.println("[" + peerName + "] downloading from peer-" + downloadPeer + " at port " + downloadPort);



            (new Thread() {                                            //anonymous class

                @Override
                public void run() {
                    try {
                        System.out.println();
                        Thread.sleep(5000);

                        System.out.println("upload connection establishing.....");

                        Socket uploadSocket              = new Socket("localhost", uploadPort);
                        ObjectOutputStream outUpStream   = new ObjectOutputStream(uploadSocket.getOutputStream());

                        System.out.println("download connection establishing.....");

                        Socket downloadSocket            = new Socket("localhost", downloadPort);
                        ObjectOutputStream outDownStream = new ObjectOutputStream(downloadSocket.getOutputStream());
                        ObjectInputStream inDownStream   = new ObjectInputStream(downloadSocket.getInputStream());

                        System.out.println("upload,download connections established");

                        while (!allPacketsReceived()) {

                            for (int i = 0; i < Client2.packetIndex.size(); i++) {
                                int getIndex = Client2.packetIndex.get(i);
                                if (Client2.packetList.containsKey(getIndex)) {
                                    continue;
                                }

                                System.out.println("[" + Client2.peerName + "] ask peer-" + downloadPeer + " for packet " + getIndex);
                                
                                postData(outDownStream, (Object) "doYouHave");           // do you have that packet??
                                post(outDownStream, getIndex);

                                
                                if (inDownStream.readInt() == 1) {                       //if he has that packet

                                    postData(outDownStream, (Object) "getPacketInfo");   //getPacketInfo for that packet
                                    post(outDownStream,  getIndex);

                                    int infoIndex = inDownStream.readInt();
                                    Object packet = (Object) inDownStream.readObject();

                                    Client2.packetList.put(infoIndex, packet);
                                    savePackets(infoIndex, packet);
                                    Client2.mylist.add(infoIndex);

                                    System.out.println("[" + Client2.peerName + "] received packet " + packetIndex.get(i) + " from peer-" + downloadPeer);

                                } else {
                                    System.out.println("[" + Client2.peerName + "] peer-" + downloadPeer + " doesn't have packet " + getIndex);
                                }
                            }

                            System.out.println("[" + Client2.peerName + "] fetched available packets from peer-" + downloadPeer);

                            for (Integer storeIndex : Client2.packetIndex) {        // send for storage
                                if (!Client2.packetList.containsKey(storeIndex)) {
                                    continue;
                                }
                                postData(outUpStream, (Object) "store");
                                post(outUpStream, storeIndex);
                                postData(outUpStream, Client2.packetList.get(storeIndex));
                            }

                            System.out.println("[" + Client2.peerName + "] packets sent for storage to uploadpeer " + uploadPeer);
                            Thread.sleep(5000);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();

            while(port<0){                                          // if port has n't been assigned properly
                Thread.sleep(100);
            }
            clientServerSocket = new ServerSocket(this.port);

            while (true) {
                Client2Socket clientSocketObject = new Client2Socket();
                Socket socket = null;                               //initialize
                
                try {
                    System.out.println("peer is listening...");
                    socket = clientServerSocket.accept();

                    clientSocketObject.setSocket(socket);           // socket for client1socket (method in socket thread)
                    clientSocketObject.setPeerId(Client2.peerId);   // peerId for client1socket
                    clientSocketObject.setPacket(packetList);       // packet for client1socket (method in socket thread)

                    clientSocketObject.setMylist(mylist);           

                    clientSocketObject.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void summary() {
        // TODO Auto-generated method stub
        try {
            FileOutputStream summaryOut = new FileOutputStream("[" + peerName + "] summary.txt", false);
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < Client2.mylist.size(); i++) {
                stringBuilder.append(Client2.mylist.get(i) +1);
                stringBuilder.append(" ");
            }
            summaryOut.write(stringBuilder.toString().getBytes());
            summaryOut.flush();
            summaryOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }

    private void savePackets(int n, Object packet) {
        // TODO Auto-generated method stub
        try {


            FileOutputStream fos = new FileOutputStream(peerName + " packets/" +  Integer.toString(n+1), false);

            File f = (File) packet;
            FileInputStream fis = new FileInputStream(f);
            byte[] fileBytes = new byte[(int) f.length()];
            int bytesRead = 0;

            bytesRead = fis.read(fileBytes, 0,(int)  f.length());
            assert(bytesRead == fileBytes.length);
            assert(bytesRead == (int) f.length());
                

            fos.write(fileBytes);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Start();
    }

}
