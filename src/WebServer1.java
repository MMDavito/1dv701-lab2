
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

import com.sun.nio.sctp.IllegalReceiveException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A server class for handeling multithreaded connections.
 */


public class WebServer1 {
    public static final int BUFSIZE = 32768;    //=2^15
    /*  Would have the buffer set to 1024, but since a normal GET header could possible reach more than 1024 bytes
    I would have loved to loop it for analysis.
    But since that would be huge effort, and i only need to read the header until the end of header, and if PUT
    or POST simply send the stream to a new thread. i see no reason to have this large buffer except for returning
    Big png files.
    So i configure it to 1/2 the maximal size of a TCP package of 2^16 bytes.
     */
    public static final int MYPORT = 8889;

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(MYPORT);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.printf("\nTCP connection accepted from %s:%d\n",
                    socket.getInetAddress().getHostAddress(), socket.getPort());
            ServerThread serverThread = new ServerThread(socket, BUFSIZE);
            serverThread.start();
        }
    }
}

/**
 * Clas for handeling threads
 */
class ServerThread extends Thread {
    private Socket socket;
    private int bufsize;
    private String retMessage = "";
    private boolean streamedPNG = false;

    private final String okHeader = "HTTP/1.1 200 OK\r\n";
    private final String fNFHeader = "HTTP/1.1 404 Not Found\r\n";//File not found
    private final String forbiddenHeader = "HTTP/1.1 403 Forbidden\r\n";
    private final String serverErrorHeader = "HTTP/1.1 500 Internal Server Error\r\n";
    private final String badRequestHeader = "HTTP/1.1 400 Bad Request\r\n";


    private final String htmlTypeHeader = "Content-Type: text/html;charset=UTF-8\r\n" +
            "Connection: close\r\n";

    private final String closeHeader = "Connection: close\r\n";
    private final String keepAliveHeader = "Connection: keep-alive\r\n";
    private final String contLength = "Content-Length: ";
    private final String emptySeperator = "\r\n";

    boolean hasRuned = false;
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
        boolean isDead = false;

