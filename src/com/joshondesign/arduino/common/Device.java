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
    
    
}
