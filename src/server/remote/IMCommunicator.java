package server.remote;

import misc.Utilities;
import model.ModelController;
import java.io.*;
import java.net.*;

/**
 * Created on 2018-09-15
 * @author Derek Worth
 */
public class IMCommunicator {
    private final int portNumber = 8456;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isListening;

    private final server.local.Console con;
    private final ModelController mc;
    
    public IMCommunicator(server.local.Console con, ModelController mc) {
        this.con = con;
        this.mc = mc;
        isListening = false;
    }
    
    private String processRequest(String msg) {
        String un, msgHash1, cmds;
        String[] tokens = msg.split(" ");
        
        if(tokens.length > 2) { // each msg must contain a minimum of 3 tokens--un, pw, and cmd(s)
            msgHash1 = tokens[0];
            un = tokens[1].toLowerCase();
            // everything after un/msgHash is considered part of the command(s)
            cmds = msg.substring(un.length() + msgHash1.length() + 2);
            // authenticate
            String pwHash = mc.getPassword(un);
            String msgHash2 = Utilities.getHash(pwHash + un + " " + cmds);
            if(msgHash1.equalsIgnoreCase(msgHash2)) { // msg integrity and authenticity check
                // interpret cmds
                Commands commands = new Commands(mc, un, Utilities.getDatestamp(0), cmds);
                // TODO: respond
                return commands.executeCommands();
            } else {
                return "Access denied. Check credentials and try again.";
            }
        } else {
            return "Error: invalid input.";
        }
    }
    
    public void stopListening() {
        isListening = false;
    }
    
    public void listen() {
        if(!isListening) { // only start listening if not already listening
            isListening = true;
            Runnable connListener = new Runnable() {
                @Override
                public void run() {
                    while (isListening) {
                        //System.out.println("Connection closed, listening...");
                        try (
                            ServerSocket serverSocket = new ServerSocket(portNumber);
                            // wait for client connection
                            Socket clientSocket = serverSocket.accept();
                            // output from server to client
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            // input from client to server
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        ) {
                            //System.out.println("Connected to " + clientSocket.getInetAddress() + ".\n");
                            String inputLine;
                            String msg = "";
                            while ((inputLine = in.readLine()) != null) {
                                msg = msg + " " + inputLine + "\n";
                                if(inputLine.contains("`")) break;
                            }
                            msg = msg.trim();
                            msg = msg.substring(0, msg.length()-1); // remove ` at end
                            //System.out.println("msg rcv'd: '" + msg + "'");
                            String result = processRequest(msg);
                            //System.out.println("TO CLIENT:\n" + result);
                            out.println(result + "`");
                            con.updateAll();
                        } catch (IOException e) {
                            //System.out.println("Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
                            //System.out.println(e.getMessage());
                        }
                    }
                }
            };

            Thread t1 = new Thread(connListener);
            t1.start();
        }
    }
}
