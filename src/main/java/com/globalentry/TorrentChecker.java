package com.globalentry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class TorrentChecker {

    private static final String QB_URL = "http://localhost:8080"; // Replace with your qBittorrent Web UI URL
    private static final String USERNAME = "admin"; // Replace with your qBittorrent Web UI username
    private static final String PASSWORD = "password"; // Replace with your qBittorrent Web UI password

    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.schedule(new TorrentCheckTask(), 0, 10 * 60 * 1000);
    }

    static class TorrentCheckTask extends TimerTask {
        @Override
        public void run() {
            try {
                JsonArray torrents = fetchTorrents();

                if (torrents != null) {
                    for (JsonObject torrent : torrents.getValuesAs(JsonObject.class)) {
                        double progress = torrent.getJsonNumber("progress").doubleValue();

                        if (progress >= 0.99) {
                            String hash = torrent.getString("hash");
                            forceRecheck(hash);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error fetching or rechecking torrents: " + e.getMessage());
            }
        }

        private JsonArray fetchTorrents() throws IOException {
            URL url = new URL(QB_URL + "/api/v2/torrents/info");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String auth = USERNAME + ":" + PASSWORD;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (JsonReader reader = Json.createReader(connection.getInputStream())) {
                    return reader.readArray();
                }
            } else {
                System.err.println("Failed to fetch torrents. HTTP response code: " + connection.getResponseCode());
                return null;
            }
        }

        private void forceRecheck(String hash) throws IOException {
            URL url = new URL(QB_URL + "/api/v2/torrents/recheck?hashes=" + hash);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            String auth = USERNAME + ":" + PASSWORD;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to force recheck torrent. HTTP response code: " + connection.getResponseCode());
            }
        }
    }
}
