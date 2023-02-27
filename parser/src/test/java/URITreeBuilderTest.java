import model.CSTTree;
import model.Language;
import model.TreeInfo.TreeInfoConf;
import model.URI.URINode;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class URITreeBuilderTest {
  @Test
  public void testURITreeBuilder() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
        "android", System.getProperty("user.home") + "/coding/xll/android/CloudReader");
    Map<Language, Map<String, CSTTree>> cstTrees = CSTBuilder.buildCST();
    TreeInfoConf conf =
        new TreeInfoConf(
            System.getProperty("user.dir") + "/src/test/resources/androidTreeConf.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode tree = builder.buildFromCST(cstTrees.get(Language.JAVA));
    System.out.println(tree.type);
  }
}
