package utils;

public class StringUtil {
  public static String capitalize(String var) {
    if (var == null || var.length() == 0) return var;
    char first = var.charAt(0);
    if (first <= 'z' && first >= 'a') {
      first += 'A' - 'a';
    }
    return first + var.substring(1);
  }

  public static String quoteStr(String str) {
    return "\"" + str.replaceAll("\\\"", "\\\\\"") + "\"";
  }

  public static String escapedForRegex(String str) {
    return escapedForRegex(str, true);
  }
  public static String escapedForRegex(String str, Boolean includeStar) {
    if (includeStar) {
      return str.replaceAll("(?<toEscape>[$+.\\[\\]()?\\\\^{}|\\*])", "\\\\${toEscape}");
    }else {
      return str.replaceAll("(?<toEscape>[$+.\\[\\]()?\\\\^{}|])", "\\\\${toEscape}");
    }
  }
}
