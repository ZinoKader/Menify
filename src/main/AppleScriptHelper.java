package main;

import com.oracle.tools.packager.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class AppleScriptHelper {

    private static final int EOF = -1;

    static String evalAppleScript(String code) {

        String[] args = { "osascript", "-e", code };

        try {
            Process process = Runtime.getRuntime().exec(args);
            process.waitFor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bigByteArray = new byte[4096];

            InputStream is = process.getInputStream();
            copyLargeStream(is, baos, bigByteArray); //write to outputstream

            String result = baos.toString().trim();

            is.close();
            baos.flush();
            baos.close();
            process.destroyForcibly();

            return result;

        } catch (IOException | InterruptedException e) {
            Log.debug(e);
            return null;
        }
    }

    private static void copyLargeStream(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        input.close();
        output.close();
    }

}
