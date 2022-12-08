package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {
  public static File createOrClearFile(String path) throws IOException {
    File file = new File(path);
    if (file.exists()) {
      file.delete();
    }
    file.createNewFile();
    return file;
  }

  public static void appendTo(File file, String... contents) throws IOException {
    FileWriter writer = new FileWriter(file, true);
    BufferedWriter buff = new BufferedWriter(writer);
    for (String part : contents) {
      buff.write(part);
    }
    buff.close();
  }
}
