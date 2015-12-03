import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class UDP implements Runnable {

    private byte[] receiveData = new byte[4*10];
    private DatagramSocket serverSocket;
    private static float[] values = new float[10];

    @Override
    public void run() {
        host();
    }

    private void host() {
        try {
            serverSocket = new DatagramSocket(9876);
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                serverSocket.receive(receivePacket);
                byte[] bytes = receivePacket.getData();
                values = ByteArray2FloatArray(bytes);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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