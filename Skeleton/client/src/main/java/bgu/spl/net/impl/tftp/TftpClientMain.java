package bgu.spl.net.impl.tftp;
import java.io.BufferedReader;
import java.io.InputStreamReader; 

import java.io.IOException;

public class TftpClientMain {

    public static void main(String[] args) throws IOException {

        try (TftpClient c = new TftpClient(args[0], 7777)) {

            // start keyboard thread that reads keys and sends to server
            // User input gets strings -> encodes -> sends (writes out_
            // in send check error before write
            // when sent, thread is in wait

            Thread keyboardThread = new Thread(() -> {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (!c.shouldTerminate()) {
                    try{
                        byte[] userInput = br.readLine().getBytes(); 
                        c.send(userInput);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            });

            Thread listenerThread = new Thread(() -> {
                while (!c.shouldTerminate()) {
                    try {
                        c.receive();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            keyboardThread.start();
            listenerThread.start();
        } 
        catch(IOException ex){};
    }
}
