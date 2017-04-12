package main;

import com.oracle.tools.packager.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AppleScriptHelper {

    private static final int EOF = -1;

    String evalAppleScript(String code) {
        Runtime runtime = Runtime.getRuntime();
        String[] args = { "osascript", "-e", code };

        try {
            Process process = runtime.exec(args);
            process.waitFor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStream is = process.getInputStream();
            copyLargeStream(is, baos, new byte[4096]); //write to outputstream
            return baos.toString().trim();

        } catch (IOException | InterruptedException e) {
            Log.debug(e);
            return null;
        }
    }

    private void copyLargeStream(InputStream input, OutputStream output, byte[] buffer) throws IOException {

        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
    }

}
