package services.AccompagnementService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class QuoteService {

    private String content;
    private String author;

    public void fetchQuote() {
        try {
            URL url = new URL("https://zenquotes.io/api/random");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONArray jsonArray = new JSONArray(response.toString());
            JSONObject jsonObject = jsonArray.getJSONObject(0);

            content = jsonObject.getString("q");
            author = jsonObject.getString("a");

        } catch (Exception e) {
            e.printStackTrace();
            content = "Reste positif et continue 💪";
            author = "System";
        }
    }

    public String getContent() { return content; }
    public String getAuthor() { return author; }
}