package jake2.qcommon.util;

import java.util.*;

public class EntityParser {
    /**
     * Parse the entities string.
     * Very similar to the json format, but without a colon between key/value pairs
     * and without commas between objects, like
     * <br>
     *    {
     *     "key" "value"
     *    }
     *    {
     *     "foo" "bar"
     *    }
     * <br>
     * TODO: add some validation and error messages, because right now parser just silently skips anything it doesn't like
     */
    public static List<Map<String, String>> parseEntities(String src) {
        if (src == null || src.isBlank())
            return Collections.emptyList();

        Map<String, String> currentEntity = new HashMap<>();
        List<Map<String, String>> result = new ArrayList<>();
        StringBuilder currentString = null; // string between the double quotes "

        String key = null;
        String value;

        boolean startComment = false; // first slash
        boolean inComment = false;

        for (char c : src.toCharArray()) {
            if (inComment) {
                if (c == '\n')
                    inComment = false;
                // else -> skip
            } else if (c == '/' && currentString == null) {
                if (startComment) {
                    inComment = true;
                    startComment = false;
                } else
                    startComment = true;
            } else if (c == '"') {
                if (currentString != null) {
                    // either key or value is completed
                    if (key == null) {
                        key = currentString.toString();
                    } else {
                        value = currentString.toString();
                        currentEntity.put(key, value);
                        // cleanup
                        key = null;
                    }
                    currentString = null;
                } else {
                    currentString = new StringBuilder();
                }
            } else if (c == '{' && currentString == null) {
                currentEntity = new HashMap<>();
            } else if (c == '}' && currentString == null) {
                result.add(currentEntity);
            } else if (currentString != null) {
                currentString.append(c);
            }
            // else -> skip
        }
        return result;
    }
}
