
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
 * A server class for handeling multithreaded connections.
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

            ServerThread serverThread = new ServerThread(socket, BUFSIZE);
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
    String retMessage = "";
    boolean streamedPNG = false;

    public final String okHeader = "HTTP/1.1 200 OK\r\n";
    public final String fNFHeader = "HTTP/1.1 404 Not Found\r\n";//File not found

    public final String htmlTypeHeader = "Content-Type: text/html;charset=UTF-8\r\n" +
            "Connection: close\r\n";

    public final String closeHeader = "Connection: close\r\n";
    public final String keepAliveHeader = "Connection: keep-alive\r\n";
    public final String contLength = "Content-Length: ";
    public final String emptySeperator = "\r\n";

    public boolean hasRuned = false;
    byte[] buf;
    InputStream inputStream;
    OutputStream outputStream;

    /**
     * Constructor for ServerThread
     *
     * @param socket
     * @param bufsize
     */
    public ServerThread(Socket socket, int bufsize) {
        this.socket = socket;
        this.bufsize = bufsize;
    }

    @Override
    public void run() {

        StringBuilder retMessageBuilder = new StringBuilder();
        try {
            inputStream = new DataInputStream(this.socket.getInputStream());
            /*while (inputStream.available() == 0) {
                if (hasRuned) System.out.println("STUPID");
            }*/
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
                    /**
                     * HERE only implemented "GET", nest else if for implementation of "POST" and "PUT"
                     */
                    String getInfo = extractInfo(rHeader, getIndex);
                    getInfo = getInfo.substring(getInfo.indexOf('/'));
                    getInfo = getInfo.substring(0, getInfo.indexOf(' '));

                    File requestedFile = new File("src/resources" + getInfo);
                    System.out.println(requestedFile.getAbsolutePath());


                    System.out.println("Path requested: " + getInfo);
                   /* if (getInfo.length() <= 1) {
                        retMessage = returnHeaderHtml("src/resources/user1/index.html");
                    } else*/
                    if (requestedFile.isDirectory()) {
                        retMessage = fNFHeader;
                        System.out.println("Les byrd");
                        File[] files = requestedFile.listFiles();
                        //Only walks one level of directory (by task requirements).
                        for (File file : files) {
                            if (file.getName().equals("index.html")) {
                                retMessage = returnHeaderHtml(file.getAbsolutePath());
                                break;
                            }
                        }
                    } else if (requestedFile.isFile()) {
                        if (getInfo.substring(getInfo.length() - 4).equals(".png")) {
                            //Is png
                            //retMessage = returnHeaderHtml("src/resources/user1/index.html");
                            streamPng(requestedFile, new DataOutputStream(this.socket.getOutputStream()));
                        } else if (getInfo.length() > 5 && getInfo.substring(getInfo.length() - 5).equals(".html")) {
                            //Is html file
                            retMessage = returnHeaderHtml(requestedFile.getAbsolutePath());
                        }
                    } else {
                        //Else, is not folder or directory.
                        retMessage = fNFHeader;
                    }
                } else {
                    throw new UnsupportedOperationException("This server only handles HTTP:GET requests");
                }
            }
            if (!streamedPNG) {
                outputStream = new DataOutputStream(this.socket.getOutputStream());
                //  Thread.sleep(10);
                outputStream.write(retMessage.getBytes());
            }
        } catch (Exception e) {
            System.err.println("Exception in serverThread:\n" + e);
        }

        //TODO CLean
        //if (!streamedPNG) {
        //Kill connection
        try {
            System.out.printf("TCP connection from %s:%d Was killed\n",
                    socket.getInetAddress().getHostAddress(), socket.getPort());
            socket.close();
        } catch (
                Exception e) {
            System.err.println("Failed to close socket in thread: " + e);
        }
        //}
        return;
    }

    String returnHeaderHtml(String path) {
        StringBuilder retString = new StringBuilder();

        File indexFile = new File(path);
        System.out.println(indexFile.getAbsolutePath());
        String indexContent = htmlFileToString(indexFile);

        if (indexContent.length() > 0) {
            retString.append(okHeader);
            retString.append(htmlTypeHeader);
            retString.append(contLength + indexContent.length() + emptySeperator);
            retString.append(emptySeperator);
            retString.append(indexContent);
        }

        return retString.toString();
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

    private void streamPng(File requestedFile, OutputStream outputStream) {
        hasRuned = true;

        try {
            InputStream pictureInputStream = new FileInputStream(requestedFile);
            final long length = requestedFile.length();
            try {
                StringBuilder retStringB = new StringBuilder();
                retStringB.append(okHeader);
                retStringB.append("Content-Type: image/png" + emptySeperator);
                //retStringB.append(keepAliveHeader);//TODO VALIDATE!!
                retStringB.append(closeHeader);
                retStringB.append(contLength + length + emptySeperator);
                retStringB.append(emptySeperator);
                System.out.println(retStringB.toString());
                outputStream.write(retStringB.toString().getBytes());

                //Iterate imageBytes
                long index = 0;
                while (length - (index + bufsize) > 0) {
                    buf = new byte[bufsize];
                    pictureInputStream.read(buf);
                    outputStream.write(buf);
                    index += bufsize;
                }
                if (length - index > 0) {
                    int size = (int) (length - index);
                    buf = new byte[size];
                    pictureInputStream.read(buf);
                    outputStream.write(buf);
                    System.out.println("Actual size: " + (index + buf.length));
                    index += buf.length;
                }
                outputStream.flush();//May be stupid
                outputStream.close();

            } catch (Exception e) {
                System.err.println("SHITEEEE failed at outputstream image \n" + e);
                retMessage = fNFHeader;
                return;
            }

        } catch (FileNotFoundException fNE) {//Validate shit twice
            System.err.println("File not found");
            retMessage = fNFHeader;
            return;
        }
        hasRuned = true;
        streamedPNG = true;
        return;
    }
//TODO Remove

    /**
     * Trim the buffer to the last ellement not equal to NULL
     *
     * @param bytes
     * @return
     */
    /*
    static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }*/
}
