import model.*;
import model.config.ConfigLink;
import model.config.ConfigLinkBlock;
import model.enums.PredicateType;
import model.souffle.*;
import model.souffle.Record;
import org.apache.commons.lang3.tuple.Pair;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    String[] inputs =
        generateInputPredicates(patterns).stream()
            .map(p -> p.toString() + "\n")
            .collect(Collectors.toList())
            .toArray(new String[patterns.size()]);

    File opt = FileUtil.createOrClearFile(souffle_output);
    FileUtil.appendTo(opt, types);
    FileUtil.appendTo(opt, predicates);
    FileUtil.appendTo(opt, inputs);
    FileUtil.appendTo(opt, "\n");

    List<ConfigLinkBlock> blocks = pair.getRight();
    Map<String, URIPattern> patternMap = new HashMap<>();
    patterns.forEach(pattern -> patternMap.put(pattern.label, pattern));
    List<Component> components = generateComponents(blocks, patternMap);
    for (Component component : components) {
      FileUtil.appendTo(opt, component.toString() + "\n");
    }
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
        AtomicInteger branchCnt = new AtomicInteger();
        pattern.branches.forEach((key, value) -> branchCnt.addAndGet(value.size()));
        for (int i = 0; i < branchCnt.get() + pattern.defaultBranches.size(); i++) {
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

  public static List<IOStmt> generateInputPredicates(List<URIPattern> patterns) {
    List<IOStmt> inputs = new ArrayList<>();
    for (URIPattern pattern : patterns) {
      inputs.add(new IOStmt(false, pattern.label));
    }
    return inputs;
  }

  public static List<Component> generateComponents(
      List<ConfigLinkBlock> blocks, Map<String, URIPattern> patternMap) {
    List<Component> components = new ArrayList<>();
    for (ConfigLinkBlock block : blocks) {
      components.add(getComponent(block, patternMap));
    }
    return components;
  }

  private static Component getComponent(ConfigLinkBlock block, Map<String, URIPattern> patternMap) {
    Component cur =
        new Component(
            block.blockName,
            block.baseBlocks.stream().map(base -> base.blockName).collect(Collectors.toList()));

    for (String predicateName : block.predicateDecls.keySet()) {
      PredicateDecl decl = new PredicateDecl(predicateName);
      List<Set<String>> params = block.paramTypes.get(predicateName);
      for (int i = 0; i < params.size(); i++) {
        decl.addParam("param" + i, params.get(i).isEmpty() ? "symbol" : "URI");
      }
      cur.innerStmts.add(decl.toString());
    }

    for (String predicateName : block.predicateDecls.keySet()) {
      List<ConfigLink> links = block.predicateDecls.get(predicateName);
      for (ConfigLink link : links) {
        cur.innerStmts.add(link.toSouffleCode(patternMap));
      }
    }

    String instanceName = ((CompInitializer) cur.postfixStmts.get(0)).instanceName;
    block.predicateTypes.forEach(
        (predicateName, type) -> {
          if (type == PredicateType.PUBLIC) {
            cur.postfixStmts.add(new IOStmt(true, instanceName + "." + predicateName));
          }
        });
    return cur;
  }
}
