package model.config;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigElement extends HashMap<String, String> {
  public List<String> params;
  public Pair<String, List<String>> template;
  public List<String> branches = new ArrayList<>();

  public ConfigElement(List<String> params, Pair<String, List<String>> template) {
    this.params = params;
    this.template = template;
  }

  public void addBranch(String branch) {
    branches.add(branch);
  }
}
