package server;

import java.io.*;
import java.net.*;
import peer.Peer;
import message.messageManager;

public class Server implements Runnable {

    // creates message handler

    // wait for handshake message
    // starts handler on thread
    private Peer peer;
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public Server(Peer peer) {
        this.peer = peer;
    }

    public void run() {
        ServerSocket listener = null;
        try {
            System.out.println("ListeningPort being used: " + peer.listeningPort);
            listener = new ServerSocket(peer.listeningPort);
        } catch (IOException e) {
            System.out.println("Error creating server socket");
            e.printStackTrace();
        }
        try {
            while (true) {
                socket = listener.accept();

                outputStream = new ObjectOutputStream(socket.getOutputStream());

                outputStream.flush();

                inputStream = new ObjectInputStream(socket.getInputStream());

                messageManager manager = new messageManager(inputStream, outputStream, peer, socket); 

                // start handler on thread
                Thread serverThread = new Thread(manager);
                serverThread.start();
            }
        } catch (IOException e) {
            System.out.println("Error creating server thread");
            e.printStackTrace();
        } finally {
            try {
                listener.close();
                
            } catch (IOException e) {
                System.out.println("Error closing server socket");
                e.printStackTrace();
            }
        }

    }

}
