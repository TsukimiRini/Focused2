package model.config;

import model.enums.PredicateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLinkBlock {
  public String blockName;
  public List<ConfigLinkBlock> baseBlocks = new ArrayList<>();
  public Map<String, List<ConfigLink>> predicateDecls = new HashMap<>();
  public Map<String, PredicateType> predicateTypes = new HashMap<>();

  public ConfigLinkBlock(String name) {
    this.blockName = name;
  }

  public void addBaseBlock(ConfigLinkBlock block) {
    baseBlocks.add(block);
  }

  public void addPredicateDecl(ConfigLink link, boolean isPublic) {
    String predicateName = link.decl.predicateName;
    if (!predicateDecls.containsKey(predicateName)) {
      predicateDecls.put(predicateName, new ArrayList<>());
      predicateTypes.put(predicateName, isPublic ? PredicateType.PUBLIC : PredicateType.PRIVATE);
    }
    predicateDecls.get(predicateName).add(link);
  }
}
