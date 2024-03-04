package utils;

import model.Language;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
  protected static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

  static {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public static File createOrClearFile(String path) {
    File file = new File(path);
    if (file.exists()) {
      file.delete();
    }
    try{
      if (!file.getParentFile().exists()){
        file.getParentFile().mkdirs();
      }
      file.createNewFile();
    }catch (Exception e){
      e.printStackTrace();
      logger.error("file not created: {}", path);
    }
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

  private static String rootPath;

  public static void setRootPath(String rootPath) {
    FileUtil.rootPath = rootPath;
  }

  public static String getRootPath() {
    return rootPath;
  }

  /**
   * Get path relative to the root path
   *
   * @param absolutePath
   * @return
   */
  public static String getRelativePath(String absolutePath) {
    if (absolutePath.startsWith(rootPath))
      return FilenameUtils.separatorsToUnix(FileUtil.getRelativePath(rootPath, absolutePath));
    return absolutePath;
  }

  /**
   * Get path relative to the root path
   *
   * @param absolutePath
   * @param rootPath
   * @return
   */
  public static String getRelativePath(String rootPath, String absolutePath) {
    return Paths.get(rootPath).relativize(Paths.get(absolutePath)).toString();
  }

  /**
   * Write the given content in the file of the given file path.
   *
   * @param content
   * @param filePath
   * @return boolean indicating the success of the write operation.
   */
  public static boolean writeStringToFile(String content, String filePath) {
    try {
      FileUtils.writeStringToFile(new File(filePath), content, "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  /**
   * Read the content of a file into string Actually wraps commons io API
   *
   * @return
   */
  public static String readFileToString(String filePath) {
    String content = "";
    try {
      content = FileUtils.readFileToString(new File(filePath), "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }

  /**
   * Read the content of a given file.
   *
   * @param path to be read
   * @return string content of the file, or null in case of errors.
   */
  public static List<String> readFileToLines(String path) {
    List<String> lines = new ArrayList<>();
    File file = new File(path);
    if (file.exists()) {
      try (BufferedReader reader =
          Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
        lines = reader.lines().collect(Collectors.toList());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      return lines;
    }
    return lines;
  }

  public static Map<String, List<String>> categorizeFilesByExtension(List<String> filePaths) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (String path : filePaths) {
      String extension = FilenameUtils.getExtension(path);
      if (!result.containsKey(extension)) {
        result.put(extension, new ArrayList<>());
      }
      result.get(extension).add(path);
    }
    return result;
  }

  public static Map<String, List<String>> categorizeFilesByExtensionInLanguages(
      List<String> filePaths, Set<Language> languages) {
    Set<String> extensions =
        languages.stream()
            .map(language -> language.extension.replace(".", ""))
            .collect(Collectors.toSet());

    Map<String, List<String>> result = new LinkedHashMap<>();
    for (String path : filePaths) {
      String extension = FilenameUtils.getExtension(path);
      if (!extensions.contains((extension))) continue;
      if (!result.containsKey(extension)) {
        result.put(extension, new ArrayList<>());
      }
      result.get(extension).add(path);
    }
    return result;
  }

  /**
   * List all files with specific extension under a folder/directory
   *
   * @param dir
   * @return absolute paths
   */
  public static List<String> listFilePaths(String dir, String extension) {
    List<String> result = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(Paths.get(dir))) {
      if (extension.isEmpty() || extension.isBlank()) {
        result = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
      } else {
        result =
            walk.filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(path -> path.endsWith(extension))
                //              .map(s -> s.substring(dir.length()))
                .collect(Collectors.toList());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * List { extension : [filePath] } under a folder/directory
   *
   * @param dir
   * @param languages
   * @return
   */
  public static Map<String, List<String>> listFilePathsInLanguages(
      String dir, Set<Language> languages) {
    if (languages.isEmpty()) {
      return new HashMap<>();
    }
    Map<String, List<String>> result = new LinkedHashMap<>();

    Set<String> extensions =
        languages.stream()
            .map(language -> language.extension.replace(".", ""))
            .collect(Collectors.toSet());
    try (Stream<Path> walk = Files.walk(Paths.get(dir))) {
      walk.filter(Files::isRegularFile)
          .map(Path::toString)
          .forEach(
              path -> {
                String ext = FilenameUtils.getExtension(path);
                if (extensions.contains(ext)) {
                  if (!result.containsKey(ext)) {
                    result.put(ext, new ArrayList<>());
                  }
                  result.get(ext).add(path);
                }
              });
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  public static boolean deleteFile(String path) {
    File f = new File(path);
    if (f.exists()) return f.delete();
    return true;
  }
}
