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
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.autoupdater.AutoUpdater;

public class Updater{
    private static final String MOD_ROOT = System.getenv("appdata")+"\\.minecraft\\mods";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String GAME_VERSION = "26.2";
    private static final String LOADER = "fabric";

    private static final String USER_AGENT = "SlightlyGoodGames/auto-updater/"+AutoUpdater.MOD_VERSION;

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

    static int mainFunction() throws IOException,InterruptedException{
        String currentProjectPath = MOD_ROOT + "\\" + currentProject + ".jar";
        String projectHash = getFileHash(currentProjectPath);
        HttpResponse<String> latestVersionResponse = getLatestVersionFromHash(projectHash);

        if(latestVersionResponse.statusCode() == 200){
            JsonObject latestVersionObj = JsonParser.parseString(latestVersionResponse.body()).getAsJsonObject();
            JsonObject versionFileData = latestVersionObj.get("files").getAsJsonArray().get(0).getAsJsonObject();
            String versionFileUrl = versionFileData.get("url").getAsString();
            String versionFileHash = versionFileData.get("hashes").getAsJsonObject().get("sha512").getAsString();

            String filePathName = MOD_ROOT + "\\updated\\" + currentProject + ".jar";

            AutoUpdater.LOGGER.info("Downloading version {} of project {}...",versionFileData.get("id").getAsString(),currentProject);

            downloadFile(versionFileUrl, filePathName);
            while (!getFileHash(filePathName).equals(versionFileHash)) {
                downloadFile(versionFileData.get("url").getAsString(), filePathName);
                AutoUpdater.LOGGER.warn("Hash comparison failed for project {}!",currentProject);
                AutoUpdater.LOGGER.warn("If this happens repeatedly, consider checking the mod page on Modrinth or reporting an issue on GitHub with the details of the mod being downloaded.");
            }
        } else {
            return 3;
        }

        currentMod++;

        try {
            currentProject = allFileNames.get(currentMod + 1);
        } catch (IndexOutOfBoundsException e) {
            return 1;
        }
        return 0;
    }

    static HttpResponse<String> getLatestVersionFromHash(String hash) throws IOException,InterruptedException{
        String address = "https://api.modrinth.com/v2/version_file/" + hash + "/update?";
        String httpArgs = "algorithm=sha512";
        String fullAddress = address + httpArgs;

        String JSON = """
                {
                    "loaders":["%s"],
                    "game_versions":["%s"]
                }
                """.formatted(LOADER,GAME_VERSION);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullAddress)).POST(
                HttpRequest.BodyPublishers.ofString(JSON)
        ).header("User-Agent",USER_AGENT).header("Content-Type", "application/json").build();

        return httpClient.send(request,HttpResponse.BodyHandlers.ofString());
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
    static void moveFile(String source, String destination){
        Path sourcePath = Path.of(source);
        Path destinationPath = Path.of(destination);
        try {
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}