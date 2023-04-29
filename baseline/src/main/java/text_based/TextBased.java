package text_based;

import model.Language;
import model.Range;
import model.SharedStatus;
import models.Baseline;
import models.CodeSource;
import org.antlr.v4.runtime.misc.Pair;
import utils.FileUtil;
import utils.MatcherUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TextBased extends Baseline {
  public Map<String, Map<Language, List<CodeSource>>> tokenMap = new HashMap<>();
  public Map<String, Map<String, Integer>> wordsCnt = new HashMap<>();
  public Map<String, Map<Language, Integer>> moduleCnt = new HashMap<>();
  public Map<String, Integer> allTokenCnt = new HashMap<>();

  private Map<Language, Integer> fileCnt = new HashMap<>();
  private int allFileCnt = 0;

  public TextBased(String framework, String projectName, String projectDir, String outputDir) {
    super(framework, projectName, projectDir, outputDir);
    outputPath = outputDir + "\\" + framework + "\\" + projectName;
  }

  public void run(Map<String, List<String>> categorizedFiles) {
    super.run(categorizedFiles);
    for (String lang : categorizedFiles.keySet()) {
      fileCnt.put(Language.valueOfLabel(lang), categorizedFiles.get(lang).size());
      allFileCnt += categorizedFiles.get(lang).size();
      for (String file : categorizedFiles.get(lang)) {
        parseFile(Language.valueOfLabel(lang), file);
      }
    }

    Set<String> filteredTokens = filter();

    Map<Pair<String, String>, Set<String>> filePairToDeps = new HashMap<>();
    List<Language> languages = new ArrayList<>(SharedStatus.projectInfo.languages);
    for (String token : filteredTokens) {
      for (int i = 0; i < languages.size(); i++) {
        Language lang1 = languages.get(i);
        for (int j = i + 1; j < languages.size(); j++) {
          Language lang2 = languages.get(j);
          List<CodeSource> identifierList1 = tokenMap.get(token).get(lang1);
          List<CodeSource> identifierList2 = tokenMap.get(token).get(lang2);
          for (CodeSource identifier1 : identifierList1) {
            for (CodeSource identifier2 : identifierList2) {
              Pair<String, String> key =
                  new Pair<>(identifier1.range.getFileName(), identifier2.range.getFileName());
              if (!filePairToDeps.containsKey(key))
                filePairToDeps.put(
                    new Pair<>(identifier1.range.getFileName(), identifier2.range.getFileName()),
                    new HashSet<>());
              filePairToDeps.get(key).add(token);
            }
          }
        }
      }
    }
    filePairToDeps = filterOutLowWeight(filePairToDeps);
    try {
      output(filePairToDeps);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void parseFile(Language lang, String filePath) {
    String content = FileUtil.readFileToString(filePath);
    String cleanedContent = MatcherUtils.cleanCommentsAndSpecSym(lang, content);
    String[] splitTokens = cleanedContent.split(" ");
    allTokenCnt.put(filePath, splitTokens.length);
    Arrays.stream(cleanedContent.split(" "))
        .forEach(
            token -> {
              if (!tokenMap.containsKey(token)) {
                tokenMap.put(token, new HashMap<>());
                SharedStatus.projectInfo.languages.forEach(
                    language -> tokenMap.get(token).put(language, new ArrayList<>()));
              }
              tokenMap
                  .get(token)
                  .get(lang)
                  .add(new CodeSource(lang, new Range("", filePath), token));

              if (!wordsCnt.containsKey(token)) {
                wordsCnt.put(token, new HashMap<>());
              }
              if (!wordsCnt.get(token).containsKey(filePath)) {
                wordsCnt.get(token).put(filePath, 0);
              }
              int curCnt = wordsCnt.get(token).get(filePath);
              wordsCnt.get(token).put(filePath, curCnt + 1);

              if (!moduleCnt.containsKey(token)) {
                moduleCnt.put(token, new HashMap<>());
              }
              if (!moduleCnt.get(token).containsKey(lang)) {
                moduleCnt.get(token).put(lang, 0);
              }
              int curModCnt = moduleCnt.get(token).get(lang);
              moduleCnt.get(token).put(lang, curModCnt);
            });
  }

  private Set<String> filter() {
    Set<String> tokenList = tokenMap.keySet();
    tokenList =
        tokenList.stream()
            .filter(this::identifierFormat)
            .filter(this::appearInMultipleLanguages)
            .filter(this::tooFrequentInOneFile)
            .filter(this::tooManyFilesCovered)
            .collect(Collectors.toSet());
    return tokenList;
  }

  private boolean identifierFormat(String token) {
    return token.matches("^[a-zA-Z]\\w*|_\\w+$");
  }

  private boolean appearInMultipleLanguages(String token) {
    Map<Language, List<CodeSource>> modList = tokenMap.get(token);
    int cnt = 0;
    for (List<CodeSource> list : modList.values()) {
      if (list.size() != 0) cnt++;
    }
    return cnt > 1;
  }

  private double frequencyInOneFile = 0.01;
  private int appearanceInOneFile = 15;

  private boolean tooFrequentInOneFile(String token) {
    Map<String, Integer> fileToCnt = wordsCnt.get(token);
    for (String fileName : fileToCnt.keySet()) {
      if ((double) fileToCnt.get(fileName) / allTokenCnt.get(fileName) > frequencyInOneFile
          || fileToCnt.get(fileName) >= appearanceInOneFile) {
        return false;
      }
    }
    return true;
  }

  private double perModuleThreshold = 0.35;

  private boolean tooManyFilesCovered(String token) {
    for (Language lang : SharedStatus.projectInfo.languages) {
      int fileCnt = moduleCnt.get(token).get(lang);
      if ((double) fileCnt / allFileCnt > perModuleThreshold) return false;
    }
    return true;
  }

  private int lowWeightThreshold = 4;

  private Map<Pair<String, String>, Set<String>> filterOutLowWeight(
      Map<Pair<String, String>, Set<String>> dependencies) {
    Map<Pair<String, String>, Set<String>> filtered = new HashMap<>();
    dependencies.forEach(
        (key, val) -> {
          if (val.size() > lowWeightThreshold) {
            filtered.put(key, val);
          }
        });
    return filtered;
  }

  private void output(Map<Pair<String, String>, Set<String>> dependencies) throws IOException {
    File f = FileUtil.createOrClearFile(outputPath);
    for (Pair<String, String> key : dependencies.keySet()) {
      Set<String> tokens = dependencies.get(key);
      for (String token : tokens) {
        FileUtil.appendTo(f, String.format("%s <%s, %s>\n", token, key.a, key.b));
      }
    }
  }
}
