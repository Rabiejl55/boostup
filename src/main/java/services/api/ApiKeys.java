package services.api;

public class ApiKeys {
    public static String openRouter() { return System.getenv("OPENROUTER_API_KEY"); }
    public static String news() { return System.getenv("RSS_API_KEY"); }
    public static String exchange() { return System.getenv("EXCHANGE_API_KEY"); }
}