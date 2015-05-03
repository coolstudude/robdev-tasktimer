/*
 * Task.java
 *
 * Created on 16 September 2006, 06:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.stu.task;

import java.util.StringTokenizer;

import org.stu.task.util.TextUtilities;

/**
 * A class which contains a Timer
 * @author Stuart
 */
public class Task {
    
    private String name;
    private int id;
    
    private Timer timer;
    
    /* main method */
    public static void main(String[] args) throws Exception {
        Task task = new Task("Test 1", 1);

        task.start();
        
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ie) {/* do nothing */ }
        System.out.println("Time : " + task.getTimeInHHMMss());
        
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ie) {/* do nothing */ }
        System.out.println("Time : " + task.getTime());

        task.stop();
    
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {/* do nothing */ }
        System.out.println("Time : " + task.getTime());
        
        task.start();
        System.out.println("Restart time : " + task.getTime());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {/* do nothing */ }
        System.out.println("Time : " + task.getTime());
        
    }
    
    public boolean equals(Object obj) {
        return this.getName().equals( obj != null ? ((Task)obj).getName() : obj);
    }
    
    /** Creates a new instance of Task */
    public Task(String name, int number) {
        this.name = name;
        this.id = number;
        this.timer = new Timer();
    }
    
    public void start() {
        timer.start();
    }
    
    public void stop() {
        timer.stop();
    }
    
    public long getTime() {
        return timer.getElapsedTimeMillis();
    }
    
    public long getTimeInMillis() {
        return timer.getElapsedTimeMillis();
    }
    
    public String getTimeInHHMMss() {
        return TextUtilities.formatTimeInHHMMss(timer.getElapsedTimeMillis());
    }
    
    public void setTimeInMillis(long time) {
        timer.setElapsedTimeMillis(time);
    }
    
    public void setTimeInHHMMss(String time) {
        StringTokenizer st = new StringTokenizer(time, ":");
        if (st.countTokens() != 3) {
            throw new RuntimeException("Invalid time - not in format HH:MM:ss");
        }
        String hrStr = st.nextToken();
        String minStr = st.nextToken();
        String sStr = st.nextToken();
        
        long t = Long.parseLong(hrStr) * 60 * 60;
        t += Long.parseLong(minStr) * 60;
        t += Long.parseLong(sStr);
        
        timer.setElapsedTimeMillis(t * 1000);
    }
    
    public void reset() {
        timer.reset();
    }

    public String getName() {
        return name;
    }
    
    public int getId() {
        return id;
    }
    
    public boolean isStarted() {
        return timer.isStarted();
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
