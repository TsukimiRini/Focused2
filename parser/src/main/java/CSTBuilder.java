import ai.serenade.treesitter.Languages;
import ai.serenade.treesitter.Parser;
import model.CSTTree;
import model.Language;
import model.SharedStatus;
import utils.FileUtil;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSTBuilder {
  static {
    // System.load(
    //     System.getProperty("user.home")
    //         + "/home/code/projects/tree-sitter-java/tree-sitter-java.dylib");
    System.load(
        System.getProperty("user.home")
            + "/home/code/projects/tree-sitter-rust/tree-sitter-rust.dylib");
  }

  public static Parser parser = new Parser();

  public static Map<Language, Map<String, CSTTree>> buildCST() throws UnsupportedEncodingException {
    Map<String, List<String>> categorizedFiles =
        FileUtil.listFilePathsInLanguages(
            SharedStatus.projectInfo.projectDir, SharedStatus.projectInfo.languages);
    Map<Language, Map<String, CSTTree>> cstTrees = new HashMap<>();
    for (String lang : categorizedFiles.keySet()) {
      Language curLang = Language.valueOfLabel(lang);
      if(!cstTrees.containsKey(Language.valueOfLabel(lang))){
        cstTrees.put(curLang, new HashMap<>());
      }
      cstTrees.get(curLang).putAll(buildCST(Language.valueOfLabel(lang), categorizedFiles.get(lang)));
    }
    return cstTrees;
  }

  public static Map<String, CSTTree> buildCST(Language language, List<String> files)
      throws UnsupportedEncodingException {
    switch (language) {
      case JAVA:
        parser.setLanguage(Languages.java());
        break;
      case XML:
        parser.setLanguage(Languages.xml());
        break;
      case HTML:
        parser.setLanguage(Languages.html());
        break;
      case CSS:
        parser.setLanguage(Languages.css());
        break;
      case Rust:
        parser.setLanguage(Languages.rust());
        break;
      default:
        throw new UnsupportedEncodingException("Unsupported language: " + language);
    }
    Map<String, CSTTree> cstTrees = new HashMap<>();
    for (String file : files) {
      String source = FileUtil.readFileToString(file);
      CSTTree tree = new CSTTree(file, source, parser.parseString(source));
      cstTrees.put(file, tree);
    }
    return cstTrees;
  }
}
