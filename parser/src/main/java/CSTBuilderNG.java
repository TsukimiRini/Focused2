import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tongfei.progressbar.ProgressBar;
import model.CSTTree;
import model.Language;
import model.SharedStatus;
import org.treesitter.*;
import org.treesitter.TreeSitterRust;
import utils.FileUtil;

public class CSTBuilderNG {
  public static TSParser parser = new TSParser();
  public static TSLanguage tsLanguage = null;

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
        tsLanguage = new TreeSitterJava();
        break;
      case XML:
        tsLanguage = new TreeSitterXml();
        break;
      case HTML:
        tsLanguage = new TreeSitterHtml();
        break;
      case CSS:
        tsLanguage = new TreeSitterCss();
        break;
      case CLike:
      case C:
      case CC:
      case CPP:
      case HPP:
      case H:
        tsLanguage = new TreeSitterCpp();
        break;
      case Python:
        tsLanguage = new TreeSitterPython();
        break;
      case Rust:
        tsLanguage = new TreeSitterRust();
        break;
    }
    parser.setLanguage(tsLanguage);
    Map<String, CSTTree> cstTrees = new HashMap<>();
    for (String file : ProgressBar.wrap(files, "Building " + tsLanguage.toString() + " CST")) {
      String source = FileUtil.readFileToString(file);

      String[] splitted = source.split("\n", -1);
      int[] lineLenSum = new int[splitted.length];
      for (int i = 0; i < splitted.length; i++) {
        lineLenSum[i] = (splitted[i]+"\n").length();
        if (i > 0) {
          lineLenSum[i] += lineLenSum[i - 1];
        }
      }

      CSTTree tree = new CSTTree(file, source, splitted, lineLenSum, parser.parseString(null, source), tsLanguage);
      cstTrees.put(file, tree);
    }
    return cstTrees;
  }
}
