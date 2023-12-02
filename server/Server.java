package server;

import java.io.*;
import java.net.*;
import peer.Peer;
import message.MessageHandler;

public class Server implements Runnable {

    // creates message handler

    // wait for handshake message
    // starts handler on thread
    private Peer peer;
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public Server(Peer peer){
        this.peer = peer;
    }

    public void run(){
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(peer.listeningPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while(true){
            try {
                socket = listener.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //create message handler this will handle dealing with incoming messages as well as sending responses to messages
            MessageHandler handler = new MessageHandler(inputStream, outputStream, peer, socket); //(assuming we want peer and socket maybe not needed?)
//                peer.setOut(out);

            //start handler on thread
            Thread serverThread = new Thread(handler);
            serverThread.start();
        }

    }


}
