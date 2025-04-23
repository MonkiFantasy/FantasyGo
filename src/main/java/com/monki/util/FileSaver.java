package com.monki.util;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileSaver {
    public static void save(String sgf) throws IOException {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = jFileChooser.showOpenDialog(null);
        if(result==JFileChooser.APPROVE_OPTION) {
            File selectedFile = jFileChooser.getSelectedFile();
            String filename = "test.sgf";
            File file = new File(selectedFile.getAbsolutePath(), filename);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(sgf);
            fileWriter.close();
            System.out.println(selectedFile.getAbsolutePath());
            System.out.println(selectedFile.isFile());
        }
    }
}
