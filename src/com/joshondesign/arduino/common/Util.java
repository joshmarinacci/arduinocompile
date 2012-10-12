package com.joshondesign.arduino.common;

import java.io.*;
import java.util.List;

public class Util {
    public static void p(String ... more) {
        for(String m : more) {
            System.out.print(m);
        }
        System.out.println();
    }

    public static void p(List<String> comm) {
        for(String s : comm) System.out.println(s);
    }

    public static void p(StringBuffer object) {
        object.toString();
    }

    public static File createTempDir(String josharduinobuild) throws IOException {
        File file = File.createTempFile(josharduinobuild,null);
        file.delete();
        file.mkdir();
        //file.deleteOnExit();
        return file;
    }

    public static String toString(File file) throws IOException {
        StringBuffer string = new StringBuffer();
        char[] buffer = new char[256];
        FileReader in = new FileReader(file);
        while(true) {
            int n = in.read(buffer);
            if(n < 0) break;
            string.append(buffer,0,n);
        }
        return string.toString();
    }

    public static void toFile(String s, File file) throws IOException {
        FileWriter out = new FileWriter(file);
        out.write(s);
        out.close();
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }
    static public boolean isLinux() {
        return System.getProperty("os.name").contains("Linux");
    }

    public static boolean isMacOSX() {
        return System.getProperty("os.name").contains("OS X");
    }
}
