package me.valkeea.fishyaddons.util.text;

import java.util.stream.IntStream;

public class StringUtils {
    private StringUtils() {}

    /** 
     * Calculates the edit distance between two strings.
     * @return The minimum number of edits needed to match the strings.
     */
    public static int levenshteinDistance(String a, String b) {

        if (a == null || b == null) return Math.max(a == null ? 0 : a.length(), b == null ? 0 : b.length());

        a = a.toLowerCase();
        b = b.toLowerCase();

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        IntStream.rangeClosed(0, a.length()).forEach(i -> dp[i][0] = i);
        IntStream.rangeClosed(0, b.length()).forEach(j -> dp[0][j] = j);

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min( // Min of deletion, insertion, substitution
                    Math.min(dp[i - 1][j] + 1,
                             dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    /** 
     * Checks if two strings are close matches using Levenshtein distance.
     * @return True if edits dont exceed: 1 edit (≤4 char), 2 edits (≤8 char), else 25% of length.
     */
    public static boolean closeMatch(String desired, String input) {
        if (desired == null || input == null) return false;

        String[] desiredWords = desired.toLowerCase().split("[ _]");
        String[] inputWords = input.toLowerCase().split("[ _]");

        for (String dWord : desiredWords) {
            for (String iWord : inputWords) {
                int distance = levenshteinDistance(dWord, iWord);
                int maxLen = Math.max(dWord.length(), iWord.length());
                
                int threshold;
                if (maxLen <= 4) {
                    threshold = 1;
                } else if (maxLen <= 8) {
                    threshold = 2;
                } else {
                    threshold = maxLen / 4;
                }
                
                if (distance <= threshold) {
                    return true;
                }
            }
        }
        return false;
    }
}