        StringBuilder retMessageBuilder;
        try {
            inputStream = new DataInputStream(this.socket.getInputStream());

            boolean runIndefinitely = true; //ANALYSE UNTIL FALSE, if ever implementing enormous
            // httpRequests(like 1000 Subirectories)

            byte[] buf = new byte[bufsize];//Create buffer
            //Read inputStream to buf while stream is streaming and buf is not full

            int deadTimeOut = 10000;//==10 seconds(when using telnet, use 1 hour = 3600000)
            int index = 0;
            long start = System.currentTimeMillis();
            while (runIndefinitely && buf[bufsize - 1] == 0 && (System.currentTimeMillis() - start) < deadTimeOut) {
                int bytesAvailible = inputStream.available();
                if (bytesAvailible > 0) {
                    if (index + bytesAvailible >= bufsize) {
                        break;
                    }
                    int i = 0;
                    inputStream.read(buf, index, bytesAvailible);
                    index += bytesAvailible;
                    if (index > 4 && new String(buf).substring(index - 4, index).equals(emptySeperator + emptySeperator)) {
                        break;
                    }
                }
            }
            if (!(buf[bufsize - 1] == 0)) {
                System.err.println("Geek, full fucking buffer");//Should maybe see if got a post, and then thread it.
            }
            //Extract information(get)
            String rHeader = new String(trim(buf));//receivedHeader
            int getIndex = findGet(rHeader);//If i read line by line it would be index0-3

            if (getIndex >= 0) {
                /**
                 * HERE only implemented "GET", nest else if for implementation of "POST" and "PUT"
                 */
                String getInfo = extractInfo(rHeader, getIndex);
                getInfo = getInfo.substring(getInfo.indexOf('/'));
                getInfo = getInfo.substring(0, getInfo.indexOf(' '));

                String sForbidden = "forbidden";//Search for Forbidden
                String sSven = "sven";          //Search for Sven
                String sKill = "kill_yourself"; //Search Kill, Used to throw exception.
                if (getInfo.length() >= (sForbidden.length() + 1) && getInfo.substring(1, sForbidden.length() + 1).equals(sForbidden)) {
                    retMessage = forbiddenHeader;
                } else if (getInfo.length() >= (sSven.length() + 1) && getInfo.substring(1, sSven.length() + 1).equals(sSven)) {
                    retMessageBuilder = new StringBuilder();
                    retMessageBuilder.append("HTTP/1.1 302 Found" + emptySeperator);
                    retMessageBuilder.append("Location: http://lmgtfy.com/?q=sven" + emptySeperator);
                    retMessage = retMessageBuilder.toString();
                } else if (getInfo.length() >= (sKill.length() + 1) && getInfo.substring(1, sKill.length() + 1).equals(sKill)) {
                    throw new IllegalReceiveException("I'm sorry " + socket.getPort() + ", I'm afraid I can't do that");
                } else {
                    //Is a valid request
                    File requestedFile = new File("resources" + getInfo);
                    if (requestedFile.isDirectory()) {
                        retMessage = fNFHeader;
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
                            streamPng(requestedFile, new DataOutputStream(this.socket.getOutputStream()));
                        } else if (getInfo.length() > 5 && getInfo.substring(getInfo.length() - 5).equals(".html")) {
                            //Is html file
                            String temp = returnHeaderHtml(requestedFile.getAbsolutePath());
                            if (temp.length() > 0) {
                                retMessage = temp;
                            }//else retMessage is serverError
                        }
                    } else {
                        //Else, is not file or directory.
                        retMessage = fNFHeader;
                    }
                }
            } else {
                //Usually an empty request, if other request type, support should be added
                //All these cases are completely ignored.
                System.err.println("You are stupid, therefore you will die port:\n" + socket.getPort() + " This server only handles HTTP:GET requests");
                socket.close();
                isDead = true;
            }
            if (!streamedPNG && !isDead) {
                outputStream = new DataOutputStream(this.socket.getOutputStream());
                //  Thread.sleep(10);
                outputStream.write(retMessage.getBytes());
            }
        } catch (Exception e) {
            System.err.println("Exception in serverThread:\n" + e);
            retMessage = serverErrorHeader;
            if (!socket.isClosed()) {
                try {
                    outputStream = socket.getOutputStream();
                    outputStream.write(retMessage.getBytes());
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e1) {
                    System.err.println("Severe Exception failed to notify fail:");
                    e1.printStackTrace();
                }
            }
        }
        if (!isDead) {
            //Kill connection
            try {
                System.out.printf("TCP connection from %s:%d Was killed\n",
                        socket.getInetAddress().getHostAddress(), socket.getPort());
                socket.close();
            } catch (
                    Exception e) {
                System.err.println("Failed to close socket in thread: " + e);
            }
        }
        return;
    }

    /**
     * Returns a html in a http response complete with headers and body.
     *
     * @param path
     * @return
     * @throws IOException Since it uses <code>htmlFileToString()</code>
     */
    String returnHeaderHtml(String path) throws IOException {
        StringBuilder retStringBuilder = new StringBuilder();

        File indexFile = new File(path);
        String indexContent = htmlFileToString(indexFile);

        if (indexContent.length() > 0) {
            retStringBuilder.append(okHeader);
            retStringBuilder.append(htmlTypeHeader);
            retStringBuilder.append(contLength + indexContent.length() + emptySeperator);
            retStringBuilder.append(emptySeperator);
            retStringBuilder.append(indexContent);
        }
        return retStringBuilder.toString();
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

    /**
     * @param htmlFile
     * @return content of Html file as a string.
     * @throws IOException Since it reads a File.
     */
    String htmlFileToString(File htmlFile) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String absPath = htmlFile.getAbsolutePath();
        if (!htmlFile.isFile() || !absPath.substring(absPath.length() - 5).equals(".html")) {
            System.err.println(htmlFile.getAbsolutePath() + " is not an actual html file!");
        } else {
            BufferedReader indexReader = new BufferedReader(new FileReader(htmlFile));
            String line;
            while ((line = indexReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            indexReader.close();
        }
        return stringBuilder.toString();
    }

    /**
     * Searches the string <code>search</code> for <code>GET</code>
     *
     * @param search String to search.
     * @return
     */
    int findGet(String search) {
        int ret = -1;
        final String GET = "GET ";
        ret = search.indexOf(GET);
        return ret;
    }

    private void streamPng(File requestedFile, OutputStream outputStream) {
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
        streamedPNG = true;
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