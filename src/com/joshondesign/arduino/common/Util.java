package com.joshondesign.arduino.common;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
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

    public static String toString(URL url) throws IOException {
        Reader in = new InputStreamReader(url.openStream());
        StringBuffer string = new StringBuffer();
        char[] buffer = new char[256];
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

    // launching code from http://www.centerkey.com/java/browser/
    public static void openBrowser(String url) {
        String os = System.getProperty("os.name");
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                        new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else { //assume Unix or Linux
                String[] browsers = {
                    "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++) {
                    if (Runtime.getRuntime().exec(
                            new String[]{"which", browsers[count]}).waitFor() == 0) {
                        browser = browsers[count];
                    }
                }
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                } else {
                    Runtime.getRuntime().exec(new String[]{browser, url});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
