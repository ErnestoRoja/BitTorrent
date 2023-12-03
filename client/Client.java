package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import message.messageManager;
import peer.Peer;

public class Client {
    private Peer hostPeer;
    private Peer peerToConnectTo;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    public Client(Peer host, Peer target){
        this.hostPeer = host;
        this.peerToConnectTo = target;
    }

    // Connects the host peer with another peer
    public void connect() {
        try {
            Socket socket = new Socket(peerToConnectTo.hostName, peerToConnectTo.listeningPort);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Instantiate a message handler to control incoming and outgoing messages
            messageManager manager = new messageManager(inputStream, outputStream, hostPeer, socket);
            System.out.println("Host PeerID [" + this.hostPeer.peerID + "] manager's peerID is being set to: " + this.peerToConnectTo.peerID);
            manager.setManagerPeerID(this.peerToConnectTo.peerID);

            // Instantiate the handler on its own thread
            Thread serverThread = new Thread(manager);
            serverThread.start();
        }
        catch (ConnectException e) {
            System.out.println("Connection refused. You need to initiate a server first.");
        }
        catch(UnknownHostException unknownHost){
            System.out.println("You are trying to connect to an unknown host!");
        }
        catch(IOException e){
            System.out.println("IOException when trying to start the client.");
            e.printStackTrace();
        }
    }
}