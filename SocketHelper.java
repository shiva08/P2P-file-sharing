import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

/**
 * @author Shiva
 * 
 */
abstract class SocketHelper extends Thread {

    protected Socket socket;
    protected ObjectOutputStream out;
    protected ObjectInputStream in;

    public String peerName = this.getName();

    protected HashMap<Integer, Object> currentPacketList;

    protected ArrayList<Integer> mylist;

    protected String filename  = "onwritingwell.pdf";

    public void setPacket(HashMap<Integer, Object> packet)
    {
        this.currentPacketList = packet;
    }

    public void setSocket(Socket socket) {

        this.socket = socket;
        System.out.println("[" + peerName + "] gets connected from port " + socket.getPort());
        try {
            in  = new ObjectInputStream(this.socket.getInputStream());
            out = new ObjectOutputStream(this.socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void setMylist(ArrayList<Integer> mylist){
        this.mylist = mylist;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public void send(Object message) throws Exception {
        out.writeObject(message);
        out.flush();
        out.reset();
    }

    public void send(int value) throws Exception {
        out.writeInt(value);
        out.flush();
        out.reset();
    }
}