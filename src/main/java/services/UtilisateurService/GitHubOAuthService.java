package services.UtilisateurService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GitHub OAuth2 Service for BoostUp Desktop Application.
 *
 * SETUP (2 minutes):
 * 1. Go to https://github.com/settings/developers
 * 2. Click "New OAuth App"
 * 3. Fill in:
 *    - Application name: BoostUp
 *    - Homepage URL: http://localhost
 *    - Authorization callback URL: http://127.0.0.1/callback
 * 4. Click "Register application"
 * 5. Copy Client ID and generate a Client Secret
 * 6. Paste them below
 *
 * Any GitHub account can login immediately (no review needed).
 */
public class GitHubOAuthService {

    private static final Logger LOGGER = Logger.getLogger(GitHubOAuthService.class.getName());

    // ------------------------------------------------------------------
    // GITHUB OAUTH2 CREDENTIALS
    // Create at: https://github.com/settings/developers
    // ------------------------------------------------------------------
    private static final String CLIENT_ID = "Ov23li85odugIHBMrAVr";
    private static final String CLIENT_SECRET = "cbac9ce87baba22d1119296e7135088472399d1b";

    // GitHub OAuth2 endpoints
    private static final String AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USERINFO_URL = "https://api.github.com/user";
    private static final String EMAILS_URL = "https://api.github.com/user/emails";

    private static final int TIMEOUT_SECONDS = 120;

    public static boolean isConfigured() {
        return CLIENT_ID != null && !CLIENT_ID.isEmpty()
                && !CLIENT_ID.equals("YOUR_GITHUB_CLIENT_ID")
                && CLIENT_SECRET != null && !CLIENT_SECRET.isEmpty()
                && !CLIENT_SECRET.equals("YOUR_GITHUB_CLIENT_SECRET");
    }

    /**
     * Starts the full GitHub OAuth2 flow.
     */
    public static CompletableFuture<GitHubUserInfo> login() {
        CompletableFuture<GitHubUserInfo> future = new CompletableFuture<>();

        new Thread(() -> {
            HttpServer server = null;
            try {
                // Step 1: Start local server on dynamic port
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                int port = server.getAddress().getPort();
                String redirectUri = "http://127.0.0.1:" + port + "/callback";

                LOGGER.info("GitHub callback server on port " + port);
                System.out.println("[GitHubOAuth] Redirect URI: " + redirectUri);

                CompletableFuture<String> codeFuture = new CompletableFuture<>();

                server.createContext("/callback", exchange -> {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        String code = null;
                        String error = null;

                        if (query != null) {
                            for (String param : query.split("&")) {
                                String[] pair = param.split("=", 2);
                                if (pair.length == 2) {
                                    if ("code".equals(pair[0]))
                                        code = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                                    if ("error".equals(pair[0]))
                                        error = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                                }
                            }
                        }

                        String html = (code != null) ? getSuccessHtml() : getErrorHtml(error != null ? error : "Login cancelled");
                        byte[] response = html.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.getResponseBody().close();

                        if (code != null) {
                            codeFuture.complete(code);
                        } else {
                            codeFuture.complete(null);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error handling GitHub callback", e);
                        codeFuture.complete(null);
                    }
                });

                // Handle favicon
                server.createContext("/", exchange -> {
                    try {
                        exchange.sendResponseHeaders(404, 0);
                        exchange.getResponseBody().close();
                    } catch (Exception ignored) {}
                });

                server.setExecutor(null);
                server.start();

                // Step 2: Open browser
                openBrowser(redirectUri);

                // Step 3: Wait for code
                String authCode = codeFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                server.stop(1);
                server = null;

                if (authCode == null) {
                    future.completeExceptionally(new Exception("Authentification GitHub annulee"));
                    return;
                }

                LOGGER.info("GitHub authorization code received");

                // Step 4: Exchange code for token
                String accessToken = exchangeCodeForToken(authCode, redirectUri);
                if (accessToken == null) {
                    future.completeExceptionally(new Exception("Impossible d'obtenir le token GitHub"));
                    return;
                }

                // Step 5: Fetch user info
                GitHubUserInfo userInfo = fetchUserInfo(accessToken);
                if (userInfo == null) {
                    future.completeExceptionally(new Exception("Impossible de recuperer le profil GitHub"));
                    return;
                }

                // Step 6: If email is null, fetch from /user/emails endpoint
                if (userInfo.email == null || userInfo.email.isEmpty()) {
                    String email = fetchPrimaryEmail(accessToken);
                    userInfo.email = email;
                }

                if (userInfo.email == null || userInfo.email.isEmpty()) {
                    future.completeExceptionally(new Exception("Email GitHub non disponible. Verifiez vos parametres de confidentialite GitHub."));
                    return;
                }

                LOGGER.info("GitHub login successful: " + userInfo.email);
                future.complete(userInfo);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "GitHub OAuth error", e);
                future.completeExceptionally(e);
            } finally {
                if (server != null) {
                    try { server.stop(0); } catch (Exception ignored) {}
                }
            }
        }, "github-oauth-thread").start();

