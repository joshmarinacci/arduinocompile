/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduino.common;

/**
 *
 * @author josh
 */
public class Device {
    public String name;
    public String protocol;
    public int maximum_size;
    public int low_fuses;
    public int high_fuses;
    public int extended_fuses;
    public String path;
    public String file;
    public int unlock_bits;
    public int lock_bits;
    public String mcu;
    public String f_cpu;
    public String core;
    public String variant;
    public Device compatible;
    public int upload_speed;
    public String vid = "";
    public String pid = "";
    public boolean disable_flushing;

    String getCore() {
        if(core == null) {
            return compatible.core;
        }
        return core;
    }

    String getVariant() {
        if(variant == null) return compatible.variant;
        return variant;
    }

    String getMCU() {
        if(mcu == null) return compatible.mcu;
        return mcu;
    }

    String getFCPU() {
        if(f_cpu == null) return compatible.f_cpu;
        return f_cpu;
    }

    String getVID() {
        return vid;
    }
    
    String getPID() {
        return pid;
    }        

    String getProtocol() {
        if(protocol == null) return compatible.protocol;
        return protocol;
    }

    int getUploadSpeed() {
        if(upload_speed == 0) return compatible.upload_speed;
        return upload_speed;
    }
    
    
}
