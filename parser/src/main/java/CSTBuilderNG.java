import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.CSTTree;
import model.Language;
import model.SharedStatus;
import org.treesitter.*;
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
    }
    parser.setLanguage(tsLanguage);
    Map<String, CSTTree> cstTrees = new HashMap<>();
    for (String file : files) {
      String source = FileUtil.readFileToString(file);
      CSTTree tree = new CSTTree(file, source, parser.parseString(null, source), tsLanguage);
      cstTrees.put(file, tree);
    }
    return cstTrees;
  }
}
