package io.github.ecdcaeb.nilmapping.download;

import com.google.gson.JsonObject;

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
            // 设置请求头
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "*/*");

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server error: " + responseCode);
            }

            // 读取文件内容到字节数组
            try (InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }

        } finally {
            connection.disconnect();
        }
    }
}
