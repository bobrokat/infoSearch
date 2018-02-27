import java.io.*;
import java.nio.charset.StandardCharsets;

public class MyStem {

    public String stem(String str) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process process = rt.exec("./mystem -d -ln ");
        String result;

        try (InputStream in = process.getInputStream();
             OutputStream out = process.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
             writer.write(str + "\n");
             writer.flush();
             result = reader.readLine();
        }
        return result;
    }

}
