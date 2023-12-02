import java.io.*;
import java.util.*;
import logger.WritingLogger;
import peer.Peer;
import server.Server;
import client.Client;

public class peerProcess {
    public static Hashtable<Integer, Peer> peers = new Hashtable<Integer, Peer>();
    static WritingLogger logger;

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
        System.out.println("Starting server thread for: " + peer.peerID);
        Server server = new Server(peer);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }

    public void setLogger(Peer peer) {
        logger = new WritingLogger(peer);
        int peerID = peer.peerID;

        logger.setVariables(
                peerID,
                peers.get(peerID).bitField,
                peers.get(peerID).hostName,
                peers.get(peerID).listeningPort,
                peers.get(peerID).hasFile,
                peers.get(peerID).numOfPreferredNeighbors,
                peers.get(peerID).unchokingInterval,
                peers.get(peerID).optimisticUnchokingInterval,
                peers.get(peerID).fileName,
                peers.get(peerID).fileSize,
                peers.get(peerID).pieceSize,
                peers.get(peerID).numPieces
        );
    }


    public static void main(String arg[]) throws FileNotFoundException {

        // Used to access the PeerInfo.cfg file from the project's 'Resource' folder
        peerProcess instance = new peerProcess();
        instance.parsePeerInfoConfig();

        int peerID = Integer.parseInt(arg[0]);
        peers.get(peerID).setManager(peers);
        peers.get(peerID).readDownloadedFile();

        instance.setLogger(peers.get(peerID));

        instance.startServer(peers.get(peerID));

        for (Map.Entry<Integer, Peer> entry : peers.entrySet()) {
            int currPeerID = entry.getKey();
            
            // Assuming that the peerID's are in increasing order within the config files
            if (currPeerID < peerID) {
                Client client = new Client(peers.get(peerID), entry.getValue());
                client.connect();
                logger.tcpConnect(peerID, currPeerID);
            }
        }
        // peers.get(peerID).startChokeThread();
        // peers.get(peerID).unchokePeer();
    }
}
