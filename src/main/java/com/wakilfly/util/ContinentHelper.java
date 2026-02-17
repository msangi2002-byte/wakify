package com.wakilfly.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps country names to continent names.
 * Used for admin map stats (continents, countries).
 */
public final class ContinentHelper {

    private static final Map<String, String> COUNTRY_TO_CONTINENT = new ConcurrentHashMap<>();

    static {
        // Africa
        String[] africa = {"Tanzania", "Kenya", "Uganda", "Rwanda", "Burundi", "Ethiopia", "Somalia", "Djibouti",
                "Eritrea", "South Sudan", "Sudan", "Egypt", "Libya", "Tunisia", "Algeria", "Morocco", "Mauritania",
                "Mali", "Niger", "Chad", "Nigeria", "Cameroon", "Central African Republic", "Gabon", "Congo",
                "DRC", "Democratic Republic of Congo", "Angola", "Zambia", "Zimbabwe", "Malawi", "Mozambique",
                "Madagascar", "Botswana", "Namibia", "South Africa", "Lesotho", "Swaziland", "Eswatini",
                "Senegal", "Gambia", "Guinea", "Guinea-Bissau", "Sierra Leone", "Liberia", "Ivory Coast", "Côte d'Ivoire",
                "Ghana", "Togo", "Benin", "Burkina Faso", "Mauritius", "Seychelles", "Comoros", "Réunion", "Reunion"};
        for (String c : africa) {
            COUNTRY_TO_CONTINENT.put(normalize(c), "Africa");
        }
        // Asia
        String[] asia = {"India", "China", "Japan", "South Korea", "North Korea", "Indonesia", "Malaysia", "Singapore",
                "Thailand", "Vietnam", "Philippines", "Pakistan", "Bangladesh", "Sri Lanka", "Nepal", "Bhutan",
                "Myanmar", "Cambodia", "Laos", "Mongolia", "Kazakhstan", "Uzbekistan", "Turkmenistan", "Tajikistan",
                "Kyrgyzstan", "Afghanistan", "Iran", "Iraq", "Syria", "Lebanon", "Jordan", "Israel", "Palestine",
                "Saudi Arabia", "Yemen", "Oman", "UAE", "Qatar", "Bahrain", "Kuwait", "Turkey", "Taiwan",
                "Hong Kong", "Macau", "Brunei", "Timor-Leste", "Maldives"};
        for (String c : asia) {
            COUNTRY_TO_CONTINENT.put(normalize(c), "Asia");
        }
        // Europe
        String[] europe = {"United Kingdom", "UK", "Germany", "France", "Italy", "Spain", "Portugal", "Netherlands",
                "Belgium", "Switzerland", "Austria", "Poland", "Sweden", "Norway", "Denmark", "Finland",
                "Ireland", "Greece", "Romania", "Hungary", "Czech Republic", "Czechia", "Russia", "Ukraine",
                "Belarus", "Croatia", "Serbia", "Bosnia", "Slovenia", "Slovakia", "Bulgaria", "Lithuania",
                "Latvia", "Estonia", "Iceland", "Luxembourg", "Malta", "Cyprus", "Albania", "North Macedonia",
                "Moldova", "Georgia", "Armenia", "Azerbaijan"};
        for (String c : europe) {
            COUNTRY_TO_CONTINENT.put(normalize(c), "Europe");
        }
        // North America
        String[] na = {"United States", "USA", "US", "Canada", "Mexico", "Cuba", "Jamaica", "Haiti", "Dominican Republic",
                "Puerto Rico", "Guatemala", "Honduras", "El Salvador", "Nicaragua", "Costa Rica", "Panama",
                "Belize", "Trinidad and Tobago", "Barbados", "Bahamas"};
        for (String c : na) {
            COUNTRY_TO_CONTINENT.put(normalize(c), "North America");
        }
        // South America
        String[] sa = {"Brazil", "Argentina", "Chile", "Colombia", "Peru", "Venezuela", "Ecuador", "Bolivia",
                "Paraguay", "Uruguay", "Suriname", "Guyana", "French Guiana"};
        for (String c : sa) {
            COUNTRY_TO_CONTINENT.put(normalize(c), "South America");
        }
        // Oceania
        String[] oceania = {"Australia", "New Zealand", "Papua New Guinea", "Fiji", "Samoa", "Tonga", "Vanuatu", "Solomon Islands"};
        for (String c : oceania) {
            COUNTRY_TO_CONTINENT.put(normalize(c), "Oceania");
        }
    }

    private static String normalize(String s) {
        if (s == null || s.isBlank()) return "";
        return s.trim().toLowerCase();
    }

    /**
     * Get continent name for a country.
     * @param country Country name (e.g. "Tanzania", "Kenya")
     * @return Continent name (e.g. "Africa") or "Unknown" if not found
     */
    public static String getContinent(String country) {
        if (country == null || country.isBlank()) return "Unknown";
        String c = normalize(country);
        return COUNTRY_TO_CONTINENT.getOrDefault(c, "Unknown");
    }

    private ContinentHelper() {}
}
