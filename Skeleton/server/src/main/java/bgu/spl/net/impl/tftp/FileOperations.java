package bgu.spl.net.impl.tftp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;


public class FileOperations{

    private File filesDirectory;

    public FileOperations(String folderPath){
        this.filesDirectory = new File(folderPath);
    }

    public boolean searchFile(String filename){
        if (filesDirectory.exists() && filesDirectory.isDirectory()) { //checks that folder exist and that he is indeed a directory
            File[] files = filesDirectory.listFiles(); //returns an array of all the files
            if (files != null) {
                for (File file : files) { //iterate on all of the files
                    if (file.isFile() && file.getName().equals(filename)) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    public byte[] readFile(String filename) {
        File fileToRead = new File(filesDirectory, filename);
        try (FileInputStream fis = new FileInputStream(fileToRead);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1<<10]; //starts with 1K
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void writeFile(String filename, byte[] uploadFile) {
    File destinationFile = new File(filesDirectory, filename);
    try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
        fos.write(uploadFile);
    } catch (IOException e) {
        e.printStackTrace();
    }
}


    public boolean deleteFile(String filename){
        File fileToDelete = new File(filesDirectory, filename);
        return fileToDelete.delete();
    }

    public boolean isEmpty(){
        if(filesDirectory.exists() && filesDirectory.isDirectory()){ //checks that folder exist 
            File[] files = filesDirectory.listFiles(); // creates an array list of all the files inside the folder
            return files == null || files.length == 0;
        }
        return true;
    }

    public String getAllFiles() {
        StringBuffer fileList = new StringBuffer(); //Thread safe, if not neccesary we can use StringBuilder
        File[] files = filesDirectory.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                fileList.append(file.getName()).append("\n");
            }
        }
        return fileList.toString();
    }
}
