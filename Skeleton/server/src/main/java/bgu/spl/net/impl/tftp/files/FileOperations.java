package bgu.spl.net.impl.tftp.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;


public class FileOperations{

    private String folderPath;

    public FileOperations(String _folderPath){
        folderPath = _folderPath;
        

        
    }

    public boolean search(String filename){
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) { //checks that folder exist and that he is indeed a directory
            File[] files = folder.listFiles(); //
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(filename)) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    public byte[] readFile(String filename) throws IOException {
        Path filePath = Paths.get(folderPath, filename);
        return Files.readAllBytes(filePath); // check how big the file can be since it said its not suppose to deal with big byte[]
    }
    
    public void writeFile(String filename, byte[] fileData) throws IOException {
        Path filePath = Paths.get(folderPath, filename);
        Files.write(filePath, fileData);
    }


    public boolean delete(String filename){
        File file = new File(filename);
        boolean isDeleted = file.delete();

        return isDeleted;

    }

    public boolean isEmpty(){
        File folder = new File(folderPath);
        if(folder.exists() && folder.isDirectory()){ //checks that folder exist 
            File[] files = folder.listFiles(); // creates an array list of all the files inside the folder
            return files == null || files.length == 0;
        }
        return true;
    }

    public List<String> getAllFiles(){
        List<String> fileList = new ArrayList<>();
        File folder = new File(folderPath);
        if(folder.exists() && folder.isDirectory()){
            File[] files = folder.listFiles();
            for(File file : files){
                if(file.isFile()){
                    fileList.add(file.getName());
                }
            }
        }

        return fileList;
    }





}