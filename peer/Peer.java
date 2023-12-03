package peer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import message.messageFactory;
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
    public ObjectOutputStream outputStream;

    // General attributes
    public Hashtable<Integer, Peer> manager;
    public BitSet bitField;
    public Hashtable<Integer, BitSet> interestingPieces;
    public ArrayList<Integer> interestedPeers;
    public List<Integer> unchokedPeers;
    public messageFactory messageCreator;
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
        this.messageCreator = new messageFactory();
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

    public boolean checkNeighborFiles() {
        for (int curr : manager.keySet()) {
            if (manager.get(curr).bitField.nextClearBit(0) != numPieces) {
                return false;
            }
        }

        return true;
    }

    // Creates a subdirectory for each peer and saves the peer's files into the newly created folder.
    public void peerDirectory() {
        // Initialize fileOutputStream to null to ensure it can be closed safely in the 'finally' block.
        FileOutputStream fileOutputStream = null;

        // Creates a subDirectory for the peer with it's peerID
        String workingDir = System.getProperty("user.dir");
        String directoryPath = workingDir + "/peer_" + Integer.toString(peerID);
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

    private HashMap<Integer, Double> calculateDownloadRates(Instant start) {
        Instant finish = Instant.now();
        HashMap<Integer, Double> selection = new HashMap<>();
    
        manager.entrySet().stream()
                .filter(entry -> entry.getKey() != peerID)
                .forEach(entry -> {
                    long timeElapsed = Duration.between(start, finish).toNanos();
                    double downloadRate = ((double) entry.getValue().bytesDownloaded) / timeElapsed;
                    selection.put(entry.getKey(), downloadRate);
                });
    
        this.bytesDownloaded = 0;
        return selection;
    }

    private List<Double> sortDownloadRates(HashMap<Integer, Double> selection) {
        Collection<Double> selectionValues = selection.values();
        List<Double> valueList = new ArrayList<>(selectionValues);
        Collections.sort(valueList);
        return valueList;
    }

    private void sendUnchokeMessage() throws IOException {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new ByteArrayOutputStream())) {
            outputStream.writeObject(this.messageCreator.unchokeMessage());
            outputStream.flush();
        }
    }

    private void sendChokeMessage(int peer) throws IOException {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new ByteArrayOutputStream())) {
            outputStream.writeObject(this.messageCreator.chokeMessage());
            outputStream.flush();
        }
    }

    private void selectPreferredNeighbors(int[] preferredNeighbors, List<Integer> interestedPeers, List<Double> valueList, HashMap<Integer, Double> selection) {
        if (interestedPeers.size() <= preferredNeighbors.length) {
            for (int i = 0; i < interestedPeers.size(); i++) {
                preferredNeighbors[i] = interestedPeers.get(i);
            }
    
            logger.preferredNeighborChange(peerID, preferredNeighbors);
        } else if (hasFile == 1) {
            Collections.shuffle(interestedPeers, new Random());
    
            for (int i = 0; i < preferredNeighbors.length; i++) {
                preferredNeighbors[i] = interestedPeers.get(i);
            }
    
            logger.preferredNeighborChange(peerID, preferredNeighbors);
        } else {
            for (int i = 0; i < preferredNeighbors.length; i++) {
                if (interestedPeers.contains(getKey(selection, valueList.get(valueList.size() - 1)))) {
                    preferredNeighbors[i] = getKey(selection, valueList.get(valueList.size() - 1));
                }
                valueList.remove(valueList.size() - 1);
            }
    
            logger.preferredNeighborChange(peerID, preferredNeighbors);
        }
    }

    private void updateAndSendUnchokeMessages(int[] preferredNeighbors, List<Integer> peersToChoke) throws IOException {
        updateUnchokedPeers(preferredNeighbors);
        processChokedPeers(preferredNeighbors, peersToChoke);
    }
    
    private void updateUnchokedPeers(int[] preferredNeighbors) throws IOException {
        for (int preferredNeighbor : preferredNeighbors) {
            if (!unchokedPeers.contains(preferredNeighbor) && preferredNeighbor != 0) {
                unchokedPeers.add(preferredNeighbor);
                sendUnchokeMessage();
            }
        }
    }
    
    private void processChokedPeers(int[] preferredNeighbors, List<Integer> peersToChoke) throws IOException {
        for (int peer : unchokedPeers) {
            if (!isActivePeer(peer, preferredNeighbors) && peer != 0) {
                sendChokeMessage(peer);
                peersToChoke.add(peer);
            }
        }
    }
    
    private boolean isActivePeer(int peer, int[] preferredNeighbors) {
        for (int neighbor : preferredNeighbors) {
            if (neighbor == peer) {
                return true;
            }
        }
        return false;
    }

    private void removeUnwantedPeers(List<Integer> peersToChoke) {
        for (int peer : peersToChoke) {
            if (unchokedPeers.contains(peer)) {
                unchokedPeers.remove(peer);
            }
        }
    }

    public void preferredPeersSelection(List<Integer> interestedPeers, Instant start) throws IOException {
        HashMap<Integer, Double> selection = calculateDownloadRates(start);
    
        int[] preferredNeighbors = new int[numOfPreferredNeighbors];
        List<Integer> peersToChoke = new ArrayList<>();
    
        List<Double> sortedSelection = sortDownloadRates(selection);
    
        if (!interestedPeers.isEmpty()) {
            selectPreferredNeighbors(preferredNeighbors, interestedPeers, sortedSelection, selection);
            updateAndSendUnchokeMessages(preferredNeighbors, peersToChoke);
            removeUnwantedPeers(peersToChoke);
        }
    }

    public void startChokeThread() {
        final Instant[] start = {Instant.now()};

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        preferredPeersSelection(interestedPeers, start[0]);
                        start[0] = Instant.now();
                        Thread.sleep(unchokingInterval);
                    } catch (Exception e) {
                        System.out.println("Exception caught while trying to start choke thread.");
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
            //sendMessage(this.messageCreator.unchokeMessage(), outputStream, currPeerID);
            
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
                while (true) {
                    try {
                        peer.unchokeActivePeer(peer.interestedPeers);
                    } catch (IOException e) {
                        System.out.println("IOException caught while trying to unchoke active peer in thread.");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(optimisticUnchokingInterval);
                    } catch (Exception e) {
                        System.out.println("Exception caught while unchoke thread is sleeping.");
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void saveFile(){
        FileOutputStream fileOutputStream = null;
        try {
            File dir = new File("./peer_"+ peerID);
            dir.mkdirs();
            File fileLocation = new File(dir, fileName);
            fileLocation.createNewFile();
            fileOutputStream = new FileOutputStream(fileLocation);

            for(int i = 0; i<numPieces; i++){
                fileOutputStream.write(file[i]);
            }
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("FileNotFoundException while saving downloaded file to disk.");
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            System.out.println("IOException while writing pieces to file.");
            ioException.printStackTrace();
        } finally {
            if(fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch(IOException ioException) {
                    System.out.println("IOException while closing file output stream");
                    ioException.printStackTrace();
                }
            }
        }
    }

    // public void updateDownloadedByte(int bytes){
    //     this.bytesDownloaded += bytes;
    // }

    // public void updatePieceNumDownloaded(){
    //     this.piecesDownloaded++;
    // }

}
