package org.embeddedt.embeddium.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class StringUtils {
    // Levenshtein distance: Calculates the number of edits (insertion, deletion, substitution) needed to transform one string into another.
    public static int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1, Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
            }
        }

        return dp[m][n];
    }

    public static <T> List<T> fuzzySearch(Iterable<T> options, String userInput, int maxDistance, Function<T, String> toStringFn) {
        List<T> result = new ArrayList<>();
        String[] targetWords = userInput.toLowerCase().split("\\s+");

        for (T option : options) {
            String sentence = toStringFn.apply(option).toLowerCase();

            boolean containsAllWords = true;
            for (String word : targetWords) {
                boolean containsWord = false;
                for (String sentenceWord : sentence.toLowerCase().split("\\s+")) {
                    int distance = levenshteinDistance(word, sentenceWord);
                    if (distance <= maxDistance) {
                        containsWord = true;
                        break;
                    }
                    // Starts with match
                    if (sentenceWord.startsWith(word)) {
                        containsWord = true;
                        break;
                    }
                }
                if (!containsWord) {
                    containsAllWords = false;
                    break;
                }
            }
            if (containsAllWords) {
                result.add(option);
            }
        }

        return result;
    }
}
