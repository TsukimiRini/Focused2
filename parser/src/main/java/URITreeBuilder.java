import model.CSTTree;
import model.TreeInfo.TreeInfoConf;
import model.URI.URINode;
import utils.FileUtil;

import java.util.Map;

public class URITreeBuilder {
  public TreeInfoConf conf;

  public URITreeBuilder(TreeInfoConf conf) {
    this.conf = conf;
  }

  public URINode buildFromCST(Map<String, CSTTree> tree) {
    URINode uriTree = new URINode("", "ROOT");
    // redundant file path
    tree.forEach((filePath, curTree) -> uriTree.addCST(FileUtil.getRelativePath(filePath), curTree, conf));
    return uriTree;
  }
}
