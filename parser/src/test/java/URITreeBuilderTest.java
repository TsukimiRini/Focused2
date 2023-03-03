import model.CSTTree;
import model.Language;
import model.TreeInfo.TreeInfoConf;
import model.URI.URINode;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class URITreeBuilderTest {
  @Test
  public void testJava() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
        "android", System.getProperty("user.home") + "/coding/xll/android/CloudReader");
    Map<String, CSTTree> cstTrees =
        CSTBuilder.buildCST(
            Language.JAVA,
            List.of(
                System.getProperty("user.home")
                    + "/coding/xll/android/CloudReader/app/src/main/java/com/example/jingbin/cloudreader/app/CloudReaderApplication.java"));
    TreeInfoConf conf =
        new TreeInfoConf(
            System.getProperty("user.dir") + "/src/test/resources/java.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode tree = builder.buildFromCST(cstTrees);
    URINode fileTree = tree;
    while (!fileTree.type.equals("FILE"))
      fileTree = ((List<URINode>) fileTree.children.values().toArray()[0]).get(0);
    System.out.println(tree.type);
  }
}
