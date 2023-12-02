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
        
        // Calculate the number of pieces in the file
        double result = (double)this.fileSize/this.pieceSize;
        this.numPieces = (int)Math.ceil(result);

        // Stores Common.cfg's data
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
        peerDirectory();
    }

    // Creates a subdirectory for each peer and saves the peer's files into the newly created folder.
    public void peerDirectory() {
        // Initialize fileOutputStream to null to ensure it can be closed safely in the 'finally' block.
        FileOutputStream fileOutputStream = null;

        // Creates a subDirectory for the peer with it's peerID
        String directoryPath = "./peer_info" + "/peer_" + Integer.toString(peerID);
        File directory = new File(directoryPath);

        // Check if the directory exists. If not, create it.
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Directory created: " + directoryPath);
            } else {
                System.out.println("Failed to create directory: " + directoryPath);
                return;
            }
        }
        try {
            // Create a file within the peer's directory with the specified fileName if the peer contains file.
            if(this.hasFile == 1){
                File filePath = new File(directory, fileName);
                fileOutputStream = new FileOutputStream(filePath);

                // Write data to the file.
                fileOutputStream.write(file);
            }
        } catch (FileNotFoundException fileNotFound) {
            System.out.println("FileNotFoundException caught file is either a directory or does not exist.");
            fileNotFound.printStackTrace();
        } catch (IOException ioException) {
            System.out.println("IOException caught from function createNewFile.");
            ioException.printStackTrace();
        } finally {
            // Ensure that the fileOutputStream is closed even if an exception occurs.
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException ioException) {
                    System.out.println("IOException caught from function flush or close");
                    ioException.printStackTrace();
                }
            }
        }
    }

    public void setManager(Hashtable<Integer, Peer> manager) {
        this.manager = manager;
        this.logger = new WritingLogger(manager.get(this.peerID));
    }

    public void sendMessage(byte [] message, ObjectOutputStream outputStream, int targetPeerId){
        try{
            outputStream.writeObject(message);
            outputStream.flush();
        }
        catch (Exception e){
            System.out.println("Exception while sending message.");
            e.printStackTrace();
        }
    }

    public void readDownloadedFile() {
        if (this.hasFile == 1) {
            try {
                // Read all bytes from the downloaded file and store them in a buffer
                byte[] allBytes = Files.readAllBytes(Paths.get("./peer_" + peerID + "/" + this.fileName));
                this.file = allBytes;
            } catch (IOException e) {
                System.out.println("IOException while reading downloaded file!");
                e.printStackTrace();
            }
        }
    }

    public static <K, V> K getKey(HashMap<K, V> map, V value) {
        return map.keySet()
                .stream()
                .filter(key -> value.equals(map.get(key)))
                .findAny().get();
    }

    public void preferredPeersSelection(List <Integer> interestedPeers, Instant start) throws IOException {
        HashMap<Integer, Double> selection = new HashMap<Integer, Double>();

        int[] preferredNeighbors = new int[numOfPreferredNeighbors];
        List<Integer> peersToChoke = new ArrayList<>();

        Instant finish = Instant.now();

        // Calculate the download rate for each neighbor in the manager
        manager.entrySet().stream()
            .filter(entry -> entry.getKey() != peerID)
            .forEach(entry -> {
                long timeElapsed = Duration.between(start, finish).toNanos();
                double downloadRate = ((double) entry.getValue().bytesDownloaded) / timeElapsed;
                selection.put(entry.getKey(), downloadRate);
        });

        this.bytesDownloaded = 0;

        // Sort the list of download rates
        Collection<Double> selectionValues = selection.values();
        List<Double> valueList = new ArrayList<Double>(selectionValues);
        Collections.sort(valueList);

        if (!interestedPeers.isEmpty()) {
            //If the number of interested peers is less than the preferred neighbor limit, just select all interested peers
            if (interestedPeers.size() <= preferredNeighbors.length) {
                for (int i = 0; i < interestedPeers.size(); i++) {
                    preferredNeighbors[i] = interestedPeers.get(i);
                }

                logger.preferredNeighborChange(peerID, preferredNeighbors);

                for (int preferredNeighbor : preferredNeighbors) {
                    // Check whether the preferredNeighbor ID is part of the unchoked peers
                    if (!unchokedPeers.contains(preferredNeighbor) && preferredNeighbor != 0) {
                        unchokedPeers.add(preferredNeighbor);

                        // Send the default unchoke message through the output
                        try {
                            ObjectOutputStream outputStream = null;
                            outputStream.writeObject(this.messageCreator.unchokeMessage());
                            outputStream.flush();
                        } catch (IOException e) {
                            System.out.println("IOException while sending unchoke message");
                            e.printStackTrace();
                        }

                    }
                }

                // Remove unwanted peers from the unchocked list
                for (int peer : unchokedPeers) {
                    boolean active = false;
                    for (int neighbor : preferredNeighbors) {
                        if (neighbor == peer) {
                            active = true;
                            break;
                        }
                    }

                    if (!active && peer != 0) {

                        // Send the default choke message to the recently removed peer
                        try {
                            ObjectOutputStream outputStream = null;
                            outputStream.writeObject(this.messageCreator.chokeMessage());
                            outputStream.flush();
                        } catch (IOException e) {
                            System.out.println("IOException while sending choke message");
                            e.printStackTrace();
                        }
                        // Add the peer back to be potentially chocked.
                        peersToChoke.add(peer);
                    }
                }

                for (int peer: peersToChoke) {
                    if (unchokedPeers.contains(peer)) {
                        unchokedPeers.remove(peer);
                    }
                }

            }
            else if (hasFile == 1){
                // Randomly select interested peers
                Collections.shuffle(interestedPeers, new Random());

                for (int i = 0; i < preferredNeighbors.length; i++) {
                    preferredNeighbors[i] = interestedPeers.get(i);
                }

                logger.preferredNeighborChange(peerID, preferredNeighbors);

                for (int preferredNeighbor : preferredNeighbors) {

                    // Check whether the preferredNeighbor ID is part of the unchoked peers
                    if (!unchokedPeers.contains(preferredNeighbor) && preferredNeighbor != 0) {
                        unchokedPeers.add(preferredNeighbor);

                        //Send unchoke message
                        try {
                            ObjectOutputStream outputStream = null;
                            outputStream.writeObject(this.messageCreator.unchokeMessage());
                            outputStream.flush();
                        } catch (IOException e) {
                            System.out.println("IOException while sending unchoke message");
                            e.printStackTrace();
                        }
                    }
                }

                // Remove unwanted peers from the unchocked list
                for (int peer: unchokedPeers) {
                    boolean active = false;
                    for (int neighbor : preferredNeighbors) {
                        if (neighbor == peer) {
                            active = true;
                            break;
                        }
                    }

                    if (!active && peer != 0) {

                        // Send the default choke message to the recently removed peer
                        try {
                            ObjectOutputStream outputStream = null;
                            outputStream.writeObject(this.messageCreator.chokeMessage());
                            outputStream.flush();
                        } catch (IOException e) {
                            System.out.println("IOException while sending unchoke message");
                            e.printStackTrace();
                        }
                        peersToChoke.add(peer);
                    }
                }

                for (int peer: peersToChoke) {
                    if (unchokedPeers.contains(peer)) {
                        unchokedPeers.remove(peer);
                    }
                }

            } else  {

                for (int i = 0; i < preferredNeighbors.length; i++){
                    if (interestedPeers.contains(getKey(selection, valueList.get(valueList.size()-1)))){
                        preferredNeighbors[i] = getKey(selection, valueList.get(valueList.size()-1));
                    }
                    valueList.remove(valueList.size()-1);
                }

                logger.preferredNeighborChange(peerID, preferredNeighbors);

                // Check whether the preferredNeighbor ID is part of the unchoked peers
                for (int preferredNeighbor : preferredNeighbors) {
                    if (!unchokedPeers.contains(preferredNeighbor) && preferredNeighbor != 0) {
                        unchokedPeers.add(preferredNeighbor);

                        // Send default unchoke message
                        try {
                            ObjectOutputStream outputStream = null;
                            outputStream.writeObject(this.messageCreator.unchokeMessage());
                            outputStream.flush();
                        } catch (IOException e) {
                            System.out.println("IOException while sending unchoke message");
                            e.printStackTrace();
                        }
                    }
                }

                // Remove unwanted peers from the unchocked list
                for (int peer: unchokedPeers) {
                    boolean active = false;
                    for (int neighbor: preferredNeighbors) {
                        if (neighbor == peer) {
                            active = true;
                            break;
                        }
                    }

                    if (!active && peer != 0) {
                        //Send default choke message 
                        try {
                            ObjectOutputStream outputStream = null;
                            outputStream.writeObject(this.messageCreator.chokeMessage());
                            outputStream.flush();
                        } catch (IOException e) {
                            System.out.println("IOException while sending unchoke message");
                            e.printStackTrace();
                        }
                        peersToChoke.add(peer);
                    }
                }

                for (int peer: peersToChoke) {
                    if (unchokedPeers.contains(peer)) {
                        unchokedPeers.remove(peer);
                    }
                }

            }
        }

    }

    public void startChokeThread() {
        Peer peer = this;
        final Instant[] start = {Instant.now()};

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        preferredPeersSelection(interestedPeers, start[0]);
                        start[0] = Instant.now();
                        Thread.sleep(unchokingInterval);
                    } catch (InterruptedException | IOException e) {
                        System.out.println("Thread to unchoke neighbor interrupted while trying to sleep.");
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    private void unchokeActivePeer(List<Integer> interestedPeers) throws IOException {
        List<Integer> selection = new ArrayList<>();
        for (int interestedPeerId : interestedPeers) {
            if (!unchokedPeers.contains(interestedPeerId)) {
                selection.add(interestedPeerId);
            }
        }

        if (!selection.isEmpty()) {
            Collections.shuffle(selection, new Random());
            int currPeerID = selection.get(0);
            logger.optimisticallyUnchockedChange(peerID, currPeerID);
            // Send default unchoke message
            try {
                ObjectOutputStream outputStream = null;
                outputStream.writeObject(this.messageCreator.unchokeMessage());
                outputStream.flush();
            } catch (IOException e) {
                System.out.println("IOException while sending unchoke message");
                e.printStackTrace();
            }

            this.optimisticUnchokedPeer = currPeerID;
        }
    }

    public void unchokePeer() {
        Peer peer = this;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // We are gonna have to check when all the peers have completed their download to stop this thread
                while(true) {
                    try {
                        peer.unchokeActivePeer(peer.interestedPeers);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(optimisticUnchokingInterval);
                    } catch (InterruptedException interruptedException) {
                        System.out.println("Thread to optimistically unchoke neighbor interrupted while trying to sleep.");
                        interruptedException.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
}
