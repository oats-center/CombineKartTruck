package edu.purdue.combinekarttruck.utils;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by Zyglabs on 7/8/15.
 */
public class LogFile {
    private String name;
    private File file;
    private FileWriter writer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public FileWriter getWriter() {
        return writer;
    }

    public void setWriter(FileWriter writer) {
        this.writer = writer;
    }
}
