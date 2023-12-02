package peer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import message.MessageCreator;
import logger.WritingLogger;

public class Peer {

    // Attributes from the 'Common.cfg' file
    public int numOfPreferredNeighbors;
    public int unchokingInterval;
    public int optimisticUnchokingInterval;
    public String fileName;
    public int fileSize;
    public int pieceSize;
    public int numPieces;

    // Attributes from the 'PeerInfo.cfg' file
    public int peerID;
    public String hostName;
    public int listeningPort;
    public int hasFile;
    public byte[] file;

    // General attributes
    public Hashtable<Integer, Peer> manager;
    public BitSet bitField;
    public Hashtable<Integer, BitSet> interestingPieces;
    public ArrayList<Integer> interestedPeers;
    public List<Integer> unchokedPeers;
    public MessageCreator messageCreator;
    public int bytesDownloaded;
    public int piecesDownloaded;
    public WritingLogger logger;
    public double downloadRate;
    public int optimisticUnchokedPeer;

    public Peer() {

    }

    public void parseCommonConfig() {
        Properties prop = new Properties();

        // Used to locate the config file within the 'resources' folder provided by Maven
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream("./resources/Common.cfg");

        if (input == null) {
            throw new IllegalArgumentException("Common.cfg file not found!");
        }

        // Processes the config file and creates a list of all available properties found within the file.
        try {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Configures the peers with all the available configs.
        this.numOfPreferredNeighbors = Integer.parseInt(prop.getProperty("NumberOfPreferredNeighbors"));
        System.out.println("\nNumberOfPreferredNeighbors " + this.numOfPreferredNeighbors);
        this.unchokingInterval = Integer.parseInt(prop.getProperty("UnchokingInterval"));
        System.out.println("UnchokingInterval " + this.unchokingInterval);
        this.optimisticUnchokingInterval = Integer.parseInt(prop.getProperty("OptimisticUnchokingInterval"));
        System.out.println("OptimisticUnchokingInterval " + this.optimisticUnchokingInterval);
        this.fileName = prop.getProperty("FileName");
        System.out.println("FileName " + this.fileName);
        this.fileSize = Integer.parseInt(prop.getProperty("FileSize"));
        System.out.println("FileSize " + this.fileSize);
        this.pieceSize = Integer.parseInt(prop.getProperty("PieceSize"));
        System.out.println("PieceSize " + this.pieceSize);
        
        //Here we calculate the number of pieces in the file
        double result = (double)this.fileSize/this.pieceSize;
        this.numPieces = (int)Math.ceil(result);

        //Storing the data of the file in Common.cfg
        InputStream input2 = classLoader.getResourceAsStream("./resources/" + fileName);
        try {
            this.file = input2.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void parsePeerInfoConfig(int peerID, String host, int port, int hasFile_) {
        this.peerID = peerID;
        this.hostName = host;
        this.listeningPort = port;
        this.hasFile = hasFile_;
        this.manager = new Hashtable<Integer, Peer>();
        this.interestingPieces = new Hashtable<Integer, BitSet>();
        this.interestedPeers = new ArrayList<>();
        this.bitField = new BitSet(numPieces);
        this.messageCreator = new MessageCreator();
        this.bytesDownloaded = 0;
        this.piecesDownloaded = 0;

        if (hasFile_ == 1) {
            this.bitField.set(0, numPieces, true);
            this.piecesDownloaded = numPieces;
        } else {
            this.bitField.clear(0, numPieces);
        }

        file = new byte[numPieces];
    }
}
