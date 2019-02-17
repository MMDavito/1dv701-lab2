
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
import java.util.ArrayList;
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


    public final String okHeader = "HTTP/1.1 200 OK\r\n";
    public final String fNFHeader = "HTTP/1.1 404 Not Found\r\n";//File not found

    public final String htmlTypeHeader = "Content-Type: text/html;charset=UTF-8\r\n" +
            "Connection: close\r\n";
    public final String contLength = "Content-Length: ";
    public final String emptySeperator = "\r\n";


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
StringBuilder retMessageBuilder = new StringBuilder();
        try {
            inputStream = new DataInputStream(this.socket.getInputStream());
            while (inputStream.available() == 0) {
                System.out.println("STUPID");
            }
            boolean runIndefinitely = true;//ANALYSE UNTIL FALSE

            while (runIndefinitely) {
                runIndefinitely = false;
                byte[] buf = new byte[bufsize];//Create buffer
                //Read inputStream to buf while stream is streaming and buf is not full
                while (inputStream.available() > 0 && buf[bufsize - 1] == 0) {
                    inputStream.read(buf); //TODO, analyse this
                }

                //Extract information(get)
                String rHeader = new String(buf);//receivedHeader
                int getIndex = findGet(rHeader);//If i read line by line it would be index0-3
                if (getIndex >= 0) {
                    String getInfo = extractInfo(rHeader, getIndex);
                    System.out.println("Beutifull: \n" + getInfo);
                }
                System.out.println();
                File indexFile = new File("src/user1/index.html");
                String indexContent = htmlFileToString(indexFile);
                System.out.println("INDEXXSAD");
                System.out.println(indexContent);
                System.out.println(indexContent);
                System.out.println();

            }



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
     * Could have used a arraylist for info sutch as ACCEPT
     *
     * @param header
     * @param start  Index where start of substring
     * @return <code>header</code> substringed until first \n
     */
    String extractInfo(String header, int start) {
        String info = header.substring(start, header.indexOf('\n'));
        return info;
    }

    String htmlFileToString(File htmlFile) {
        StringBuilder stringBuilder = new StringBuilder();
        String absPath = htmlFile.getAbsolutePath();
        System.out.println("SHITEEEE");
        System.out.println(absPath.substring(absPath.length() - 5));
        System.out.println("");
        if (!htmlFile.isFile() || !absPath.substring(absPath.length() - 5).equals(".html")) {
            System.err.println(htmlFile.getAbsolutePath() + " is not an actual html file!");
        } else {
            try {
                BufferedReader indexReader = new BufferedReader(new FileReader(htmlFile));
                String line;
                while ((line = indexReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                indexReader.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Make algorithm better if time
     *
     * @param search
     * @return
     */
    int findGet(String search) {
        int ret = -1;
        final String GET = "GET ";
        ret = search.indexOf(GET);
        return ret;
    }


    //TODO Remove

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