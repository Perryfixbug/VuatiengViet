package vuatiengvietpj.protocol;

import java.util.regex.Pattern;

// quy ước 
public class MessageFormatter {
    public static String formatRequest;
    public static final String DELIM = "|";
    private static final Pattern SPLIT_PATTERN = Pattern.compile("(?<!\\\\)\\|");

    // public static String formatError(String message) {
    // return formatResponse("ERROR", message);
    // }

    // public static String formatSuccess(String message) {
    // return formatResponse("SUCCESS", message);
    // }

}