        return future;
    }

    private static void openBrowser(String redirectUri) throws Exception {
        String url = AUTH_URL
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=read:user,user:email"
                + "&allow_signup=true";

        System.out.println("[GitHubOAuth] Opening browser...");

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            new ProcessBuilder("cmd", "/c", "start", "\"\"", url).start();
        }
    }

    private static String exchangeCodeForToken(String code, String redirectUri) {
        try {
            String params = "client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

            HttpURLConnection conn = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (responseCode != 200) {
                LOGGER.severe("GitHub token exchange failed (" + responseCode + "): " + body);
                return null;
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                LOGGER.severe("GitHub token error: " + json.get("error").getAsString());
                return null;
            }
            return json.get("access_token").getAsString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exchanging GitHub code for token", e);
            return null;
        }
    }

    private static GitHubUserInfo fetchUserInfo(String accessToken) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(USERINFO_URL).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (responseCode != 200) {
                LOGGER.severe("GitHub user info failed (" + responseCode + "): " + body);
                return null;
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            GitHubUserInfo info = new GitHubUserInfo();
            info.id = getStr(json, "id") != null ? getStr(json, "id") : String.valueOf(json.get("id").getAsLong());
            info.login = getStr(json, "login");
            info.name = getStr(json, "name");
            info.email = getStr(json, "email");
            info.avatarUrl = getStr(json, "avatar_url");
            info.bio = getStr(json, "bio");
            return info;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching GitHub user info", e);
            return null;
        }
    }

    /**
     * Fetch primary email from GitHub /user/emails (if email is private).
     */
    private static String fetchPrimaryEmail(String accessToken) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(EMAILS_URL).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return null;

            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonArray emails = JsonParser.parseString(body).getAsJsonArray();

            // Find primary email
            for (int i = 0; i < emails.size(); i++) {
                JsonObject emailObj = emails.get(i).getAsJsonObject();
                if (emailObj.has("primary") && emailObj.get("primary").getAsBoolean()) {
                    return emailObj.get("email").getAsString();
                }
            }

            // Fallback: return first email
            if (emails.size() > 0) {
                return emails.get(0).getAsJsonObject().get("email").getAsString();
            }

            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not fetch GitHub emails", e);
            return null;
        }
    }

    private static String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    // ------------------------------------------------------------------
    // HTML pages
    // ------------------------------------------------------------------

    private static String getSuccessHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>BoostUp</title>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family:'Segoe UI',system-ui,sans-serif; "
                + "background:linear-gradient(135deg,#1b2a4a,#2d1b4e); "
                + "min-height:100vh; display:flex; align-items:center; justify-content:center; }"
                + ".card { background:white; border-radius:24px; padding:60px 50px; "
                + "text-align:center; max-width:480px; width:90%; "
                + "box-shadow:0 25px 60px rgba(0,0,0,0.3); }"
                + ".icon { font-size:64px; margin-bottom:20px; }"
                + "h1 { color:#24292f; font-size:26px; font-weight:800; margin-bottom:12px; }"
                + "p { color:#6c757d; font-size:15px; line-height:1.6; }"
                + ".hint { margin-top:20px; color:#adb5bd; font-size:12px; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='icon'>&#10004;</div>"
                + "<h1>Connexion GitHub OK !</h1>"
                + "<p>Retournez dans l'application BoostUp.</p>"
                + "<p class='hint'>Cette page va se fermer automatiquement...</p>"
                + "</div>"
                + "<script>setTimeout(function(){window.close();},3000);</script>"
                + "</body></html>";
    }

    private static String getErrorHtml(String error) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>BoostUp - Erreur</title>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family:'Segoe UI',system-ui,sans-serif; "
                + "background:linear-gradient(135deg,#1b2a4a,#2d1b4e); "
                + "min-height:100vh; display:flex; align-items:center; justify-content:center; }"
                + ".card { background:white; border-radius:24px; padding:60px 50px; "
                + "text-align:center; max-width:480px; width:90%; "
                + "box-shadow:0 25px 60px rgba(0,0,0,0.3); }"
                + ".icon { font-size:64px; margin-bottom:20px; }"
                + "h1 { color:#dc3545; font-size:26px; font-weight:800; margin-bottom:12px; }"
                + "p { color:#6c757d; font-size:15px; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='icon'>&#10060;</div>"
                + "<h1>Erreur GitHub</h1>"
                + "<p>" + error + "</p>"
                + "</div></body></html>";
    }

    // ------------------------------------------------------------------
    // Data holder
    // ------------------------------------------------------------------

    public static class GitHubUserInfo {
        public String id;
        public String login;
        public String name;
        public String email;
        public String avatarUrl;
        public String bio;

        @Override
        public String toString() {
            return "GitHubUserInfo{login='" + login + "', email='" + email + "', name='" + name + "'}";
        }
    }
}

