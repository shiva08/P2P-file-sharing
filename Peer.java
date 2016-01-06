import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

/**
 * @author Shiva
 * 
 */
abstract class Peer {
    Peer() {}                                       // constructors
    public abstract void Start();
    public static String peerName = "";             //initiate
	public final  int packetSize  = 100*1024;       //100 KB
    public static ArrayList<Integer> packetIndex = new ArrayList<Integer>();
    public static HashMap<Integer, Object> packetList = new HashMap<Integer, Object>();
    public static ArrayList<Integer> mylist = new ArrayList<Integer>();
}
