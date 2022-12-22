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
}
