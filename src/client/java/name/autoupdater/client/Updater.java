package name.autoupdater.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.autoupdater.AutoUpdater;

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
        int latestVersionReturn = downloadLatestVersion(allFileNames.get(currentMod));

        if(latestVersionReturn==1){
            return 3;
        } else {
            currentMod++;
        }

        if (currentMod >= allFileNames.size()) {
            return 1;
        }

        try {
            currentProject = allFileNames.get(currentMod + 1);
        } catch (IndexOutOfBoundsException e){
            currentProject = "finished";
        }
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
        AutoUpdater.LOGGER.info("Downloading Modrinth project "+project+"...");

        if(!project.contains("IGNORE")){
            int fromLatest = 0;
            try {
                for (boolean keepLooping = true; keepLooping; ) {
                    fromLatest++;

                    PossibleFail<String> latestVersionStr = getXLatestVersion(project, fromLatest);

                    if (!latestVersionStr.failed()) {
                        JsonObject xLatestVersion = getJsonAsObj(queryAPI(latestVersionStr.data(), QueryType.VERSION).body());
                        JsonArray loaders = xLatestVersion.getAsJsonArray("loaders");
                        JsonArray versions = xLatestVersion.getAsJsonArray("game_versions");

                        keepLooping = !(jsonArrayContains(loaders, LOADER) && jsonArrayContains(versions, GAME_VERSION));// && xLatestVersion.get("version_type").getAsString().equals("release"));
                    } else {
                        return 5;
                    }
                }
                JsonObject versionFileData = getVersionFileData(getXLatestVersion(project, fromLatest).data());

                String filePathName = MOD_ROOT + "\\updated\\" + project + ".jar";

                AutoUpdater.LOGGER.info("Downloading version " + versionFileData.get("id").getAsString() + " of project "+project);

                downloadFile(versionFileData.get("url").getAsString(), filePathName);
                while (!getFileHash(filePathName).equals(versionFileData.get("hashes").getAsJsonObject().get("sha512").getAsString())) {
                    downloadFile(versionFileData.get("url").getAsString(), filePathName);
                    AutoUpdater.LOGGER.warn("Hash comparison failed for project "+project+"!");
                    AutoUpdater.LOGGER.warn("If this happens repeatedly, consider checking the mod page on Modrinth or reporting an issue on GitHub with the details of the mod being downloaded.");
                }
                //AutoUpdater.log("Downloaded "+project+"!");
            } catch (Exception e){
                e.printStackTrace();
            }
        } else {
            copyFile(MOD_ROOT+"\\"+project+".jar",MOD_ROOT+"\\updated\\"+project+".jar");
            //AutoUpdater.log("Moved "+project+"!");
        }
        return 0;
    }
    static String getFileHash(String fileName){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");

            try (InputStream in = Files.newInputStream(Path.of(fileName))) {
                byte[] buffer = new byte[8192];
                int n;

                while ((n = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, n);
                }
            }

            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
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

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(newAddress)).GET().header("User-Agent","SlightlyGoodGames/auto-updater/1.1.0").build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return new APIReturn(response.body(),response.statusCode());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    static APIReturn queryAPI(String address,QueryType queryType,Map<String,String> args){
        try {
            String newAddress = queryType.getAddress() + address + "?";
            for(String key : args.keySet()){
                newAddress += key + "=" + args.get(key) + "&";
            }
            newAddress = newAddress.substring(0, newAddress.length()-1);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(newAddress)).GET().header("User-Agent","SlightlyGoodGames/auto-updater/1.1.0").build();
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
            try {
                toReturn = projectVersions.get(projectVersions.size() - fromLatest).getAsString();
            } catch (IndexOutOfBoundsException e) {
                return new PossibleFail<>(null,true);
            }
        }

        return new PossibleFail<>(toReturn, failed);
    }
    static JsonObject getJsonAsObj(String json){
        return JsonParser.parseString(json).getAsJsonObject();
    }
    static JsonObject getVersionFileData(String version){
        String versionJson = queryAPI(version,QueryType.VERSION).body();

        JsonObject versionJsonObj = getJsonAsObj(versionJson);
        JsonArray versionFiles = versionJsonObj.getAsJsonArray("files");
        JsonObject fileObj = versionFiles.get(0).getAsJsonObject();

        return fileObj;
    }
    static void downloadFile(String url, String destination){
        try {
            Path destinationPath = Path.of(destination);
            Files.createDirectories(destinationPath.getParent());

            Files.deleteIfExists(destinationPath);

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
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}