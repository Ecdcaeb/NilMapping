package io.github.ecdcaeb.nilmapping.download;

import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public interface IMappingDownloader {

    /*
    META-INF/nil/mappings.json
       String -> JsonObject
            classes -> JsonArray -> JsonObject
                                from -> String<className>
                                to -> String<className>
                                methods -> JsonObject
                                        String:from<name+desc> -> String:to<name+desc>
                                fields -> JsonObject
                                        String:from<name+desc> -> String:to<name+desc>
                                inner-classes -> JsonArray -> JsonObject -> ...<class>

    */


    Map<String, JsonObject> download();

    static byte[] url2bytes(String url) throws IOException, URISyntaxException {
        URL fileUrl = new URI(url).toURL();
        HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();

        try {
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "*/*");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        String errorMessage = new String(readAll(errorStream));
                        throw new IOException("Server error " + responseCode + ": " + errorMessage);
                    }
                }
                throw new IOException("Server error: " + responseCode);
            }
            
            try (InputStream in = connection.getInputStream()) {
                return readAll(in);
            }

        } finally {
            connection.disconnect();
        }
    }

    static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        
        return buffer.toByteArray();
    }
}