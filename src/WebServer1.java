
/**
 * Inspired by provided UDPEchoServer
 * Biggest difference: kills connection when no messages recived for 2.1 seconds
 *
 * @editor: David Carlsson
 * This is a simple TCP echo Server.
 */

 /*//WIRESHARK:
 port 4950
  */

import java.io.*;
import java.net.*;
import java.util.Arrays;

/**
 * A for handeling multithreaded connections.
 */
public class WebServer1 {
    public static final int BUFSIZE = 1024;
    public static final int MYPORT = 8889;

    private static final String HTMLBODY = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Title</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "HELLO FUCKING WORLD\n" +
            "</body>\n" +
            "</html>";

    public static final String RETURNMESSAGE =
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html;charset=UTF-8\r\n" +
                    "Connection: close\r\n" +
                    "Content-Length: " + HTMLBODY.length() + " \r\n" +
                    "\r\n" +//End of header

                    HTMLBODY;

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(MYPORT);
        int count = 0;
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println();
            System.out.printf("TCP connection accepted from %s:%d\n",
                    socket.getInetAddress().getHostAddress(), socket.getPort());
            //System.out.println("It is conNumber: " + (count += 1));

            ServerThread serverThread = new ServerThread(socket, BUFSIZE, RETURNMESSAGE);
            serverThread.start();
        }
    }
}

/**
 * Class for handeling threads
 */
class ServerThread extends Thread {
    Socket socket;
    int bufsize;
    String message;

    byte[] buf;
    InputStream inputStream;
    OutputStream outputStream;

    /**
     * Constructor for ServerThread
     *
     * @param socket
     * @param bufsize
     * @param message
     */
    public ServerThread(Socket socket, int bufsize, String message) {
        this.socket = socket;
        this.bufsize = bufsize;
        this.message = message;
    }

    @Override
    public void run() {
        long dead = System.currentTimeMillis();
        final int alive = 2100;//Is alive if not dead for time
        boolean triedCpr = false;
        try {
            inputStream = new DataInputStream(this.socket.getInputStream());
            while (inputStream.available() == 0) {
                System.out.println("STUPID");
            }
            byte[] buf = new byte[bufsize];
            while (inputStream.available() > 0 && buf[bufsize - 1] == 0) {
                inputStream.read(buf); //TODO, analyse this
            }^-
            System.out.println(new String(buf));


            outputStream = new DataOutputStream(this.socket.getOutputStream());
            //  Thread.sleep(10);
            outputStream.write(message.getBytes());
        } catch (Exception e) {
            System.err.println("Exception in serverThread:\n" + e);
        }
        //Connection is dead
        try {
            System.out.printf("TCP connection from %s:%d Was killed\n",
                    socket.getInetAddress().getHostAddress(), socket.getPort());
            socket.close();
        } catch (
                Exception e) {
            System.err.println("Failed to close socket in thread: " + e);
        }
        return;

    }

    /**
     * Trim the buffer to the last ellement not equal to NULL
     *
     * @param bytes
     * @return
     */
    static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
}