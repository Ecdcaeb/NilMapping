package io.github.ecdcaeb.nilmapping.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IntermediaryDownloader implements IMappingDownloader {

    public static final String URL = "https://github.com/FabricMC/intermediary/archive/refs/heads/master.zip";

    protected final String url;

    public IntermediaryDownloader(String url) {
        this.url = url;
    }

    @Override
    public Map<String, JsonObject> download() {
        HashMap<String, JsonObject> map = new HashMap<>();
        try {
            System.out.printf("Downloading mapping:" + this.url);
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(IMappingDownloader.url2bytes(this.url)))) {
                System.out.printf("Downloaded mapping:" + this.url);
                ZipEntry zipEntry;

                while ((zipEntry = zis.getNextEntry()) != null) {
                    if (isValidMappingEntry(zipEntry)) {
                        String version = extractVersion(zipEntry.getName());
                        System.out.printf("Find mapping:" + version);
                        byte[] data = getFromEntry(zis);
                        MemoryMappingTree memoryMappingTree = new MemoryMappingTree();
                        MappingReader.read(new InputStreamReader(new ByteArrayInputStream(data)), MappingFormat.TINY_2_FILE, memoryMappingTree);
                        String dst = memoryMappingTree.getDstNamespaces().get(0);
                        HashMap<String, JsonObject> classes = new HashMap<>();
                        HashMap<String, JsonObject> childs = new HashMap<>();
                        for (MappingTreeView.ClassMappingView classMapping : memoryMappingTree.getClasses()) {
                            if (classMapping.getSrcName().indexOf('$') == -1) {
                                classes.put(classMapping.getSrcName(), createClass(classMapping, dst));
                            } else childs.put(classMapping.getSrcName(), createClass(classMapping, dst));
                        }
                        for (Map.Entry<String, JsonObject> clsEntry : childs.entrySet()) {
                            String parent = clsEntry.getKey().substring(0, clsEntry.getKey().lastIndexOf('$'));
                            if (childs.containsKey(parent)) {
                                JsonObject parentObj = childs.get(parent);
                                if (!parentObj.has("inner-classes")) {
                                    parentObj.add("inner-classes", new JsonArray());
                                }
                                parentObj.getAsJsonArray("inner-classes").add(clsEntry.getValue());
                            } else if (classes.containsKey(parent)) {
                                JsonObject parentObj = classes.get(parent);
                                if (!parentObj.has("inner-classes")) {
                                    parentObj.add("inner-classes", new JsonArray());
                                }
                                parentObj.getAsJsonArray("inner-classes").add(clsEntry.getValue());
                            }
                        }
                        JsonObject result = new JsonObject();
                        result.add("classes", classes.values().stream().collect(Collector.of(JsonArray::new, JsonArray::add, (array, array2) -> array.size() < array2.size() ? array2 : array, Collector.Characteristics.IDENTITY_FINISH)));
                        map.put(version, result);
                    }
                    zis.closeEntry();
                }
            } catch (Throwable t) {
                System.out.printf(t.toString());
            }
        } catch (Throwable e) {
            System.out.printf(e.toString());
        }
        return map;
    }

    private static JsonObject createClass(MappingTreeView.ClassMappingView classMappingView, String dst) {
        String name = classMappingView.getSrcName();
        String dstName = classMappingView.getDstName(0);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("from", name);
        jsonObject.addProperty("to", dstName);
        if (!classMappingView.getFields().isEmpty()) {
            JsonObject fieldArray;
            jsonObject.add("methods", fieldArray = new JsonObject());
            for (MappingTreeView.FieldMappingView fieldMappingView : classMappingView.getFields()) {
                fieldArray.addProperty(fieldMappingView.getSrcName() + fieldMappingView.getSrcDesc(), fieldMappingView.getName(0) + fieldMappingView.getDesc(0));
            }
        }
        if (!classMappingView.getMethods().isEmpty()) {
            JsonObject fieldArray;
            jsonObject.add("methods", fieldArray = new JsonObject());
            for (MappingTreeView.MethodMappingView fieldMappingView : classMappingView.getMethods()) {
                fieldArray.addProperty(fieldMappingView.getSrcName() + fieldMappingView.getSrcDesc(), fieldMappingView.getName(0) + fieldMappingView.getDesc(0));
            }
        }
        return jsonObject;
    }

    private static byte[] getFromEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = zis.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private static boolean isValidMappingEntry(ZipEntry entry) {
        return entry.getName().startsWith("mappings/")
                && entry.getName().lastIndexOf('/') == 8
                && !entry.isDirectory()
                && entry.getName().endsWith(".tiny");
    }

    private static String extractVersion(String fileName) {
        String mappingName = fileName.substring(9); // 去掉"mappings/"前缀
        return mappingName.substring(0, mappingName.lastIndexOf('.'));
    }

}
