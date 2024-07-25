package com.payhere.mqtt;

import android.util.Log;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import timber.log.Timber;


public class log {
    private static final char[] wJ = "0123456789abcdef".toCharArray();
    public static String imsi = "204046330839890";
    public static String p = "0";
    public static String keyword = "Telephone";
    public static String tranlateKeyword = "%E7%94%B5%E8%AF%9D";

    public static final String tag = "payhereCat";
    public static boolean debug = true;

    public static void l() {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();

                Log.d(tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")" + "\t" + element[1].getMethodName());

            }
        } catch (Exception ignore) {
        }

    }

    public static void d(String msg) {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();
                Log.d(tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")" + "\t" + element[1].getMethodName() + " -> " + msg);
            }
        } catch (Exception ignore) {
        }
    }

    public static void d(String _tag, String msg) {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();
                Log.d(_tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")" + "\t" + element[1].getMethodName() + " -> " + msg);
            }
        } catch (Exception ignore) {
        }
    }

    public static void e(String msg) {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();
                Log.e(tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")" + "\t" + element[1].getMethodName() + " -> " + msg);
            }
        } catch (Exception ignore) {
        }
    }

    public static void e(Throwable t) {
        log.e(t.getMessage());
    }

    public static void w(String msg) {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();
                Log.w(tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")" + "\t" + element[1].getMethodName() + " -> " + msg);
            }
        } catch (Exception ignore) {
        }
    }

    public static void i(String msg) {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();
                Log.i(tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")" + "\t" + element[1].getMethodName() + " -> " + msg);
            }
        } catch (Exception ignore) {
        }
    }

    public static void v(String msg) {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();
                Log.v(tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")" + "\t" + element[1].getMethodName() + " -> " + msg);
            }
        } catch (Exception ignore) {
        }
    }

    public static void dMqtt(String tag, String tag2, String msg, boolean isRetained) {
        try {
            if (debug) {
                Exception e = new Exception();
                StackTraceElement[] element = e.getStackTrace();
                Log.d(tag, "(" + element[1].getFileName() + ":" + element[1].getLineNumber() + ")--------------------------------------------------------------------------------------------");
                if (tag2 != null){
                    Log.d(tag, tag2);
                }
                Timber.tag(tag).d(new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(msg)));
                Log.d(tag, "isRetained: " + isRetained);
            }
        } catch (Exception ignore) {
        }
    }
}
