package text_based;

import com.csvreader.CsvReader;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import model.Language;
import model.Range;
import model.SharedStatus;
import models.Baseline;
import models.CodeSource;
import org.antlr.v4.runtime.misc.Pair;
import org.apache.commons.io.FilenameUtils;
import utils.FileUtil;
import utils.MatcherUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TextBased extends Baseline {
  public Map<String, Map<Language, List<CodeSource>>> tokenMap = new HashMap<>();
  public Map<String, Map<String, Integer>> wordsCnt = new HashMap<>();
  public Map<String, Map<Language, Integer>> moduleCnt = new HashMap<>();
  public Map<String, Integer> allTokenCnt = new HashMap<>();

  private Map<Language, Integer> fileCnt = new HashMap<>();
  private int allFileCnt = 0;

  public Map<Pair<String, String>, Set<String>> filePairToDeps = new HashMap<>();

  public static class FocusedRecord {
    public String defLang, defFile, defIdentifier;
    public String refLang, refFile, refIdentifier;

    public FocusedRecord(List<String> values) {
      this.defLang = values.get(0);
      this.defFile = FileUtil.getRelativePath(values.get(1).replaceAll("//", "/"));
      if (!defFile.startsWith("/")) defFile = "/" + defFile;
      this.refLang = values.get(3);
      this.refFile = FileUtil.getRelativePath(values.get(4).replaceAll("//", "/"));
      if (!refFile.startsWith("/")) refFile = "/" + refFile;

      this.defIdentifier = values.get(2);
      this.refIdentifier = values.get(5);
    }

    public String toString() {
      return String.format("%s,%s,%s,%s", defIdentifier, defFile, refIdentifier, refFile);
    }

    public boolean equals(Object o) {
      if (o instanceof FocusedRecord) {
        FocusedRecord record = (FocusedRecord) o;
        String objectDefIdentifier, objectRefIdentifier;
        String objectDefFile, objectRefFile;
        if (record.defLang.equals(refLang) && record.refLang.equals(defLang)) {
          objectDefIdentifier = record.refIdentifier;
          objectRefIdentifier = record.defIdentifier;
          objectDefFile = record.refFile;
          objectRefFile = record.defFile;
        } else if (record.defLang.equals(defLang) && record.refLang.equals(refLang)) {
          objectDefIdentifier = record.defIdentifier;
          objectRefIdentifier = record.refIdentifier;
          objectDefFile = record.defFile;
          objectRefFile = record.refFile;
        } else return false;
        return (objectDefIdentifier.equals(defIdentifier)
                || objectRefIdentifier.equals(refIdentifier))
            && objectDefFile.equals(defFile)
            && objectRefFile.equals(refFile);
      }
      return false;
    }

    public int hashCode() {
      return defFile.hashCode()
          + defIdentifier.hashCode()
          + refFile.hashCode()
          + refIdentifier.hashCode();
    }

    private String getIdentifierFromURI(String uri) {
      if (uri.length() == 0) return uri;
      List<String> segs = MatcherUtils.matchSegments(uri);
      String lastSeg = segs.get(segs.size() - 1);
      Map<String, String> parts = MatcherUtils.splitSegment(lastSeg);
      return parts.get("content");
    }
  }

  public TextBased(String framework, String projectName, String projectDir, String outputDir) {
    super(framework, projectName, projectDir, outputDir);
    outputPath = outputDir + "/" + framework + "/" + projectName;
  }

  public Multiset<FocusedRecord> readInFocusedOutput(String fileName) {
    Multiset<FocusedRecord> focusedRecords = HashMultiset.create();
    try {
      CsvReader reader = new CsvReader(fileName, ',', StandardCharsets.UTF_8);
      while (reader.readRecord()) {
        FocusedRecord newRecord = new FocusedRecord(List.of(reader.getValues()));
        if (newRecord.defIdentifier.length() == 0 || newRecord.refIdentifier.length() == 0)
          continue;
        focusedRecords.add(newRecord);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return focusedRecords;
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
      outputDeps(filePairToDeps);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Multiset<FocusedRecord> transformBaselineRes() {
    Multiset<FocusedRecord> res = HashMultiset.create();

    for (Pair<String, String> filePair : filePairToDeps.keySet()) {
      String file1 = filePair.a, file2 = filePair.b;
      String lang1 = Language.valueOfLabel("." + FilenameUtils.getExtension(file1)).getIdentifier(),
          lang2 = Language.valueOfLabel("." + FilenameUtils.getExtension(file2)).getIdentifier();
      for (String identifier : filePairToDeps.get(filePair))
        res.add(new FocusedRecord(List.of(lang1, file1, identifier, lang2, file2, identifier)));
    }
    return res;
  }

  public void validate(String focusedResPath) {
    Multiset<FocusedRecord> ourRes = readInFocusedOutput(focusedResPath);
    Multiset<FocusedRecord> baselineRes = transformBaselineRes();
    Multiset<FocusedRecord> inter = Multisets.intersection(ourRes, baselineRes);
    ourRes = Multisets.filter(ourRes, Predicates.not(inter::contains));
    baselineRes = Multisets.filter(baselineRes, Predicates.not(inter::contains));

    logger.info(
        "our results not found in theirs: {}\ntheir results not found in ours: {}\nintersection count: {}\n",
        ourRes.size(),
        baselineRes.size(),
        inter.size());
    try {
      outputValidation(ourRes, baselineRes, inter);
    } catch (Exception e) {
      logger.error("can't read or write file");
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
              moduleCnt.get(token).put(lang, curModCnt + 1);
            });
  }

  private Set<String> filter() {
    Set<String> tokenList = tokenMap.keySet();
    tokenList =
        tokenList.stream()
            .filter(this::identifierFormat)
            .filter(this::appearInMultipleLanguages)
            //            .filter(this::tooFrequentInOneFile)
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

  private final double frequencyInOneFile = 0.01;
  private final int appearanceInOneFile = 15;

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

  private final double perModuleThreshold = 0.35;

  private boolean tooManyFilesCovered(String token) {
    for (Language lang : SharedStatus.projectInfo.languages) {
      int modCntForToken = moduleCnt.get(token).get(lang);
      if ((double) modCntForToken / fileCnt.get(lang) > perModuleThreshold) {
        return false;
      }
    }
    return true;
  }

  private final int lowWeightThreshold = 4;

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

  private void outputDeps(Map<Pair<String, String>, Set<String>> dependencies) throws IOException {
    File f = FileUtil.createOrClearFile(outputPath);
    for (Pair<String, String> key : dependencies.keySet()) {
      Set<String> tokens = dependencies.get(key);
      for (String token : tokens) {
        FileUtil.appendTo(f, String.format("%s <%s, %s>\n", token, key.a, key.b));
      }
    }
  }

  private void outputValidation(
      Multiset<FocusedRecord> ours,
      Multiset<FocusedRecord> baselines,
      Multiset<FocusedRecord> inter)
      throws IOException {
    if (ours.size() > 0) {
      File fours = FileUtil.createOrClearFile(outputPath + "_ours");
      outputFocusedRecord(fours, ours);
    } else FileUtil.deleteFile(outputPath + "_ours");
    if (baselines.size() > 0) {
      File ftext = FileUtil.createOrClearFile(outputPath + "_textBased");
      outputFocusedRecord(ftext, baselines);
    } else FileUtil.deleteFile(outputPath + "_textBased");
    if (inter.size() > 0) {
      File finter = FileUtil.createOrClearFile(outputPath + "_inter");
      outputFocusedRecord(finter, inter);
    } else FileUtil.deleteFile(outputPath + "_inter");
  }

  private void outputFocusedRecord(File f, Multiset<FocusedRecord> records) throws IOException {
    for (FocusedRecord record : records) {
      FileUtil.appendTo(f, record.toString() + "\n");
    }
  }
}
