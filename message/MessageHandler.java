package message;

import java.io.*;
import java.net.*;
import peer.Peer;

//import main.java.com.bittorrent.message.MessageCreator;
// have compile message creator first

public class MessageHandler implements Runnable{
    private Peer peer;
    private int targetPeerId;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private MessageCreator creator;
    // private WritingLog logger;


    public MessageHandler(ObjectInputStream inputStream, ObjectOutputStream outputStream, Peer peer, Socket socket){
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.peer = peer;
        this.socket = socket;
        creator = new MessageCreator();
        //logger = new WritingLog(peer);
    }

    /* steps

        1 - send handshake message

        2 - if peer does NOT have file and neighbors do NOT have file -> exit
            else, listen

        3 - read message from input stream
        4 - switch case message type + handle


    */

    // Still needs further implementation.
    public static void readMessage(byte[] message) throws IOException {
        int messageType = message[4];
        System.out.println("Message Type: " + messageType);


        switch (messageType) {
            case 0: // choke
                System.out.println("Choke Message");
                break;
            case 1: // unchoke
                System.out.println("Unchoke Message");
                break;
            case 2: // interested
                System.out.println("Interested Message");
                break;
            case 3: // not interested
                System.out.println("Not interested Message");
                break;
            case 4: // have
                System.out.println("Have Message");
                break;
            case 5: // bitfield
                System.out.println("Bitfield Message");
                break;
            case 6: // request
                System.out.println("Request Message");
                break;
            case 7: // piece
                System.out.println("Piece Message");
                break;
            default: // handshake message
                System.out.println("default");

        }


//        byte[] lengthBytes = new byte[4];
//
//        //byte [] message = (byte[]) inputStream.readObject();
//
//        //inputStream.read(lengthBytes);
//        int messageLength = ByteBuffer.wrap(lengthBytes).getInt();
//
//        byte messageType = (byte) inputStream.read();
//
//        byte[] payload = new byte[messageLength - 5];
//        inputStream.read(payload);
//
//        /*
//         depending on the payload
//         for example, if it is a bitfield message, then the payload will be the bitfield
//         send the rest of the message to the bitfield handler
//         if it is a request message, then the payload will be the index of the piece requested
//        */
//
//        // This tests the content of the message
//        byte[] message = new byte[messageLength];
//        System.arraycopy(lengthBytes, 0, message, 0, 4);
//        message[4] = messageType;
//        System.arraycopy(payload, 0, message, 5, payload.length);
//
//        return message;
    }

    public void run(){
        // executed when thread.start() is run
        // peer send handshake message to target
        // if peer + neightbors DO NOT have file -> System.exit(0)
        try{
            byte [] receivedMessage = (byte[])inputStream.readObject();

            readMessage(receivedMessage);

        }
        catch (Exception e){
            System.out.println(e);
        }

    }
}
