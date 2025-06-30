package io.github.ecdcaeb.nilmapping.download;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class DownloadHandler {
    public static final String URL_FABRIC = "https://github.com/FabricMC/intermediary/archive/refs/heads/master.zip";
    public static final String URL_LEGACY = "https://github.com/Legacy-Fabric/Legacy-Intermediaries/archive/refs/heads/v2.zip";

    public static void main(String[] args) {
        JsonObject jsonObject = new JsonObject();
        IntermediaryDownloader downloader = new IntermediaryDownloader(URL_FABRIC);
        for (Map.Entry<String, JsonObject> entry : downloader.download().entrySet()) {
            jsonObject.add(entry.getKey(), entry.getValue());
        }
        downloader = new IntermediaryDownloader(URL_LEGACY);
        for (Map.Entry<String, JsonObject> entry : downloader.download().entrySet()) {
            jsonObject.add(entry.getKey(), entry.getValue());
        }
        File file = new File("mapping.json");
        file.mkdir();
        try(BufferedWriter writer = Files.newBufferedWriter(file.toPath())){
            writer.append(jsonObject.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf(file.getAbsolutePath());
    }
}
