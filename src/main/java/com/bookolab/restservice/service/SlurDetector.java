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
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return DIACRITICS_AND_ACCENTS.matcher(normalized).replaceAll("");
    }

    public boolean isSlur(String rawInput) {
        String processed = flattenText(rawInput);
        processed = processed.toLowerCase();
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
        processed = processed.replaceAll("[^a-z]", "");
        return processed.matches(".*(chink|coon|faggot|gook|kike|nigger|spearchucker|tranny).*");
    }
}
