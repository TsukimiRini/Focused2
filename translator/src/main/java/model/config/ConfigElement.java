package model.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigElement extends HashMap<String, String> {
  public List<String> branches = new ArrayList<>();

  public void addBranch(String branch) {
    branches.add(branch);
  }
}
