package message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

public class messageFactory {
    public byte[] handshakeMessage(int peerId) throws IOException {

        byte[] handshakeMessage = new byte[32];
        byte[] header = "P2PFILESHARINGPROJ00000000".getBytes();
        System.arraycopy(header, 0, handshakeMessage, 0, header.length);
        byte[] peerIdBytes = String.format("%04d", peerId).getBytes();
        System.arraycopy(peerIdBytes, 0, handshakeMessage, header.length, 4);

        return handshakeMessage;
    }

    public byte[] chokeMessage() throws IOException {
        byte[] message = new byte[5];
        byte[] messageLength = new byte[4];
        byte[] messageType = new byte[1];
        messageType[0] = 0;

        // determining the length of message
        int lengthNum = messageType.length;
        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
        messageLengthBuffer.putInt(lengthNum);

        messageLength = messageLengthBuffer.array();
        System.arraycopy(messageLengthBuffer.array(), 0, message, 0, 4);
        System.arraycopy(messageType, 0, message, 4, 1);

        return message;
    }

    public byte[] unchokeMessage() throws IOException {

        byte[] message = new byte[5];

        byte[] messageLength = new byte[4];
        byte[] messageType = new byte[1];

        messageType[0] = 1;

        // determining the length of message
        int lengthNum = messageType.length;
        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
        messageLengthBuffer.putInt(lengthNum);
        messageLength = messageLengthBuffer.array();

        System.arraycopy(messageLength, 0, message, 0, 4);
        System.arraycopy(messageType, 0, message, 4, 1);

        return message;
    }

    public byte[] interestedMessage() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] message = new byte[5];

        byte[] messageLength = new byte[4];
        byte[] messageType = new byte[1];

        messageType[0] = 2;

        // determining the length of message
        int lengthNum = messageType.length;
        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
        messageLengthBuffer.putInt(lengthNum);

        messageLength = messageLengthBuffer.array();

        System.arraycopy(messageLength, 0, message, 0, 4);
        System.arraycopy(messageType, 0, message, 4, 1);

        outputStream.write(messageLength);
        outputStream.write(messageType);
        outputStream.close();

        return message;
    }

    // not interested type 3
    public byte[] notInterestedMessage() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] message = new byte[5];
        byte[] messageLength = new byte[4];
        byte[] messageType = new byte[1];

        messageType[0] = 3;

        // determining the length of message
        int lengthNum = messageType.length;
        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
        messageLengthBuffer.putInt(lengthNum);

        messageLength = messageLengthBuffer.array();

        System.arraycopy(messageLength, 0, message, 0, 4);
        System.arraycopy(messageType, 0, message, 4, 1);

        outputStream.write(messageLength);
        outputStream.write(messageType);
        outputStream.close();

        return message;
    }

    public byte[] haveMessage(int index) throws IOException {
        byte[] message = new byte[9];
        byte[] messageLength = new byte[4];
        byte[] messageType = new byte[1];

        messageType[0] = 6;

        int lengthNum = messageType.length;

        ByteBuffer indexBuffer = ByteBuffer.allocate(4);
        indexBuffer.putInt(index);
        byte[] payload = indexBuffer.array();
        lengthNum += payload.length;

        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
        messageLengthBuffer.putInt(lengthNum);

        messageLength = messageLengthBuffer.array();

        System.arraycopy(messageLength, 0, message, 0, 4);
        System.arraycopy(messageType, 0, message, 4, 1);
        System.arraycopy(payload, 0, message, 5, payload.length);

        return message;
    }

    public byte[] bitfieldMessage(BitSet bitfieldMessage) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int lengthCount = 0;

        byte[] messageLength;

        byte[] messageType = new byte[1];
        lengthCount += messageType.length;
        messageType[0] = 5;

        byte[] messagePayload = bitfieldMessage.toByteArray();
        lengthCount += messagePayload.length;

        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
        messageLengthBuffer.putInt(lengthCount);
        messageLength = messageLengthBuffer.array();

        outputStream.write(messageLength);
        outputStream.write(messageType);
        outputStream.write(messagePayload);

        outputStream.close();

        return outputStream.toByteArray();
    }

    public byte[] requestMessage(int index) throws IOException {
        // total 9 bytes
        byte[] message = new byte[9];
        byte[] messageLength = new byte[4];
        byte[] messageType = new byte[1];

        messageType[0] = 6;

        int lengthNum = messageType.length;

        ByteBuffer indexBuffer = ByteBuffer.allocate(4);
        indexBuffer.putInt(index);
        byte[] payload = indexBuffer.array();
        lengthNum += payload.length;

        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);
        messageLengthBuffer.putInt(lengthNum);

        // // Obtain the byte array representation of the messageLengthBuffer.
        messageLength = messageLengthBuffer.array();

        System.arraycopy(messageLength, 0, message, 0, 4);

        System.arraycopy(messageType, 0, message, 4, 1);

        System.arraycopy(payload, 0, message, 5, payload.length);

        return message;

    }

    public byte[] pieceMessage(int index, byte[] content) throws IOException {

        byte[] messageLength = new byte[4];
        byte[] messageType = new byte[1];

        messageType[0] = 7;

        int lengthNum = messageType.length;

        ByteBuffer indexBuffer = ByteBuffer.allocate(4);
        indexBuffer.putInt(index);
        byte[] indexByteArray = indexBuffer.array();
        lengthNum += indexByteArray.length;
        lengthNum += content.length;

        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4);

        messageLengthBuffer.putInt(lengthNum);

        byte[] message = new byte[lengthNum + 4];

        messageLength = messageLengthBuffer.array();

        System.arraycopy(messageLength, 0, message, 0, 4);

        System.arraycopy(messageType, 0, message, 4, 1);

        System.arraycopy(indexByteArray, 0, message, 5, indexByteArray.length);

        System.arraycopy(content, 0, message, 9, content.length);

        return message;
    }

}
