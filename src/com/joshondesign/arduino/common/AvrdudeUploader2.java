/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduino.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author josh
 */
class AvrdudeUploader2 {
    private final File root;
    private final String uploadPortPath;
    private final Device device;
    private final String buildPath;
    private final String classname;
    private final boolean verbose;
    private CompileTask outtask;

    AvrdudeUploader2(File root, String portPath, Device device, String absolutePath, String classname, boolean verbose) {
        this.root = root;
        this.uploadPortPath = portPath;
        this.device = device;
        this.buildPath = absolutePath;
        this.classname = classname;
        this.verbose = verbose;
    }

    void upload() throws RunnerException {
        Util.p("uploading to device using device.protocol " + this.device.getProtocol());
        //Map<String, String> boardPreferences = Base.getBoardPreferences();
        List commandDownloader = new ArrayList();
        //String protocol = boardPreferences.get("upload.protocol");
        String protocol = device.getProtocol();

        // avrdude wants "stk500v1" to distinguish it from stk500v2
        if (protocol.equals("stk500"))
            protocol = "stk500v1";
        commandDownloader.add("-c" + protocol);
        commandDownloader.add("-P" + (Util.isWindows() ? "\\\\.\\" : "")
                + uploadPortPath);

        commandDownloader.add(
                "-b" + device.getUploadSpeed());//Integer.parseInt("19200"));
        commandDownloader.add("-D"); // don't erase
        //if (!Preferences.getBoolean("upload.verify")) commandDownloader.add("-V"); // disable verify
        commandDownloader.add("-Uflash:w:" + buildPath + File.separator + classname + ".hex:i");
        boolean avrdudeResult = avrdude(commandDownloader);
    }
    public boolean avrdude(Collection params) throws RunnerException {
        List commandDownloader = new ArrayList();

        File hardwarePath = new File(root,"hardware");


        if(Util.isLinux()) {
            File avrdude = new File(root,"tools/avrdude"); 
            if (avrdude.exists()) {
                commandDownloader.add(avrdude);
                commandDownloader.add("-C" + new File(root,"tools/avrdude.conf").getAbsolutePath());
            } else {
                commandDownloader.add("avrdude");
            }
        }
        else {
            //mac and window?
            File avrdude = new File(root,"tools/avr/bin/avrdude");
            File conf = new File(root,"tools/avr/etc/avrdude.conf");
            commandDownloader.add(avrdude.getAbsolutePath());
            commandDownloader.add("-C" + conf.getAbsolutePath());
        }

        if (verbose) {
            commandDownloader.add("-v");
            //commandDownloader.add("-v");
            commandDownloader.add("-v");
            commandDownloader.add("-v");
        } else {
            commandDownloader.add("-q");
            commandDownloader.add("-q");
        }
        commandDownloader.add("-p" + device.getMCU());//"atmega168");
        commandDownloader.addAll(params);

        return executeUploadCommand(commandDownloader);
    }
    
    boolean firstErrorFound;
    boolean secondErrorFound;
    boolean notFoundError;
    RunnerException exception;
    static final String SUPER_BADNESS =("Compiler error, please submit this code to {0}");
    protected boolean executeUploadCommand(Collection commandDownloader)
            throws RunnerException
    {
        firstErrorFound = false;  // haven't found any errors yet
        secondErrorFound = false;
        notFoundError = false;
        int result=0; // pre-initialized to quiet a bogus warning from jikes

        String userdir = System.getProperty("user.dir") + File.separator;

        try {
            String[] commandArray = new String[commandDownloader.size()];
            commandDownloader.toArray(commandArray);

            if (verbose) {
                for(int i = 0; i < commandArray.length; i++) {
                    System.out.print(commandArray[i] + " ");
                }
                System.out.println();
            }
            Process process = Runtime.getRuntime().exec(commandArray);
            MessageSiphon in = new MessageSiphon(process.getInputStream(), new MessageConsumer() {

                              @Override
                              public void message(String s) {
                                  outtask.stdout(s);
                              }
                          });
            MessageSiphon err = new MessageSiphon(process.getErrorStream(), new MessageConsumer() {

                              @Override
                              public void message(String s) {
                                  outtask.stderr(s);
                              }
                          });

            // wait for the process to finish.  if interrupted
            // before waitFor returns, continue waiting
            //
            boolean compiling = true;
            while (compiling) {
                try {
                    in.join();
                    err.join();
                    result = process.waitFor();
                    compiling = false;
                } catch (InterruptedException intExc) {
                }
            }
            if(exception!=null) {
                exception.hideStackTrace();
                throw exception;
            }
            if(result!=0)
                return false;
        } catch (Exception e) {
            String msg = e.getMessage();
            if ((msg != null) && (msg.indexOf("uisp: not found") != -1) && (msg.indexOf("avrdude: not found") != -1)) {
                //System.err.println("uisp is missing");
                //JOptionPane.showMessageDialog(editor.base,
                //                              "Could not find the compiler.\n" +
                //                              "uisp is missing from your PATH,\n" +
                //                              "see readme.txt for help.",
                //                              "Compiler error",
                //                              JOptionPane.ERROR_MESSAGE);
                return false;
            } else {
                e.printStackTrace();
                result = -1;
            }
        }
        //System.out.println("result2 is "+result);
        // if the result isn't a known, expected value it means that something
        // is fairly wrong, one possibility is that jikes has crashed.
        //
        if (exception != null) throw exception;

        if ((result != 0) && (result != 1 )) {
            exception = new RunnerException(SUPER_BADNESS);
            //editor.error(exception);
            //PdeBase.openURL(BUGS_URL);
            //throw new PdeException(SUPER_BADNESS);
        }

        return (result == 0); // ? true : false;

    }

    void setOutputListener(CompileTask task) {
        this.outtask = task;
    }
}
