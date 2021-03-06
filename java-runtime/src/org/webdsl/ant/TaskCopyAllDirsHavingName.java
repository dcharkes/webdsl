package org.webdsl.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import org.apache.tools.ant.Task;

/**
 *  copies all sub folders(recursively) of Basedir that equals to parameter name and are not in the folder of Exclude
 */
public class TaskCopyAllDirsHavingName  extends Task {
    private String Basedir;
    private String Name;
    private ArrayList<String> Exclude;
    private String To;
    private String nameWindows;
    private String nameUNIX;
    private int numberOfFiles;

    public void setBasedir(String basedir) {
        Basedir = basedir;
    }

    public void setName(String name) {
        Name = name;
        nameWindows = name.replace('/', '\\');
        nameUNIX = name.replace('\\', '/');
    }

    public void setExclude(String exclude) {
        String[] elements = exclude.split(",");
        Exclude = new ArrayList<String>();
        for (String elem : elements){
            Exclude.add(elem.trim());
        }
    }

    public void setTo(String to) {
        To = to;
    }

    public void execute() {
        numberOfFiles = 0;
//        System.out.println(this.toString());
        try {
            findDirectoryAndCopy(new File(Basedir));
        } catch (Exception e) {
            System.out.println("Skipped copying " + Basedir + " to: " + To + " : " + e.getMessage());
        }
        if(numberOfFiles > 0){
//        	System.out.println("Copied " + numberOfFiles + " modified files from directories named " + Name + " to " + To);
        	System.out.println("Copied " + numberOfFiles + " files from directories named " + Name + " to " + To);
        }
    }

    @Override
    public String toString() {
        return "TaskCopyAllDirsHavingName [Basedir=" + Basedir + ", Name="
                + Name + ", Exclude=" + Exclude + ", To=" + To + "]";
    }

    private void findDirectoryAndCopy(File basedir) throws IOException {
        if(basedir.isDirectory()) {
            for(String file : basedir.list()) {
                File newFile = new File(basedir, file);
                if(Exclude.contains(file) || newFile.isHidden()) {
                    continue;
                }
                else if(newFile.getAbsolutePath().endsWith(nameUNIX) || newFile.getAbsolutePath().endsWith(nameWindows)) {
                    copyDirectory(newFile, new File (To));
                } else {
                    findDirectoryAndCopy(newFile);
                }
            }
        }
    }

    private void copyDirectory(File src, File dest) throws IOException {
        if(src.isDirectory()) {
            if(!dest.exists()) {
                 dest.mkdir();
            }

            for(String file : src.list()) {
                File from = new File(src, file);
                File to = new File(dest, file);
                copyDirectory(from, to);
            }
        } else {
//        	if(dest.exists() && src.lastModified() <= dest.lastModified()){
//        		return; //skip when files are the same
//            } else {
	            copyFile(src, dest);
	            numberOfFiles ++;
//            }
       }
    }

    private static void copyFile(File source, File dest) throws IOException {
        if(!dest.exists()) {
            dest.createNewFile();
        }
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            out.transferFrom(in, 0, in.size());
        }
        finally {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.close();
            }
            dest.setLastModified(source.lastModified());
        }
    }


}


