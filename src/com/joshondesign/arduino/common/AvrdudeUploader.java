package com.joshondesign.arduino.common;

/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  AvrdudeUploader - uploader implementation using avrdude
  Part of the Arduino project - http://www.arduino.cc/

  Copyright (c) 2004-05
  Hernando Barragan

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

  $Id$
*/

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class AvrdudeUploader extends Uploader  {
    File root;
    
    public AvrdudeUploader() {
    }

    public boolean uploadUsingPreferences(String buildPath, String className, boolean usingProgrammer)
            throws RunnerException, SerialException {
        this.verbose = verbose;
        //Map<String, String> boardPreferences = Base.getBoardPreferences();

        // if no protocol is specified for this board, assume it lacks a
        // bootloader and upload using the selected programmer.
        return uploadViaBootloader(buildPath, className);
    }

    
    private boolean uploadViaBootloader(String buildPath, String className)
            throws RunnerException, SerialException {
        Util.p("uploading to device using device.protocol " + this.device.getProtocol());
        //Map<String, String> boardPreferences = Base.getBoardPreferences();
        List commandDownloader = new ArrayList();
        //String protocol = boardPreferences.get("upload.protocol");
        String protocol = device.getProtocol();

        // avrdude wants "stk500v1" to distinguish it from stk500v2
        if (protocol.equals("stk500"))
            protocol = "stk500v1";

        //String uploadPort = Preferences.get("serial.port");

        /*
        // need to do a little dance for Leonardo and derivatives:
        // open then close the port at the magic baudrate (usually 1200 bps) first
        // to signal to the sketch that it should reset into bootloader. after doing
        // this wait a moment for the bootloader to enumerate. On Windows, also must
        // deal with the fact that the COM port number changes from bootloader to
        // sketch.
        if (boardPreferences.get("bootloader.path") != null && boardPreferences.get("bootloader.path").equals("caterina")) {
            String caterinaUploadPort = null;
            try {
                // Toggle 1200 bps on selected serial port to force board reset.
                List<String> before = Serial.list();
                if (before.contains(uploadPort)) {
                    if (verbose || Preferences.getBoolean("upload.verbose"))
                        System.out
                                .println(_("Forcing reset using 1200bps open/close on port ")
                                        + uploadPort);
                    Serial.touchPort(uploadPort, 1200);

                    // Scanning for available ports seems to open the port or
                    // otherwise assert DTR, which would cancel the WDT reset if
                    // it happened within 250 ms.  So we wait until the reset should
                    // have already occured before we start scanning.
                    if (!Base.isMacOS()) Thread.sleep(300);
                }

                // Wait for a port to appear on the list
                int elapsed = 0;
                while (elapsed < 10000) {
                    List<String> now = Serial.list();
                    List<String> diff = new ArrayList<String>(now);
                    diff.removeAll(before);
                    if (verbose || Preferences.getBoolean("upload.verbose")) {
                        System.out.print("PORTS {");
                        for (String p : before)
                            System.out.print(p+", ");
                        System.out.print("} / {");
                        for (String p : now)
                            System.out.print(p+", ");
                        System.out.print("} => {");
                        for (String p : diff)
                            System.out.print(p+", ");
                        System.out.println("}");
                    }
                    if (diff.size() > 0) {
                        caterinaUploadPort = diff.get(0);
                        if (verbose || Preferences.getBoolean("upload.verbose"))
                            System.out.println("Found Leonardo upload port: " + caterinaUploadPort);
                        break;
                    }

                    // Keep track of port that disappears
                    before = now;
                    Thread.sleep(250);
                    elapsed += 250;

                    // On Windows, it can take a long time for the port to disappear and
                    // come back, so use a longer time out before assuming that the selected
                    // port is the bootloader (not the sketch).
                    if (((!Base.isWindows() && elapsed >= 500) || elapsed >= 5000) && now.contains(uploadPort)) {
                        if (verbose || Preferences.getBoolean("upload.verbose"))
                            System.out.println("Uploading using selected port: " + uploadPort);
                        caterinaUploadPort = uploadPort;
                        break;
                    }
                }

                if (caterinaUploadPort == null)
                    // Something happened while detecting port
                    throw new RunnerException(
                            _("Couldn’t find a Leonardo on the selected port. Check that you have the correct port selected.  If it is correct, try pressing the board's reset button after initiating the upload."));

                uploadPort = caterinaUploadPort;
            } catch (SerialException e) {
                throw new RunnerException(e.getMessage());
            } catch (InterruptedException e) {
                throw new RunnerException(e.getMessage());
            }
        }
        */

        //String uploadPort = "/dev/tty.usbserial-A70062Gd";
        String uploadPort = this.getUploadPortPath();
        commandDownloader.add("-c" + protocol);
        commandDownloader.add("-P" + (Util.isWindows() ? "\\\\.\\" : "")
                + uploadPort);

        commandDownloader.add(
                "-b" + device.getUploadSpeed());//Integer.parseInt("19200"));
        commandDownloader.add("-D"); // don't erase
        //if (!Preferences.getBoolean("upload.verify")) commandDownloader.add("-V"); // disable verify
        commandDownloader.add("-Uflash:w:" + buildPath + File.separator + className + ".hex:i");
        /*
        if (boardPreferences.get("upload.disable_flushing") == null ||
                boardPreferences.get("upload.disable_flushing").toLowerCase().equals("false")) {
            flushSerialBuffer();
        }
        */

        boolean avrdudeResult = avrdude(commandDownloader);

        /*
        // For Leonardo wait until the bootloader serial port disconnects and the sketch serial
        // port reconnects (or timeout after a few seconds if the sketch port never comes back).
        // Doing this saves users from accidentally opening Serial Monitor on the soon-to-be-orphaned
        // bootloader port.
        if (true == avrdudeResult && boardPreferences.get("bootloader.path") != null && boardPreferences.get("bootloader.path").equals("caterina")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) { }
            long timeout = System.currentTimeMillis() + 2000;
            while (timeout > System.currentTimeMillis()) {
                List<String> portList = Serial.list();
                uploadPort = Preferences.get("serial.port");
                if (portList.contains(uploadPort)) {
                    try {
                        Thread.sleep(100); // delay to avoid port in use and invalid parameters errors
                    } catch (InterruptedException ex) { }
                    // Remove the magic baud rate (1200bps) to avoid future unwanted board resets
                    int serialRate = Preferences.getInteger("serial.debug_rate");
                    if (verbose || Preferences.getBoolean("upload.verbose"))
                        System.out.println("Setting baud rate to " + serialRate + " on " + uploadPort);
                    Serial.touchPort(uploadPort, serialRate);
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) { }
            }
        }
        */
        return avrdudeResult;
    }

    public boolean burnBootloader() throws RunnerException {
        /*
        String programmer = Preferences.get("programmer");
        Target target = Base.getTarget();
        if (programmer.indexOf(":") != -1) {
            target = Base.targetsTable.get(programmer.substring(0, programmer.indexOf(":")));
            programmer = programmer.substring(programmer.indexOf(":") + 1);
        }
        return burnBootloader(getProgrammerCommands(target, programmer));
        */
        return false;
    }

    /*
    private Collection getProgrammerCommands(Target target, String programmer) {
        Map<String, String> programmerPreferences = target.getProgrammers().get(programmer);
        List params = new ArrayList();
        params.add("-c" + programmerPreferences.get("protocol"));

        if ("usb".equals(programmerPreferences.get("communication"))) {
            params.add("-Pusb");
        } else if ("serial".equals(programmerPreferences.get("communication"))) {
            params.add("-P" + (Base.isWindows() ? "\\\\.\\" : "") + Preferences.get("serial.port"));
            if (programmerPreferences.get("speed") != null) {
                params.add("-b" + Integer.parseInt(programmerPreferences.get("speed")));
            }
        }
        // XXX: add support for specifying the port address for parallel
        // programmers, although avrdude has a default that works in most cases.

        if (programmerPreferences.get("force") != null &&
                programmerPreferences.get("force").toLowerCase().equals("true"))
            params.add("-F");

        if (programmerPreferences.get("delay") != null)
            params.add("-i" + programmerPreferences.get("delay"));

        return params;
    }
    */

    protected boolean burnBootloader(Collection params)
            throws RunnerException {
        //Map<String, String> boardPreferences = Base.getBoardPreferences();
        List fuses = new ArrayList();
        fuses.add("-e"); // erase the chip

        //if (boardPreferences.get("bootloader.unlock_bits") != null)
            fuses.add("-Ulock:w:" + "0x3F" + ":m");
        //if (boardPreferences.get("bootloader.extended_fuses") != null)
            fuses.add("-Uefuse:w:" + "0x00" + ":m");
        fuses.add("-Uhfuse:w:" + "0xdd" + ":m");
        fuses.add("-Ulfuse:w:" + "0xff" + ":m");

        if (!avrdude(params, fuses))
            return false;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        //Target t;
        List bootloader = new ArrayList();
        //String bootloaderPath = boardPreferences.get("bootloader.path");
        String bootloaderPath = "atmega";

        if (bootloaderPath != null) {
            /*
            if (bootloaderPath.indexOf(':') == -1) {
                t = Base.getTarget(); // the current target (associated with the board)
            } else {
                String targetName = bootloaderPath.substring(0, bootloaderPath.indexOf(':'));
                t = Base.targetsTable.get(targetName);
                bootloaderPath = bootloaderPath.substring(bootloaderPath.indexOf(':') + 1);
            }
            */

            //File bootloadersFile = new File(t.getFolder(), "bootloaders");
            //File bootloaderFile = new File(bootloadersFile, bootloaderPath);
            //bootloaderPath = bootloaderFile.getAbsolutePath();
            bootloaderPath = new File(root,"/hardware/arduino/bootloaders/atmega").getAbsolutePath();

            bootloader.add("-Uflash:w:" + new File(bootloaderPath,"ATmegaBOOT_168_diecimila.hex").getAbsolutePath() + ":i");
            //bootloader.add("-Uflash:w:" + bootloaderPath + File.separator +
            //                    boardPreferences.get("bootloader.file") + ":i");
        }
        //if (boardPreferences.get("bootloader.lock_bits") != null)
            bootloader.add("-Ulock:w:" +"0x0F" + ":m");

        if (bootloader.size() > 0)
            return avrdude(params, bootloader);

        return true;
    }

    public boolean avrdude(Collection p1, Collection p2) throws RunnerException {
        ArrayList p = new ArrayList(p1);
        p.addAll(p2);
        return avrdude(p);
    }

    public boolean avrdude(Collection params) throws RunnerException {
        List commandDownloader = new ArrayList();

        File hardwarePath = new File(root,"hardware");


        if(Util.isLinux()) {
            File avrdude = new File(hardwarePath,"tools/avrdude"); 
            if (avrdude.exists()) {
                commandDownloader.add(avrdude);
                commandDownloader.add("-C" + new File(hardwarePath,"tools/avrdude.conf").getAbsolutePath());
            } else {
                commandDownloader.add("avrdude");
            }
        }
        else {
            //mac and window?
            File avrdude = new File(hardwarePath,"tools/avr/bin/avrdude");
            File conf = new File(hardwarePath,"tools/avr/etc/avrdude.conf");
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

}