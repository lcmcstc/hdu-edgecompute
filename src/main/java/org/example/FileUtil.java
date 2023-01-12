package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    public static void write(String name,String content) throws IOException {
        File f=new File(name);
        FileOutputStream fos1=new FileOutputStream(f);
        OutputStreamWriter dos1=new OutputStreamWriter(fos1);
        dos1.write(content);
        dos1.close();
    }

    public static String read(String name) throws IOException{
        Path path = Paths.get(name);
        return Files.readString(path);
    }


}
