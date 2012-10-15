/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.joshondesign.arduino.common;

/**
 *
 * @author josh
 */
public class CompileException extends Exception {
    private final String desc;
    private final String compout;

    CompileException(String compiler_error, String errorString) {
        super(compiler_error);
        this.desc = compiler_error;
        this.compout = errorString;
    }
    
    public String getCompilerMessage() {
        return this.compout;
    }
    
}
