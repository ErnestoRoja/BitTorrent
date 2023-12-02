package logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import peer.Peer;

public class WritingLogger {
    private Peer peer;
    private int peerID;
    private Logger messageLogger;

    public WritingLogger(Peer peer) {
        this.peer = peer;
        this.peerID = peer.peerID;
        this.messageLogger = Logger.getLogger(Integer.toString(peerID));

        FileHandler fileHandler;
        
        //Creating the log file directory for the peer
        try {  
            String workingDir = System.getProperty("user.dir");
            // This block configure the logger with handler and formatter  
            fileHandler = new FileHandler(workingDir+ "/log_peer_" + Integer.toString(peerID) + ".log");  
            messageLogger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();  
            fileHandler.setFormatter(formatter);  
        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } 
    }

    // Provides the current time of execution.
    public static String getCurrentTime(){
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss");
        return currentTime.format(formatter);
    }

    public void setVariables(int peerID, BitSet bitfield, String hostName, int listeningPort, int hasFile_, int numOfPreferredNeighbors, int unchokingInterval, int optimisticUnchokingInterval, String fileName, int fileSize, int pieceSize, int numPieces){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " has started its server and is setting variables: ");
        messageLogger.info("Peer bitfield: " + bitfield.toString());
        messageLogger.info("Peer hostname: "+ hostName);
        messageLogger.info("Peer port number: " + listeningPort);
        messageLogger.info("Peer contains file: "+ hasFile_);
        messageLogger.info("Peer numbOfPreferredNeighbors: " + numOfPreferredNeighbors);
        messageLogger.info("Peer unchoking interval: "+ unchokingInterval);
        messageLogger.info("Peer optimistically unchoking interval: " + optimisticUnchokingInterval);
        messageLogger.info("Peer download file name: "+ fileName);
        messageLogger.info("Peer file size: "+ fileSize);
        messageLogger.info("Peer piece size: "+ pieceSize);
        messageLogger.info("Peer number of pieces: "+ numPieces);
    }
    public void tcpConnect(int peerID, int peerID_2){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " makes a connection to Peer " + peerID_2 + ".");
    }

    public void connect(int peerID, int peerID_2){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " is connected from Peer " + peerID_2 + ".");
    }

    public void preferredNeighborChange(int peerID, int[] neighborsID){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " has the preferred neighbors " + Arrays.toString(neighborsID) + ".");
    }

    public void optimisticallyUnchockedChange(int peerID, int peerID_2){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " has the optimistically unchoked neighbor " + peerID_2 + ".");
    }

    public void unchoking(int peerID, int peerID_2){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " is unchoked by " + peerID_2 + ".");
    }

    public void choking(int peerID, int peerID_2){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " is choked by " + peerID_2 + ".");
    }

    public void receivingHave(int peerID, int peerID_2, int pieceIndex){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " received the ‘have’ message from " + peerID_2 + "for the piece" + pieceIndex + ".");
    }

    public void interested(int peerID, int peerID_2){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " received the ‘interested’ message from " + peerID_2 + ".");
    }

    public void notInterested(int peerID, int peerID_2){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " received the ‘not interested’ message from " + peerID_2 + ".");
    }

    public void downloading(int peerID, int peerID_2, int pieceIndex, int numPieces){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " has downloaded the piece " + pieceIndex + "from" + peerID_2 + ". Now the number of pieces it has is" + numPieces + ".");
    }

    public void downloaded(int peerID){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " has downloaded the complete file.");
    }

    public void handShake(int peerID, int targetPeerID){
        messageLogger.info(getCurrentTime() + ": Peer " + peerID + " has sent a handshake message to Peer " + targetPeerID);
    }

    public static void main(String[] args) {
    }
}
