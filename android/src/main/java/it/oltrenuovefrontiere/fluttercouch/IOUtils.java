package it.oltrenuovefrontiere.fluttercouch;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    private IOUtils(){

    }

    public static void inputStreamToOutputStream(InputStream is, OutputStream os) throws IOException {
        int read;
        byte[] bytes = new byte[8192];
        while ((read = is.read(bytes)) != -1) {
            os.write(bytes, 0, read);
        }
    }

    public static void closeSafe(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //do nothing;
            }
        }
    }
}
