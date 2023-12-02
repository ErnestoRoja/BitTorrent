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

    public void readBitfieldMessage(byte[] message) throws IOException { // 5

        byte[] messagePayload = new byte[message.length - 5];
        System.arraycopy(message, 5, messagePayload, 0, message.length - 5);

        BitSet receivedBitfield = BitSet.valueOf(message);

        peer.manager.get(targetPeerId).bitField = receivedBitfield;
        BitSet peerBitfield = peer.bitField;

        if (peerBitfield.equals(receivedBitfield)) {
            byte[] notInterestedMessage = creator.notInterestedMessage();
            peer.sendMessage(notInterestedMessage, outputStream, targetPeerId);
        } else if (peerBitfield.isEmpty() && !receivedBitfield.isEmpty()) {
            BitSet interestingPiece = (BitSet) peerBitfield.clone();
            interestingPiece.or(receivedBitfield);

            if (peer.interestingPieces.containsKey(peer.peerID)) {
                if (interestingPiece.isEmpty()) {
                    peer.interestingPieces.remove(peer.peerID);
                } else {
                    peer.interestingPieces.replace(peer.peerID, peer.interestingPieces.get(peer.peerID),
                            interestingPiece);
                }
            } else {
                peer.interestingPieces.put(peer.peerID, interestingPiece);
            }

            byte[] interestedMessage = creator.interestedMessage();
            peer.sendMessage(interestedMessage, outputStream, targetPeerId);

        } else if (peerBitfield.isEmpty() && receivedBitfield.isEmpty()) {
            byte[] notInterestedMessage = creator.notInterestedMessage();
            peer.sendMessage(notInterestedMessage, outputStream, targetPeerId);
        } else {
            BitSet interestingPiece = (BitSet) peerBitfield.clone();
            interestingPiece.or(receivedBitfield);

            interestingPiece.xor(peer.bitField);

            if (peer.interestingPieces.containsKey(peer.peerID)) {
                if (interestingPiece.isEmpty()) {
                    peer.interestingPieces.remove(peer.peerID);
                } else {
                    peer.interestingPieces.replace(peer.peerID, peer.interestingPieces.get(peer.peerID),
                            interestingPiece);
                }
            } else {
                peer.interestingPieces.put(peer.peerID, interestingPiece);
            }

            if (interestingPiece.isEmpty()) {
                byte[] notInterestedMessage = creator.notInterestedMessage();
                peer.sendMessage(notInterestedMessage, outputStream, targetPeerId);
            } else {
                byte[] interestedMessage = creator.interestedMessage();
                peer.sendMessage(interestedMessage, outputStream, targetPeerId);

            }
        }
        // peer.bitField = BitSet.valueOf(bitfield);
        // logger.receivingBitfield(peer.peerID, targetPeerId, peer.bitField);
    }

    public void readRequestMessage(byte[] message) throws IOException { // 6
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 5, 9)).order(ByteOrder.BIG_ENDIAN).getInt();
        byte[] data = peer.file.clone();

        // peer.file[pieceIndex].clone();
        byte[] pieceMessage = creator.pieceMessage(pieceIndex, data);

        peer.sendMessage(pieceMessage, outputStream, targetPeerId);

        // byte [] requestMessage = creator.requestMessage(pieceIndex);
        // peer.sendMessage(requestMessage, outputStream, targetPeerId);
    }

    public void readPieceMessage(byte[] message) throws IOException { // 7
        int messagePayloadLength = message.length - 5;
        byte[] pieceIndex = new byte[4];
        byte[] piece = new byte[messagePayloadLength - 4];
        System.arraycopy(message, 5, pieceIndex, 0, pieceIndex.length);
        System.arraycopy(message, 9, piece, 0, piece.length);

        int pieceIndexInt = ByteBuffer.wrap(pieceIndex).getInt();

        for (int i = 0; i < piece.length; i++) {
            peer.file[pieceIndexInt + i] = piece[i];
        }

        // peer.file[pieceIndexInt] = piece;

        peer.piecesDownloaded++;

        logger.downloading(peer.peerID, targetPeerId, pieceIndexInt, peer.piecesDownloaded);
        // Set bitfield to indicate we now have this piece ( we will not request this
        // piece)
        peer.manager.get(targetPeerId).bytesDownloaded += piece.length;

        if (peer.bitField.nextClearBit(0) == peer.numPieces) {
            peer.hasFile = 1;
        }

        peer.manager.forEach((k, v) -> {
            if (k != peer.peerID) {
                try {
                    // logger.sentHaveMessage(peer.peerID, k, pieceIndexInt);
                    peer.sendMessage(creator.haveMessage(pieceIndexInt), outputStream, k);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if (peer.hasFile == 0) {

            BitSet validPieces = (BitSet) peer.bitField.clone();
            validPieces.flip(0, peer.numPieces);
            validPieces.and(peer.interestingPieces.get(targetPeerId));

            // System.out.println(validPieces);

            List<Integer> validPieceIndex = new ArrayList<>();

            for (int i = 0; i < validPieces.length(); i++) {
                if (validPieces.get(i) == true)
                    validPieceIndex.add(i);
            }

            Collections.shuffle(validPieceIndex, new Random());

            int requestPiece = validPieceIndex.get(0);

            /*
             * Test Print Statement:
             * System.out.println("Requesting Piece after receiving piece request piece is "
             * + requestPiece);
             */
            // logger.requestedPieceFrom(peer.peerID, remotePeerId, requestPiece);
            // Create and send request message for the piece we want
            peer.sendMessage(creator.requestMessage(requestPiece), outputStream, targetPeerId);
        } else {
            // logger.finishedDownloadComplete(peer.peerID);
            peer.saveFile();
        }

        // logger.downloading(pieceIndex, pieceIndex, pieceIndex, pieceIndex);
    }

    /*
     * steps
     * 
     * 1 - send handshake message
     * 
     * 2 - if peer does NOT have file and neighbors do NOT have file -> exit
     * else, listen
     * 
     * 3 - read message from input stream
     * 4 - switch case message type + handle
     * 
     * 
     */

    public void readHandshakeMessage(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        byte[] header = new byte[18];
        buffer.get(header, 0, 18);

        String handShakeString = new String(header, StandardCharsets.UTF_8);

        if (handShakeString.equals("P2PFILESHARINGPROJ")) {
            byte[] peerId = new byte[4];
            System.arraycopy(message, 28, peerId, 0, 4);
            int peerIdInt = ByteBuffer.wrap(peerId).getInt();

            /*
             * Test Print Statement:
             * System.out.println("Peer " + peer.peerID +
             * " received the handshake message from Peer " + peerIdInt);
             */

            targetPeerId = peerIdInt;
            // logger.connectedFromPeer(peer.peerID, targetPeerId);
            logger.handShake(peer.peerID, targetPeerId);

            try {
                peer.sendMessage(creator.bitfieldMessage(peer.bitField), outputStream, targetPeerId);
            } catch (IOException e) {
                System.out.println(e);
            }

            peer.manager.get(targetPeerId).outputStream = outputStream;

        }
    }

    // Still needs further implementation.
    public void readMessage(byte[] message) throws IOException {
        int messageType = message[4];
        System.out.println("Message Type: " + messageType);

        switch (messageType) {
            case 0: // choke
                System.out.println("Choke Message");
                logger.choking(peer.peerID, targetPeerId);
            case 1: // unchoke
                System.out.println("Unchoke Message");
                logger.unchoking(peer.peerID, targetPeerId);
                break;
            case 2: // interested
                System.out.println("Interested Message");
                logger.interested(peer.peerID, peer.peerID);
                break;
            case 3: // not interested
                System.out.println("Not interested Message");
                logger.notInterested(peer.peerID, peer.peerID);
                break;
            case 4: // have
                System.out.println("Have Message");
                logger.receivingHave(messageType, messageType, messageType);
                break;
            case 5: // bitfield
                System.out.println("Bitfield Message");
                // logger.bi
                readBitfieldMessage(message);
                break;
            case 6: // request
                System.out.println("Request Message");
                readRequestMessage(message);
                break;
            case 7: // piece
                System.out.println("Piece Message");
                readPieceMessage(message);
                break;
            default: // handshake message
                System.out.println("Handshake Message");
                readHandshakeMessage(message);

        }
    }

    public void run() {
        // executed when thread.start() is run
        // peer send handshake message to target
        try {
            byte[] handshakeMessage = new byte[0];
            handshakeMessage = creator.handshakeMessage(peer.peerID);
            peer.sendMessage(handshakeMessage, outputStream, targetPeerId);
        } catch (Exception e) {
            System.out.println(e);
        }

        // if peer + neightbors DO NOT have file -> System.exit(0)

        while (true) {
            if (peer.hasFile == 1) { // add check for neighbors having file
                System.exit(0);
            }
            try {

                byte[] receivedMessage = (byte[]) inputStream.readObject();
                readMessage(receivedMessage);

            } catch (Exception e) {
                System.out.println(e);
            }
        }

    }
}
