import model.*;
import model.config.ConfigLinkBlock;
import model.souffle.ADT;
import model.souffle.PredicateDecl;
import model.souffle.Record;
import org.apache.commons.lang3.tuple.Pair;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Focused2Souffle {
  private static String framework = "android";
  public static final String config_path =
      System.getProperty("user.dir")
          + "/translator/src/main/resources/"
          + framework
          + "_config.fcs";
  public static final String souffle_output =
      System.getProperty("user.dir") + "/translator/src/main/resources/" + framework + "_config.dl";

  public static void main(String[] args) throws IOException {
    Pair<List<URIPattern>, List<ConfigLinkBlock>> pair = ConfigLoader.load(config_path);
    List<URIPattern> patterns = pair.getLeft();
    ADT capType = generateCaptureType(patterns);
    ADT brType = generateBranchType(patterns);
    Record URIType = generateURIType();

    String[] types = {capType + "\n", brType + "\n", URIType + "\n"};
    String[] predicates =
        generateElePredicates(patterns).stream()
            .map(p -> p.toString() + "\n")
            .collect(Collectors.toList())
            .toArray(new String[patterns.size()]);

    File opt = FileUtil.createOrClearFile(souffle_output);
    FileUtil.appendTo(opt, types);
    FileUtil.appendTo(opt, predicates);
  }

  public static Record generateURIType() {
    Record decl = new Record("URI");
    decl.parseFields(
        "lang: symbol, file: symbol, element: symbol, branches: Branch, caps: Capture");
    return decl;
  }

  public static ADT generateCaptureType(List<URIPattern> patterns) {
    ADT adt = new ADT("Capture", "ZeroCap");
    for (URIPattern pattern : patterns) {
      if (!pattern.captures.isEmpty()) {
        List<Pair<String, String>> captures = new ArrayList<>();
        for (String cap : pattern.captures) {
          captures.add(Pair.of(cap, "symbol"));
        }
        adt.addBranch(pattern.label + "Cap", captures);
      }
    }
    return adt;
  }

  public static ADT generateBranchType(List<URIPattern> patterns) {
    ADT adt = new ADT("Branch", "ZeroBranch");
    for (URIPattern pattern : patterns) {
      if (!pattern.branches.isEmpty()) {
        List<Pair<String, String>> branches = new ArrayList<>();
        for (int i = 0; i < pattern.branches.size(); i++) {
          SegmentPattern br = pattern.branches.get(i);
          branches.add(Pair.of("_" + i, "symbol"));
        }
        adt.addBranch(pattern.label + "Br", branches);
      }
    }
    return adt;
  }

  public static List<PredicateDecl> generateElePredicates(List<URIPattern> patterns) {
    List<PredicateDecl> decls = new ArrayList<>();
    for (URIPattern pattern : patterns) {
      decls.add(new PredicateDecl(pattern.label, "uri", "URI"));
    }
    return decls;
  }
}
