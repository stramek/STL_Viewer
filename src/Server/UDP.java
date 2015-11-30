package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class UDP implements Runnable {

    private static float[] values = new float[9];
    private double time = 0;

    @Override
    public void run() {
        host();
    }

    private void host() {
        UdpServer us = new UdpServer();
        us.setPort(9876);
        us.addUdpServerListener( new UdpServer.Listener() {
            @Override
            public void packetReceived( UdpServer.Event evt ) {
                byte[] bytes = evt.getPacketAsBytes();
                values = ByteArray2FloatArray(bytes);
                System.out.println(System.currentTimeMillis() - time); // Drukuje czas wykonania funkcji packet Received
                time = System.currentTimeMillis();
            }
        });
        us.start();
    }

    public static float[] getValues() {
        return values;
    }

    private static float[] ByteArray2FloatArray(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        float[] result = new float[floatBuffer.remaining()];
        floatBuffer.get(result);
        return result;
    }
}