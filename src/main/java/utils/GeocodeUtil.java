package utils;

import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.net.URL;
import java.net.HttpURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class GeocodeUtil {

    // Renvoie {latitude, longitude} pour un lieu
    public static double[] getCoordinates(String lieu) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(lieu, "UTF-8")
                    + "&format=json&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "JavaFXApp");
            conn.connect();

            JsonArray arr = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonArray();
            if (arr.size() > 0) {
                double lat = arr.get(0).getAsJsonObject().get("lat").getAsDouble();
                double lon = arr.get(0).getAsJsonObject().get("lon").getAsDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}