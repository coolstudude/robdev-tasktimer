/*
 * TextUtilities.java
 *
 * Created on 01 October 2006, 09:25
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.stu.task.util;

/**
 * *** TEST ***
 * @author Stuart
 */
public class TextUtilities {
    public static final int PAD_LEFT  = 1;
    public static final int PAD_RIGHT = 2;
    
    /**
     * Pads the specified value with the specified char and to the specified alignment. Pretty cool.
     * @return the padded result
     * @param value the fieldValue to be padded
     * @param padWith the char to pad with
     * @param alignment the alignment for the final result
     * @param size the ultimate size of the result
     */
    public static String pad(String value, char padWith, int alignment, int size) {
        char[] padArray = new char[size];
        for (int i = 0; i < padArray.length; i++) {
            padArray[i] = padWith;
        }

        if (value == null) {
            value = "";
        }
        String pad = new String(padArray);
        String result = null;
        switch (alignment) {
            case 1: {
                value = pad + value; // left
                result = value.substring(value.length() - pad.length());
                break;
            }
            case 2: {
                value = value + pad; // right
                result = value.substring(0, pad.length());
                break;
            }
            default: {
                value = pad + value; // left
                result = value.substring(value.length() - pad.length());
                break;
            }
        }

        return result;
    }
    
    /**
     * Returns the time supplied in the HH:MM:ss.mmm format
     * @param timeInMillis the time in ms
     * @return the formatted String
     */
    public static String formatTimeInHHMMss(long timeInMillis) {
        long multiplier = 1000 * 60 * 60;

        // e.g. timeInMillis = 3700000;

        long hrs = timeInMillis / multiplier; // =1
        double hrsMod = timeInMillis % multiplier; // =100000 equiv to (100000/3600000)

        double minsDbl = (hrsMod / multiplier) * 60; // (100000/3600000)*60
        long mins = (long) minsDbl;
        double minsDec = minsDbl - mins;

        double secs = minsDec * 60;
        String secsStr = String.valueOf(secs);
        int decIndex = secsStr.indexOf(".");

        String secsPadInt = TextUtilities.pad(secsStr.substring(0, decIndex), '0', TextUtilities.PAD_LEFT, 2);
        String secsPadDecs = TextUtilities.pad(secsStr.substring(decIndex + 1), '0', TextUtilities.PAD_RIGHT, 3); // 3
                                                                                                                    // decimal
                                                                                                                    // places

        secsStr = secsPadInt + "." + secsPadDecs;

        return TextUtilities.pad(String.valueOf(hrs), '0', TextUtilities.PAD_LEFT, 2) + ":"
                + TextUtilities.pad(String.valueOf(mins), '0', TextUtilities.PAD_LEFT, 2) + ":"
                + TextUtilities.pad(secsStr, '0', TextUtilities.PAD_LEFT, 6);
    }
    
    /**
     * @param timeHHMMss
     * @return
     */
    public static long formatTimeInMillis(String timeHHMMss) {
        String[] split = timeHHMMss.split(":");
        if (timeHHMMss.length() != 8 && split.length != 3) {
            throw new RuntimeException("Invalid time String format");
        }
        long t = Long.parseLong(split[0]) * 60 * 60 * 1000;
        t += Long.parseLong(split[1]) * 60 * 1000;
        t += Long.parseLong(split[2]) * 1000;

        return t;
    }
}
