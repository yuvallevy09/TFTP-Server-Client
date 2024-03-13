package bgu.spl.net.impl.tftp;

import java.io.IOException;

public class TftpClientMain {

    public static void main(String[] args) throws IOException {

        try (TftpClient c = new TftpClient(args[0], 7777)) {

            // start keyboard thread that reads keys and sends to server
            // start listening thread that receives 

            System.out.println("sending message to server");
            c.send(args[1].getBytes());

            System.out.println("awaiting response");
            c.receive();
        } // added closing brace to match the opening brace of try block
        catch(IOException ex){};
    }
}
