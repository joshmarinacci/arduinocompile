package com.joshondesign.arduino.common;

import java.util.HashMap;
import java.util.Map;

public class Preferences {
    static Map<String,String> map = new HashMap<String,String>();
    static {
        //map.put()
    }
    public static String get(String s) {
        return map.get(s);
    }

    public static int getInteger(String s) {
        if(!map.containsKey(s)) return 0;
        return Integer.parseInt(get(s));
    }
}
