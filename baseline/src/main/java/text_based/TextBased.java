package text_based;

import model.Language;
import model.SharedStatus;
import models.Baseline;
import models.CodeSource;
import utils.FileUtil;
import utils.MatcherUtils;

import java.util.*;
import java.util.stream.Collectors;

public class TextBased extends Baseline {
  public Map<String, Map<Language, List<CodeSource>>> tokenMap = new HashMap<>();
  public Map<String, Map<String, Integer>> wordsCnt = new HashMap<>();
  public Map<String, Integer> allTokenCnt = new HashMap<>();

  private Map<Language, Integer> fileCnt = new HashMap<>();
  private int allFileCnt = 0;

  public TextBased(String framework, String projectName, String projectDir) {
    super(framework, projectName, projectDir);
  }

  public void run(Map<String, List<String>> categorizedFiles) {
    super.run();
    for (String lang : categorizedFiles.keySet()) {
      fileCnt.put(Language.valueOfLabel(lang), categorizedFiles.get(lang).size());
      allFileCnt += categorizedFiles.get(lang).size();
      for (String file : categorizedFiles.get(lang)) {
        parseFile(Language.valueOfLabel(lang), file);
      }
    }

    Set<String> filteredTokens = filter();
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
              tokenMap.get(token).get(lang).add(new CodeSource(lang, null, token));

              if (!wordsCnt.containsKey(token)) {
                wordsCnt.put(token, new HashMap<>());
              }
              if (!wordsCnt.get(token).containsKey(filePath)) {
                wordsCnt.get(token).put(filePath, 0);
              }
              int curCnt = wordsCnt.get(token).get(filePath);
              wordsCnt.get(token).put(filePath, curCnt + 1);
            });
  }

  private Set<String> filter() {
    Set<String> tokenList = tokenMap.keySet();
    tokenList = tokenList.stream()
        .filter(this::appearInMultipleLanguages)
        .filter(this::tooFrequentInOneFile)
        .filter(this::tooManyFilesCovered)
        .collect(Collectors.toSet());
    return tokenList;
  }

  private boolean appearInMultipleLanguages(String token) {
    Map<Language, List<CodeSource>> modList = tokenMap.get(token);
    int cnt = 0;
    for (List<CodeSource> list : modList.values()) {
      if (list.size() != 0) cnt++;
    }
    return cnt > 0;
  }

  private double frequencyInOneFile = 0.01;

  private boolean tooFrequentInOneFile(String token) {
    Map<String, Integer> fileToCnt = wordsCnt.get(token);
    for (String fileName : fileToCnt.keySet()) {
      if ((double) fileToCnt.get(fileName) / allTokenCnt.get(fileName) < frequencyInOneFile) {
        return true;
      }
    }
    return false;
  }

  private double fileCoveredRate = 0.3;

  private boolean tooManyFilesCovered(String token) {
    int fileCnt = wordsCnt.get(token).size();
    return (double) fileCnt / allFileCnt < fileCoveredRate;
  }
}
