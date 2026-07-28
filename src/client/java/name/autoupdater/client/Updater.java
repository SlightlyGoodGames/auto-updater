package name.autoupdater.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//import name.autoupdater.AutoUpdater;

enum QueryType{
    PROJECT("https://api.modrinth.com/v2/project/"),VERSION("https://api.modrinth.com/v2/version/");

    private String address;
    QueryType(String address){
        this.address = address;
    }
    public String getAddress(){
        return this.address;
    }
}

record APIReturn(String body,int statusCode){}

record PossibleFail<T>(T data, boolean failed){}

public class Updater{
    private static final String MOD_ROOT = System.getenv("appdata")+"\\.minecraft\\mods";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String GAME_VERSION = "26.2";
    private static final String LOADER = "fabric";

    private static final Scanner SCANNER = new Scanner(System.in);

    private static int currentMod = 0;
    private static List<String> allFileNames;

    private static String currentProject;

    static void startFunction(){
        Stream<Path> allFiles = getFiles(MOD_ROOT);
        allFileNames = new ArrayList<>();
        allFiles.forEach(path -> {
            String fileName = path.getFileName().toString();
            allFileNames.add(fileName.substring(0,fileName.lastIndexOf('.')));
        });
        allFiles.close();

        currentProject = allFileNames.get(currentMod);
    }

    static String getCurrentDownloading(){
        return currentProject;
    }

    static void updateCurrentProject(String newName){
        allFileNames.set(currentMod,newName);
    }

    static int mainFunction(){
        if(downloadLatestVersion(allFileNames.get(currentMod))==1){
            return 3;
        } else {
            currentMod++;
        }

        if (currentMod >= allFileNames.size()) {
            return 1;
        }

        currentProject = allFileNames.get(currentMod);
        return 0;


        /*deleteFilesInFolder(MOD_ROOT);
        moveFiles(MOD_ROOT+"\\updated",MOD_ROOT);

        try {
            Files.delete(Path.of(MOD_ROOT + "\\updated"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Finished moving files!");*/
    }
    static int downloadLatestVersion(String project){
        if(!project.contains("IGNORE")){
            int fromLatest = 0;
            for(boolean keepLooping=true;keepLooping;){
                fromLatest++;

                PossibleFail<String> latestVersionStr = getXLatestVersion(project,fromLatest);

                if(!latestVersionStr.failed()) {
                    JsonObject xLatestVersion = getJsonAsObj(queryAPI(latestVersionStr.data(), QueryType.VERSION).body());
                    JsonArray loaders = xLatestVersion.getAsJsonArray("loaders");
                    JsonArray versions = xLatestVersion.getAsJsonArray("game_versions");

                    keepLooping = !(jsonArrayContains(loaders, LOADER) && jsonArrayContains(versions, GAME_VERSION));
                } else {
                    return 1;
                }
            }
            downloadFile(getVersionFilePath(getXLatestVersion(project,fromLatest).data()),MOD_ROOT+"\\updated\\"+project+".jar");
            //AutoUpdater.log("Downloaded "+project+"!");
        } else {
            copyFile(MOD_ROOT+"\\"+project+".jar",MOD_ROOT+"\\updated\\"+project+".jar");
            //AutoUpdater.log("Moved "+project+"!");
        }
        return 0;
    }
    static boolean jsonArrayContains(JsonArray jsonArray,String memberName){
        for(JsonElement jsonElement : jsonArray){
            if(jsonElement.getAsString().equals(memberName)){
                return true;
            }
        }
        return false;
    }
    static APIReturn queryAPI(String address,QueryType queryType){
        try {
            String newAddress = queryType.getAddress() + address;

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(newAddress)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return new APIReturn(response.body(),response.statusCode());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    static PossibleFail<String> getXLatestVersion(String project, int fromLatest){
        APIReturn apiQuery = queryAPI(project,QueryType.PROJECT);

        String projectJson = apiQuery.body();

        boolean failed = apiQuery.statusCode() != 200;

        String toReturn = "";

        if(!failed) {
            JsonObject projectJsonObj = getJsonAsObj(projectJson);
            JsonArray projectVersions = projectJsonObj.getAsJsonArray("versions");
            toReturn = projectVersions.get(projectVersions.size() - fromLatest).getAsString();
        }

        return new PossibleFail<>(toReturn, failed);
    }
    static JsonObject getJsonAsObj(String json){
        return JsonParser.parseString(json).getAsJsonObject();
    }
    static String getVersionFilePath(String version){
        String versionJson = queryAPI(version,QueryType.VERSION).body();

        JsonObject versionJsonObj = getJsonAsObj(versionJson);
        JsonArray versionFiles = versionJsonObj.getAsJsonArray("files");
        JsonObject fileObj = versionFiles.get(0).getAsJsonObject();

        return fileObj.get("url").getAsString();
    }
    static void downloadFile(String url, String destination){
        try {
            Path destinationPath = Path.of(destination);
            Files.createDirectories(destinationPath.getParent());

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofFile(destinationPath));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    static Stream<Path> getFiles(String directory){
        try {
            Stream<Path> files = Files.list(Path.of(directory));
            Stream<Path> toReturn = files.filter(Files::isRegularFile);
            return toReturn;
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
    static void deleteFilesInFolder(String directory){
        try (Stream<Path> files = Files.list(Path.of(directory))) {
            files.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    static void moveFiles(String sourceDir, String destinationDir){
        Path source = Path.of(sourceDir);
        Path destination = Path.of(destinationDir);

        try (Stream<Path> files = Files.list(source)) {
            Files.createDirectories(destination);
            files.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.move(
                                    file,
                                    destination.resolve(file.getFileName())
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    static void copyFile(String source, String destination){
        Path sourcePath = Path.of(source);
        Path destinationPath = Path.of(destination);

        try {
            Files.copy(sourcePath, destinationPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}