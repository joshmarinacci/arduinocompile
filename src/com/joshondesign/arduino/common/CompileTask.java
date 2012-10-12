package com.joshondesign.arduino.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompileTask {
    private File sketchDir;
    private File hardwareDir;
    private File userLibrariesDir;
    private File arduinoLibrariesDir;

    public static void main(String ... args) throws IOException {
        CompileTask task = new CompileTask();
        //task.setSketchDir(new File("/Users/Josh/Documents/Arduino/Blink"));
        task.setSketchDir(new File("/Users/josh/Documents/Arduino/led_test_03"));
        task.setUserLibrariesDir(new File("/Users/josh/Documents/Arduino/Libraries"));
        task.setArduinoLibrariesDir(new File("/Users/josh/projects/Arduino.app/Contents/Resources/Java/libraries"));
        task.setHardwareDir(new File("/Users/josh/projects/Arduino.app/Contents/Resources/Java/hardware/"));
        task.assemble();
        task.download();
    }
    private String portPath;
    private Device device;


    public void setUserLibrariesDir(File file) {
        this.userLibrariesDir = file;
    }
    /*

    package
    verify
    compile
    download

    prereqs:
        sketch directory
        list of files to include or exclude
        desired temp dirs
        desired output dir for the hex file
        name of the hex file
        location of the compiler
        location of standard include files
        name of the board
        location of board specific include files


    use this as a guide:
        http://arduino.cc/en/Hacking/BuildProcess



    assemble:
        concat all .ino files
        include #include "WProgram.h"

        create decs for all functions. put before code but after #defines and #includes
        append target board's main.cxx

        set some vars based on the current board ?

    compile:
        invoke avr-gcc with proper include dirs
            build .c .cpp files
            link .o into static lib
            generate .hex file


    upload:



     */

    public void assemble() throws IOException {
        Util.p(sketchDir.list());
        Util.p("sketch path = " + sketchDir.getAbsolutePath());
        for(String file : sketchDir.list()) {
            Util.p("   file = ",file);
        }
        
        String sketchName = sketchDir.getName();
        
        Util.p("sketch name = " + sketchName);

        File avrBase = new File(hardwareDir, "tools/avr/bin");
        File corePath = new File(new File(hardwareDir,"arduino/cores"),device.core);
        File variantPath = new File(new File(hardwareDir,"arduino/variants/"),device.variant);

        
        List<File> includePaths = new ArrayList<File>();
        //core arduino include
        includePaths.add(corePath);
        //standard variant
        includePaths.add(variantPath);


        //File tempdir = Util.createTempDir("josharduinobuild");
        File tempdir = new File("/tmp/blah");
        tempdir.mkdirs();
        File cfile = new File(tempdir,sketchName+".cpp");
        Util.p("sketch c file " + cfile.getAbsolutePath());
        
        //assemble the C file
        StringBuffer code = new StringBuffer();
        for(File sketchFile : sketchDir.listFiles()) {
            //code.append("#line 1 \"" + sketchFile.getName()+ "\"\n");
            //code.append(Util.toString(sketchFile));
        }
        //Util.toFile(code.toString(),cfile);
        //Util.p(code.toString());

        FileWriter writer = new FileWriter(cfile);

        //Arduino.h include file
        writer.append("#include \"Arduino.h\"\n");
        writer.append(code.toString());
        writer.append("\n");
        writer.close();



        //list of all possible libraries
        List<File> libraryDirs = new ArrayList<File>();
        libraryDirs.addAll(Arrays.asList(arduinoLibrariesDir.listFiles()));
        libraryDirs.addAll(Arrays.asList(userLibrariesDir.listFiles()));



        //compile the sketch itself
        Util.p("============ compiling the sketch");
        List<File> cFiles = new ArrayList<File>();
        cFiles.add(cfile);
        includePaths.addAll(calculateIncludePaths(sketchDir,libraryDirs));
        compile(tempdir,cFiles,includePaths);

        
        //compile any 3rd party libs used
        Util.p("============ compiling 3rd party libs");
        cFiles.clear();
        List<File> libPaths = new ArrayList<File>();
        libPaths.addAll(calculateIncludePaths(sketchDir, libraryDirs));
        for(File libdir : libPaths) {
            for(File file : libdir.listFiles()) {
                if(file.getName().toLowerCase().endsWith(".c")) cFiles.add(file);
                if(file.getName().toLowerCase().endsWith(".cpp")) cFiles.add(file);
            }
        }
        compile(tempdir,cFiles,includePaths);


        Util.p("============ compiling the core");
        //compile the core code
        includePaths.clear();
        includePaths.add(corePath);
        includePaths.add(variantPath);
        cFiles.clear();
        cFiles.addAll(Arrays.asList(corePath.listFiles()));
        compile(tempdir,cFiles,includePaths);


        //link everything into core.a
        for(File file : tempdir.listFiles()) {
            if(file.getName().toLowerCase().endsWith(".o")) {
                List<String> linkCommand = generateCoreACommand(avrBase, tempdir);
                linkCommand.add(file.getAbsolutePath());
                execCommand(linkCommand);
            }
        }



        // 4. link it all together into the .elf file
        // For atmega2560, need --relax linker option to link larger
        // programs correctly.
        List<String> linkElf = new ArrayList<>();
        linkElf.add(new File(avrBase,"avr-gcc").getAbsolutePath());
        linkElf.add("-Os"); //??
        linkElf.add("-Wl,--gc-sections" + "");//not using relax yet
        linkElf.add("-mmcu="+device.mcu);//atmega168
        linkElf.add("-o");
        linkElf.add(new File(tempdir,sketchName+".cpp.elf").getAbsolutePath());
        /*
        for(File file : tempdir.listFiles()) {
            if(file.getName().toLowerCase().endsWith(".o")) {
                linkElf.add(file.getAbsolutePath());
            }
        }*/
        linkElf.add(new File(tempdir, sketchName + ".cpp.o").getAbsolutePath());
        linkElf.add(new File(tempdir,"core.a").getAbsolutePath());
        linkElf.add("-L"+tempdir.getAbsolutePath());
        linkElf.add("-lm");
        execCommand(linkElf);





        // 5. extract EEPROM data (from EEMEM directive) to .eep file.
        List<String> objCopy1 = new ArrayList<String>();
        objCopy1.add(new File(avrBase, "avr-objcopy").getAbsolutePath());
        objCopy1.add("-O");

        objCopy1.add("ihex");
        objCopy1.add("-j");
        objCopy1.add(".eeprom");
        objCopy1.add("--set-section-flags=.eeprom=alloc,load");
        objCopy1.add("--no-change-warnings");
        objCopy1.add("--change-section-lma");
        objCopy1.add(".eeprom=0");
        objCopy1.add(new File(tempdir, sketchName+".cpp.elf").getAbsolutePath());
        objCopy1.add(new File(tempdir, sketchName+".cpp.eep").getAbsolutePath());
        execCommand(objCopy1);


        // 6. build the .hex file
        List<String> objCopy = new ArrayList<String>();
        objCopy.add(new File(avrBase,"avr-objcopy").getAbsolutePath());
        objCopy.add("-O");
        objCopy.add("ihex");
        objCopy.add("-R");

        objCopy.add(".eeprom");//remove eeprom data
        objCopy.add(new File(tempdir,sketchName+".cpp.elf").getAbsolutePath());
        objCopy.add(new File(tempdir,sketchName+".cpp.hex").getAbsolutePath());
        execCommand(objCopy);

    }

    private Collection<? extends File> calculateIncludePaths(File sketchDir, List<File> libraryDirs) {
        List<File> includeDirs = new ArrayList<File>();
        for(File file : sketchDir.listFiles()) {
            if(!file.getName().toLowerCase().endsWith(".ino")) continue;
            try {
                String code = Util.toString(file);
                String importRegexp = "^\\s*#include\\s*[<\"](\\S+)[\">]";
                String[][] pieces = matchAll(code, importRegexp);
                if(pieces!= null) {
                    for(String[] p : pieces) {
                        String libname = p[1];
                        Util.p("getting library: " + libname);
                        for(File libfile : libraryDirs) {
                            File includefile = new File(libfile,libname);
                            File utilDir = new File(libfile,"utility");
                            if(includefile.exists()) {
                                if(includefile.getName().toLowerCase().equals(libname.toLowerCase())) {
                                    Util.p("got a match!");
                                    includeDirs.add(libfile);
                                    if(utilDir.exists()) includeDirs.add(utilDir);
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
        return includeDirs;
    }
    
    static public String[][] matchAll(String what, String regexp) {
        Pattern p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(what);
        ArrayList<String[]> results = new ArrayList<String[]>();
        int count = m.groupCount() + 1;
        while (m.find()) {
            String[] groups = new String[count];
            for (int i = 0; i < count; i++) {
                groups[i] = m.group(i);
            }
            results.add(groups);
        }
        if (results.isEmpty()) {
            return null;
        }
        String[][] matches = new String[results.size()][count];
        for (int i = 0; i < matches.length; i++) {
            matches[i] = (String[]) results.get(i);
        }
        return matches;
    }

    private List<String> generateCoreACommand(File avrBase, File tempdir) {
        List<String> comm = new ArrayList<String>();
        File compilerBinary = new File(avrBase,"avr-ar");
        comm.add(compilerBinary.getAbsolutePath());
        comm.add("rcs");
        File coreLibrary = new File(tempdir,"core.a");
        comm.add(coreLibrary.getAbsolutePath());
        return comm;
    }

    private void compile(File tempdir, List<File> cFiles, List<File> includePaths) {
        File avrBase = new File("/Users/josh/projects/Arduino.app/Contents/Resources/Java/hardware/tools/avr/bin");
        for(File file : cFiles) {
            if(file.getName().toLowerCase().endsWith(".c")) {
                List<String> comm = generateCCommand(tempdir, avrBase, file, includePaths);
                execCommand(comm);
            }
            if(file.getName().toLowerCase().endsWith(".cpp")) {
                List<String> comm = generateCPPCommand(tempdir, avrBase, file, includePaths);
                execCommand(comm);
            }
        }
    }

    private List<String> generateCPPCommand(File tempdir, File avrBase, File cppfile, List<File> includePaths) {
        List<String> comm = new ArrayList<String>();
        File compilerBinary = new File(avrBase,"avr-g++");
        comm.add(compilerBinary.getAbsolutePath());
        comm.add("-c"); //compile, don't link
        comm.add("-g"); //include debug info and line numbers
        comm.add("-Os"); //optimize for size
        comm.add("-Wall"); //turn on verbose warnings
        comm.add("-fno-exceptions"); // ??
        comm.add("-ffunction-sections"); // put each function in it's own section
        comm.add("-fdata-sections"); // ??
        comm.add("-mmcu="+device.mcu);
        comm.add("-DF_CPU="+device.f_cpu);
        comm.add("-MMD"); //output dependency info
        comm.add("-DARDUINO=101");
        comm.add("-DUSB_VID="+device.vid);
        comm.add("-DUSB_PID="+device.pid);

        for(File file : includePaths) {
            comm.add("-I"+file.getAbsolutePath());
        }
        //add source file
        comm.add(cppfile.getAbsolutePath());
        //add output: -o objectname
        comm.add("-o");
        comm.add(new File(tempdir,cppfile.getName()+".o").getAbsolutePath());

        return comm;
    }

    private List<String> generateCCommand(File tempdir, File avrBase, File cfile, List<File> includePaths) {
        List<String> comm = new ArrayList<String>();
        File compilerBinary = new File(avrBase,"avr-gcc");
        comm.add(compilerBinary.getAbsolutePath());
        comm.add("-c"); //compile, don't link
        comm.add("-g"); //turn on debugging so that we can get line numbers
        comm.add("-Os"); //optimize for size
        comm.add("-Wall"); //verbose warnings
        comm.add("-ffunction-sections"); //place each function in its own section
        comm.add("-fdata-sections"); //??
        //comm.add("-assembler-with-cpp"); //???
        comm.add("-mmcu="+device.mcu);//atmega168
        comm.add("-DF_CPU="+device.f_cpu);//16000000L
        comm.add("-MMD"); //output dependency info
        comm.add("-DARDUINO=101");
        comm.add("-DUSB_VID=");
        comm.add("-DUSB_PID=");

        //add include paths
        for(File file : includePaths) {
            comm.add("-I"+file.getAbsolutePath());
        }
        //add source file
        comm.add(cfile.getAbsolutePath());
        //add output: -o objectname
        comm.add("-o");
        comm.add(new File(tempdir,cfile.getName()+".o").getAbsolutePath());
        return comm;
    }

    private void execCommand(List<String> comm) {
        //Util.p("=== execing ==============");
        for(String c : comm) {
            System.out.print(c + " ");
        }
        System.out.println();
        //Util.p(comm);
        try {
            Process process = Runtime.getRuntime().exec(comm.toArray(new String[0]));
            MessageSiphon in = new MessageSiphon(process.getInputStream(), new MessageConsumer() {
                public void message(String s) {
                    Util.p(s);
                }
            });
            MessageSiphon err = new MessageSiphon(process.getErrorStream(), new MessageConsumer() {
                public void message(String s) {
                    Util.p(s);
                }
            });
            try {
                in.join();
                err.join();
                int result = process.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            //System.out.println("result is " + result);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSketchDir(File sketchDir) {
        this.sketchDir = sketchDir;
    }

    public void setHardwareDir(File hardwareDir) {
        this.hardwareDir = hardwareDir;
    }

    public void setArduinoLibrariesDir(File arduinoLibrariesDir) {
        this.arduinoLibrariesDir = arduinoLibrariesDir;
    }


    public void download() {
        Uploader uploader = new AvrdudeUploader();
        File buildPath = new File("/tmp/blah");
        String classname = "led_test_03.cpp";
        try {
            uploader.setUploadPortPath(this.portPath);
            uploader.uploadUsingPreferences(buildPath.getAbsolutePath(),classname,false);
        } catch (RunnerException e) {
            e.printStackTrace();
        } catch (SerialException e) {
            e.printStackTrace();
        }
    }

    public void setUploadPortPath(String portName) {
        this.portPath = portName;
    }

    public void setDevice(Device currentDevice) {
        Util.p("current device set to: " + currentDevice);
        Util.p("current device set to: " + currentDevice.core);
        this.device = currentDevice;
    }
}
