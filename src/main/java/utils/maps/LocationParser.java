package utils.maps;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocationParser {

    private LocationParser() {}

    public enum Kind { URL, LAT_LON, ADDRESS, EMPTY, INVALID }

    public static final class ParsedLocation {
        public final Kind kind;
        public final String raw;
        public final String url;
        public final String address;
        public final Double lat;
        public final Double lon;

        private ParsedLocation(Kind kind, String raw, String url, String address, Double lat, Double lon) {
            this.kind = kind;
            this.raw = raw;
            this.url = url;
            this.address = address;
            this.lat = lat;
            this.lon = lon;
        }

        public static ParsedLocation empty(String raw) {
            return new ParsedLocation(Kind.EMPTY, raw, null, null, null, null);
        }

        public static ParsedLocation invalid(String raw) {
            return new ParsedLocation(Kind.INVALID, raw, null, null, null, null);
        }

        public static ParsedLocation url(String raw, String url) {
            return new ParsedLocation(Kind.URL, raw, url, null, null, null);
        }

        public static ParsedLocation latLon(String raw, double lat, double lon) {
            return new ParsedLocation(Kind.LAT_LON, raw, null, null, lat, lon);
        }

        public static ParsedLocation address(String raw, String address) {
            return new ParsedLocation(Kind.ADDRESS, raw, null, address, null, null);
        }
    }

    private static final Pattern COORDS = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");

    public static ParsedLocation parse(String lieu) {
        String raw = lieu == null ? "" : lieu;
        String s = raw.trim();
        if (s.isEmpty()) return ParsedLocation.empty(raw);

        // URL
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return ParsedLocation.url(raw, s);
        }

        // lat,lon
        Matcher m = COORDS.matcher(s);
        if (m.matches()) {
            try {
                double lat = Double.parseDouble(m.group(1));
                double lon = Double.parseDouble(m.group(2));
                if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                    return ParsedLocation.invalid(raw);
                }
                return ParsedLocation.latLon(raw, lat, lon);
            } catch (NumberFormatException ex) {
                return ParsedLocation.invalid(raw);
            }
        }

        // Adresse (fallback)
        if (s.length() < 3) {
            return ParsedLocation.invalid(raw);
        }
        return ParsedLocation.address(raw, s);
    }

    public static Optional<double[]> tryParseLatLon(String lieu) {
        ParsedLocation p = parse(lieu);
        if (p.kind != Kind.LAT_LON) return Optional.empty();
        return Optional.of(new double[]{p.lat, p.lon});
    }
}

