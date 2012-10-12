package com.joshondesign.arduino.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessageSiphon implements Runnable {
    BufferedReader streamReader;
    Thread thread;
    MessageConsumer consumer;


    public MessageSiphon(InputStream stream, MessageConsumer consumer) {
        this.streamReader = new BufferedReader(new InputStreamReader(stream));
        this.consumer = consumer;

        thread = new Thread(this);
        // don't set priority too low, otherwise exceptions won't
        // bubble up in time (i.e. compile errors have a weird delay)
        //thread.setPriority(Thread.MIN_PRIORITY);
        thread.setPriority(Thread.MAX_PRIORITY-1);
        thread.start();
    }


    public void run() {
        try {
            // process data until we hit EOF; this will happily block
            // (effectively sleeping the thread) until new data comes in.
            // when the program is finally done, null will come through.
            //
            String currentLine;
            while ((currentLine = streamReader.readLine()) != null) {
                // \n is added again because readLine() strips it out
                //EditorConsole.systemOut.println("messaging in");
                consumer.message(currentLine + "\n");
                //EditorConsole.systemOut.println("messaging out");
            }
            //EditorConsole.systemOut.println("messaging thread done");
            thread = null;

        } catch (NullPointerException npe) {
            // Fairly common exception during shutdown
            thread = null;

        } catch (Exception e) {
            // On Linux and sometimes on Mac OS X, a "bad file descriptor"
            // message comes up when closing an applet that's run externally.
            // That message just gets supressed here..
            String mess = e.getMessage();
            if ((mess != null) &&
                    (mess.indexOf("Bad file descriptor") != -1)) {
                //if (e.getMessage().indexOf("Bad file descriptor") == -1) {
                //System.err.println("MessageSiphon err " + e);
                //e.printStackTrace();
            } else {
                e.printStackTrace();
            }
            thread = null;
        }
    }

    // Wait until the MessageSiphon thread is complete.
    public void join() throws java.lang.InterruptedException {
        // Grab a temp copy in case another thread nulls the "thread"
        // member variable
        Thread t = thread;
        if (t != null) t.join();
    }

    public Thread getThread() {
        return thread;
    }
}