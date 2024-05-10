import me.tongfei.progressbar.ProgressBar;
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
    for(Map.Entry<String, CSTTree> entry : ProgressBar.wrap(tree.entrySet(), "Building URI Tree")) {
      String filePath = entry.getKey();
      CSTTree curTree = entry.getValue();
      uriTree.addCST(FileUtil.getRelativePath(filePath), curTree, conf);
    }
//    tree.forEach((filePath, curTree) -> uriTree.addCST(FileUtil.getRelativePath(filePath), curTree, conf));

    return uriTree;
  }
}
