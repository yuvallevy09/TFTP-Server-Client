package bgu.spl.net.impl.tftp.files;
import java.io.Serializable;

import bgu.spl.net.impl.rci.Command;


public class DeleteFileCommand<T> implements Command<Files<T>> {
 
    private String fileName;
 
    public DeleteFileCommand(String fileName) {
        this.fileName = fileName;
    }
 
    @Override
    public Serializable execute(Files<T> files) {
        files.delete(fileName);
        return "OK";
    }
}