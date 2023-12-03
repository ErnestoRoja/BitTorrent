package message;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import peer.Peer;
import logger.WritingLogger;
//import main.java.com.bittorrent.message.MessageCreator;
// have compile message creator first

public class messageManager implements Runnable {
    private Peer peer;
    private int targetPeerId;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private messageFactory creator;
    private WritingLogger logger;

    public messageManager(ObjectInputStream inputStream, ObjectOutputStream outputStream, Peer peer, Socket socket) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.peer = peer;
        this.socket = socket;
        creator = new messageFactory();
        logger = new WritingLogger(peer);
    }

    public void setManagerPeerID(int id) {
        this.targetPeerId = id;
    }

    public void run() {

        try {
            byte[] handshakeMessage = new byte[0];
            handshakeMessage = creator.handshakeMessage(peer.peerID);
            peer.sendMessage(handshakeMessage, outputStream);
        } catch (Exception e) {
        }

        while (true) {
            if (peer.hasFile == 1 && peer.checkNeighborFiles()) { // add check for neighbors having file
                System.exit(0);
            }
            try {
                byte[] receivedMessage = (byte[]) inputStream.readObject();
                readMessage(receivedMessage);

            } catch (Exception e) {
            }
        }
    }

    public void readHaveMessage(byte[] message) { // 4
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 5, 9)).order(ByteOrder.BIG_ENDIAN).getInt();

        logger.receivingHave(peer.peerID, targetPeerId, pieceIndex);

        peer.bitField.set(pieceIndex, true);
        if (peer.bitField.nextClearBit(0) == peer.numPieces) {
            peer.hasFile = 1;
        }

        BitSet updatedBitfield = peer.manager.get(targetPeerId).bitField;

        if (peer.bitField.equals(updatedBitfield)) {

            try {
                byte[] notInterestedMessage = creator.notInterestedMessage();
                peer.sendMessage(notInterestedMessage, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (peer.bitField.isEmpty() && !updatedBitfield.isEmpty()) {

            BitSet interestingPieces = (BitSet) peer.bitField.clone();
            interestingPieces.or(updatedBitfield);

            if (peer.interestingPieces.containsKey(targetPeerId)) {
                if (interestingPieces.isEmpty()) {
                    peer.interestingPieces.remove(targetPeerId);
                } else {
                    peer.interestingPieces.replace(targetPeerId, peer.interestingPieces.get(targetPeerId),
                            interestingPieces);
                }
            } else {
                peer.interestingPieces.put(targetPeerId, interestingPieces);
            }

            try {
                byte[] interestedMessage = creator.interestedMessage();
                peer.sendMessage(interestedMessage, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (peer.bitField.isEmpty() && updatedBitfield.isEmpty()) {
            try {
                byte[] notInterestedMessage = creator.notInterestedMessage();
                peer.sendMessage(notInterestedMessage, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            BitSet interestingPieces = (BitSet) peer.bitField.clone();
            interestingPieces.or(updatedBitfield);

            interestingPieces.xor(peer.bitField);

            if (peer.interestingPieces.containsKey(targetPeerId)) {
                if (interestingPieces.isEmpty()) {
                    peer.interestingPieces.remove(targetPeerId);
                } else {
                    peer.interestingPieces.replace(targetPeerId, peer.interestingPieces.get(targetPeerId),
                            interestingPieces);
                }
            } else {
                peer.interestingPieces.put(targetPeerId, interestingPieces);
            }

            try {
                if (interestingPieces.isEmpty()) {
                    byte[] notInterestedMessage = creator.notInterestedMessage();
                    peer.sendMessage(notInterestedMessage, outputStream);
                } else {
                    byte[] interestedMessage = creator.interestedMessage();
                    peer.sendMessage(interestedMessage, outputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void readBitfieldMessage(byte[] message) throws IOException { // 5

        byte[] messagePayload = new byte[message.length - 5];
        System.arraycopy(message, 5, messagePayload, 0, message.length - 5);

        BitSet receivedBitfield = BitSet.valueOf(message);

        peer.manager.get(targetPeerId).bitField = receivedBitfield;
        BitSet peerBitfield = peer.bitField;

        if (peerBitfield.equals(receivedBitfield)) {
            byte[] notInterestedMessage = creator.notInterestedMessage();
            peer.sendMessage(notInterestedMessage, outputStream);
        } else if (peerBitfield.isEmpty() && !receivedBitfield.isEmpty()) {
            BitSet interestingPieces = (BitSet) peerBitfield.clone();
            interestingPieces.or(receivedBitfield);

            if (peer.interestingPieces.containsKey(targetPeerId)) {
                if (interestingPieces.isEmpty()) {
                    peer.interestingPieces.remove(targetPeerId);
                } else {
                    peer.interestingPieces.replace(targetPeerId, peer.interestingPieces.get(targetPeerId),
                            interestingPieces);
                }
            } else {
                peer.interestingPieces.put(targetPeerId, interestingPieces);
            }

            byte[] interestedMessage = creator.interestedMessage();
            peer.sendMessage(interestedMessage, outputStream);

        } else if (peerBitfield.isEmpty() && receivedBitfield.isEmpty()) {
            byte[] notInterestedMessage = creator.notInterestedMessage();
            peer.sendMessage(notInterestedMessage, outputStream);
        } else {
            BitSet interestingPieces = (BitSet) peerBitfield.clone();

            interestingPieces.or(receivedBitfield);
            interestingPieces.xor(peer.bitField);

            if (peer.interestingPieces.containsKey(targetPeerId)) {
                if (interestingPieces.isEmpty()) {
                    peer.interestingPieces.remove(targetPeerId);
                } else {
                    peer.interestingPieces.replace(targetPeerId, peer.interestingPieces.get(targetPeerId),
                            interestingPieces);
                }
            } else {
                peer.interestingPieces.put(targetPeerId, interestingPieces);
            }

            if (interestingPieces.isEmpty()) {
                byte[] notInterestedMessage = creator.notInterestedMessage();
                peer.sendMessage(notInterestedMessage, outputStream);
            } else {
                byte[] interestedMessage = creator.interestedMessage();
                peer.sendMessage(interestedMessage, outputStream);

            }
        }

    }

    public void readRequestMessage(byte[] message) throws IOException { // 6
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 5, 9)).order(ByteOrder.BIG_ENDIAN).getInt();
        byte[] data = peer.file.clone();

        byte[] pieceMessage = creator.pieceMessage(pieceIndex, data);

        peer.sendMessage(pieceMessage, outputStream);

    }

    public void readPieceMessage(byte[] message) throws IOException { // 7
        int messagePayloadLength = message.length - 5;
        byte[] pieceIndex = new byte[4];
        byte[] piece = new byte[messagePayloadLength - 4];
        System.arraycopy(message, 5, pieceIndex, 0, pieceIndex.length);
        System.arraycopy(message, 9, piece, 0, piece.length);

        int pieceIndexInt = ByteBuffer.wrap(pieceIndex).getInt();

        if (peer.pieceInfo.containsKey(pieceIndexInt)) {
            return;
        }

        peer.pieceInfo.put(pieceIndexInt, piece);

        peer.piecesDownloaded++;

        logger.downloading(peer.peerID, targetPeerId, pieceIndexInt, peer.piecesDownloaded);

        peer.manager.get(targetPeerId).bytesDownloaded += piece.length;

        peer.bitField.set(pieceIndexInt, true);
        if (peer.bitField.nextClearBit(0) == peer.numPieces) {
            peer.hasFile = 1;
        }

        peer.manager.forEach((i, j) -> {
            if (i != peer.peerID) {
                try {
                    peer.sendMessage(creator.haveMessage(pieceIndexInt), outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if (peer.hasFile == 0) {

            BitSet validPieces = (BitSet) peer.bitField.clone();
            validPieces.flip(0, peer.numPieces);
            validPieces.and(peer.interestingPieces.get(targetPeerId));

            List<Integer> validPieceIndex = new ArrayList<>();

            for (int i = 0; i < validPieces.length(); i++) {
                if (validPieces.get(i) == true) {
                    validPieceIndex.add(i);
                }
            }

            Collections.shuffle(validPieceIndex, new Random());

            int requestPiece = validPieceIndex.get(0);

            peer.sendMessage(creator.requestMessage(requestPiece), outputStream);

        } else {
            logger.downloaded(peer.peerID);
            peer.saveFile();
        }

    }

    public void readUnchokeMessage(byte[] message) throws IOException { // 1
        if (peer.hasFile == 0) {
            BitSet valid = (BitSet) peer.bitField.clone();
            valid.flip(0, peer.numPieces);

            valid.and(peer.interestingPieces.get(targetPeerId));
            List<Integer> validPieceIndex = new ArrayList<>();
            for (int i = 0; i < valid.length(); i++) {
                if (valid.get(i) == true) {
                    validPieceIndex.add(i);
                }
            }
            Collections.shuffle(validPieceIndex, new Random());

            int requestPiece = validPieceIndex.get(0);
            byte[] requestMessage = creator.requestMessage(requestPiece);
            peer.sendMessage(requestMessage, outputStream);
        }

    }

    public void readHandshakeMessage(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        byte[] header = new byte[18];
        buffer.get(header, 0, 18);

        String handShakeHeader = new String(header, StandardCharsets.UTF_8);

        String handShakeString = new String(message, StandardCharsets.UTF_8);

        int targetId = Integer.parseInt(handShakeString.substring(handShakeString.length() - 6).trim());
        setManagerPeerID(targetId);

        if (handShakeHeader.equals("P2PFILESHARINGPROJ")) {
            byte[] peerId = new byte[4];
            System.arraycopy(message, 28, peerId, 0, 4);

            logger.handShake(peer.peerID, targetPeerId);
            try {
                peer.sendMessage(creator.bitfieldMessage(peer.bitField), outputStream);
            } catch (IOException e) {
            }

            peer.manager.get(targetPeerId).outputStream = outputStream;

        }
    }

    public void readMessage(byte[] message) throws IOException {

        int messageType = message[4];

        switch (messageType) {
            case 0: // choke
                logger.choking(peer.peerID, targetPeerId);
                break;
            case 1: // unchoke
                logger.unchoking(peer.peerID, targetPeerId);
                readUnchokeMessage(message);
                break;
            case 2: // interested
                logger.interested(peer.peerID, targetPeerId);
                peer.interestedPeers.add(targetPeerId);
                break;
            case 3: // not interested
                logger.notInterested(peer.peerID, targetPeerId);

                int notInterestedIndex = peer.interestedPeers.indexOf(targetPeerId);
                if (notInterestedIndex != -1) {
                    peer.interestedPeers.remove(notInterestedIndex);
                }
                break;
            case 4: // have
                readHaveMessage(message);
                logger.receivingHave(peer.peerID, targetPeerId, messageType);
                break;
            case 5: // bitfield
                readBitfieldMessage(message);
                break;
            case 6: // request
                readRequestMessage(message);
                break;
            case 7: // piece
                readPieceMessage(message);
                break;
            default: // handshake message
                readHandshakeMessage(message);

        }
    }
}
