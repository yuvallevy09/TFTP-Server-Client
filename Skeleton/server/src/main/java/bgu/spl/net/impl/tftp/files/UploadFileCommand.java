package bgu.spl.net.impl.tftp.files;
import java.io.Serializable;

import bgu.spl.net.impl.rci.Command;


public class UploadFileCommand<T> implements Command<Files<T>> {
 
    private String fileName;
    private T file;
 
    public UploadFileCommand(String fileName, T file) {
        this.fileName = fileName;
        this.file = file;
    }
 
    @Override
    public Serializable execute(Files<T> files) {
        files.upload(fileName, file);
        return "OK";
    }
}