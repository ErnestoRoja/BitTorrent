import java.io.*;
import java.util.*;
import logger.WritingLogger;
import peer.Peer;
import server.Server;
import client.Client;

public class peerProcess {
    public static Hashtable<Integer, Peer> peers = new Hashtable<Integer, Peer>();

    public void parsePeerInfoConfig() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("./resources/PeerInfo.cfg");

        if (inputStream != null) {
            Scanner scanner = new Scanner(inputStream);
            while (scanner.hasNextLine()) {
                String currLine = scanner.nextLine();
                //System.out.println(currLine);
                String[] parameters = currLine.split(" ");
                Peer peer = new Peer();
                peer.parseCommonConfig();
                peer.parsePeerInfoConfig(Integer.parseInt(parameters[0]), parameters[1], Integer.parseInt(parameters[2]), Integer.parseInt(parameters[3]));
                System.out.println("PeerID: " + peer.peerID + ", is being added to the peers hashtable");
                peers.put(peer.peerID, peer);
            }
            scanner.close();
        } else {
            throw new NullPointerException("inputStream is NULL!");
        }
    }

    public void startServer(Peer peer) {
        System.out.println("Starting server thread for:" + peer.peerID);
        Server server = new Server(peer);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }


    public static void main(String arg[]) throws FileNotFoundException {

        // Used to access the PeerInfo.cfg file from the project's 'Resource' folder
        peerProcess instance = new peerProcess();
        instance.parsePeerInfoConfig();

        int peerID = Integer.parseInt(arg[0]);

        instance.startServer(peers.get(peerID));
        
        


        
    }
}
