package com.voxlearning.androidtcpdumpgui.util;


import android.content.Context;
import android.util.Log;

import com.voxlearning.androidtcpdumpgui.CaptureFilePath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Io {

    static private final String TAG = "Io";

    static public void close(Closeable c) {
        if (c == null)
            return;

        try {
            c.close();
        } catch (IOException ignored) {
        }
    }


    static public String readAll(File f) {
        FileInputStream fis = null;

        try {
            long size = f.length();
            if(size < 0) return null;
            if(size == 0) return "";

            byte []buf = new byte[(int)size];
            fis = new FileInputStream(f);
            if(fis.read(buf) != size)
                return null;

            return new String(buf, "UTF-8");
        }
        catch (Exception e) {
            return null;
        }
        finally {
            close(fis);
        }
    }

    static public String readAll(String fn) {
        return readAll(new File(fn));
    }

    static public void writeFile(String fn, String s) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fn);
            fos.write(s.getBytes());
        } catch (Exception ignored) {
        } finally {
            close(fos);
        }
    }

    static public void writeFileAppend(String fn, String s) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fn, true);
            fos.write(s.getBytes());
        } catch (Exception ignored) {
        } finally {
            close(fos);
        }
    }

    static public boolean deleteFollowSymlink(String f) {
        return deleteFollowSymlink(new File(f));
    }

    static public boolean deleteFollowSymlink(File f) {
        if (f.isDirectory()) {
            File []files = f.listFiles();
            if(files != null) {
                for (File c : f.listFiles()) {
                    deleteFollowSymlink(c);
                }
            }
        }
        return f.delete();
    }

    static public boolean extractAssetExecutable(Context context, String assetFilePath, String outFilePath) {
        if(extractAssetFile(context, assetFilePath, outFilePath)) {

            String fullFilePath = outFilePath;
            if (!fullFilePath.startsWith("/")) {
                fullFilePath = CaptureFilePath.AppFilePath(context, outFilePath);
            }

            return new File(fullFilePath).setExecutable(true, false);

        }
        return false;
    }

    static public boolean extractAssetFile(Context context, String assetFilePath, String outFilePath) {
        InputStream is = null;
        FileOutputStream fos = null;

        Log.i(TAG, "extracting " + assetFilePath + " to " + outFilePath + " ...");
        try {
            is = context.getAssets().open(assetFilePath);
            if (outFilePath.startsWith("/")) {
                fos = new FileOutputStream(outFilePath);
            } else {
                fos = context.openFileOutput(outFilePath, Context.MODE_PRIVATE);
            }
            byte[] buffer = new byte[4096];
            int len;
            while (-1 != (len = is.read(buffer))) {
                fos.write(buffer, 0, len);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "can not extract " + assetFilePath + " to " + outFilePath, e);
            return false;
        } finally {
            Io.close(is);
            Io.close(fos);
        }
    }

    static public boolean zip(String[] files, String zipFile) {
        ZipOutputStream out = null;
        int BUFFER_SIZE = 4096;
        byte data[] = new byte[BUFFER_SIZE];

        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            for (int i = 0; i < files.length; i++) {

                File f = new File(files[i]);

                //skip non-existing files
                if(!f.exists())
                    continue;

                FileInputStream fi = null;
                try {
                    fi = new FileInputStream(f);
                    BufferedInputStream origin = new BufferedInputStream(fi, BUFFER_SIZE);
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);

                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    close(fi);
                }
            }
            return true;
        }
        catch (IOException e) {
            Log.e(TAG, "can not zip files: " + e.getMessage(), e);
            return false;
        }
        finally {
            close(out);
        }
    }
}
