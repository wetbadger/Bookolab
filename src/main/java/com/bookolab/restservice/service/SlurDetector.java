package com.bookolab.restservice.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class SlurDetector {
    private static final Pattern DIACRITICS_AND_ACCENTS = Pattern.compile("\\p{M}");

    private String flattenText(String input) {
        if (input == null) {
            return null;
        }

        // 1. Decompose characters (e.g., "Ć" becomes "C" + "combining acute accent")
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // 2. Remove all the combining accents, leaving just the base English letters
        return DIACRITICS_AND_ACCENTS.matcher(normalized).replaceAll("");
    }

    public boolean isSlur(String rawInput) {
        String processed = flattenText(rawInput);              // Remove accents (Č -> C)
        processed = processed.toLowerCase();                    // Lowercase everything
        processed = processed.replace("1", "i")
                .replace("0", "o")
                .replace("3", "e")
                .replace("4", "a")
                .replace("2", "s")
                .replace("5", "s")
                .replace("6", "b")
                .replace("7", "t")
                .replace("8", "b")
                .replace("9", "g");
        processed = processed.replaceAll("[^a-z]", "");         // Remove spaces/punctuation

        // Now the regex list can just be a list of plain English words!
        return processed.matches(".*(chink|coon|faggot|gook|kike|nigger|spearchucker|tranny).*");
    }
}
