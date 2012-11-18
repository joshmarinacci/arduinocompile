/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduino.common;

/**
 *
 * @author josh
 */
public interface OutputListener {
    public void log(String string);
    public void exec(String string);
    public void stdout(String string);
    public void stderr(String string);
    public void clear();
    
}
