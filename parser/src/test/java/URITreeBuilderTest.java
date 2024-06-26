import model.CSTTree;
import model.Language;
import model.SharedStatus;
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
                    + "/coding/xll/android/CloudReader/app/src/main/java/com/example/jingbin/cloudreader/ui/gank/child/EverydayFragment.java"));
    TreeInfoConf conf =
        new TreeInfoConf(System.getProperty("user.dir") + "/src/resources/java.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode fileTree = builder.buildFromCST(cstTrees);
    while (!fileTree.type.equals("FILE"))
      fileTree = ((List<URINode>) fileTree.children.values().toArray()[0]).get(0);
    System.out.println(fileTree.type);
  }

  @Test
  public void testXML() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
        "android", System.getProperty("user.home") + "/coding/xll/android/CloudReader");
    Map<String, CSTTree> cstTrees =
        CSTBuilder.buildCST(
            Language.XML,
            List.of(
                System.getProperty("user.home")
                    + "/coding/xll/android/CloudReader/app/src/main/res/layout/footer_item_book.xml"));
    TreeInfoConf conf =
        new TreeInfoConf(System.getProperty("user.dir") + "/src/resources/xml.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode fileTree = builder.buildFromCST(cstTrees);
    while (!fileTree.type.equals("FILE"))
      fileTree = ((List<URINode>) fileTree.children.values().toArray()[0]).get(0);
    System.out.println(fileTree.type);
  }

  @Test
  public void testHTML() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
            "web", System.getProperty("user.home") + "/coding/xll/static-web/latex-css"
    );
    Map<String, CSTTree> cstTrees =
            CSTBuilderNG.buildCST(
                    Language.HTML,
                    List.of(
                            System.getProperty("user.home")
                                    + "/coding/xll/web/latex-css/index.html"));
    TreeInfoConf conf =
            new TreeInfoConf(System.getProperty("user.dir") + "/src/main/resources/html.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode fileTree = builder.buildFromCST(cstTrees);
    while (!fileTree.type.equals("FILE"))
      fileTree = ((List<URINode>) fileTree.children.values().toArray()[0]).get(0);
    System.out.println(fileTree.type);
  }

  @Test
  public void testCSS() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
            "web", System.getProperty("user.home") + "/coding/xll/static-web/latex-css"
    );
    Map<String, CSTTree> cstTrees =
            CSTBuilderNG.buildCST(
                    Language.CSS,
                    List.of(
                            System.getProperty("user.home")
                                    + "/coding/xll/web/latex-css/style.css"));
    TreeInfoConf conf =
            new TreeInfoConf(System.getProperty("user.dir") + "/src/main/resources/css.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode fileTree = builder.buildFromCST(cstTrees);
    while (!fileTree.type.equals("FILE"))
      fileTree = ((List<URINode>) fileTree.children.values().toArray()[0]).get(0);
    System.out.println(fileTree.type);
  }

  @Test
  public void  testPython() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
            "python", System.getProperty("user.home") + "/coding/xll-gt/cpython/projects/dgl/python/dgl/global_config.py"
    );
    Map<String, CSTTree> cstTrees =
            CSTBuilderNG.buildCST(
                    Language.Python,
                    List.of(
                            System.getProperty("user.home")
                                    + "/coding/xll-gt/cpython/projects/tvm/python/tvm/relay/_build_module.py"));
    TreeInfoConf conf =
            new TreeInfoConf(System.getProperty("user.dir") + "/src/main/resources/python.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode fileTree = builder.buildFromCST(cstTrees);
    while (!fileTree.type.equals("FILE"))
      fileTree = ((List<URINode>) fileTree.children.values().toArray()[0]).get(0);
    System.out.println(fileTree.type);
  }

  @Test
  public void testCPP() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
            "cpp", System.getProperty("user.home") + "/coding/xll/cpython/python-ldap"
    );
    Map<String, CSTTree> cstTrees =
            CSTBuilderNG.buildCST(
                    Language.CPP,
                    List.of(
                            System.getProperty("user.home")
                                    + "/coding/xll/cpython/python-ldap/Modules/constants.c"));
    TreeInfoConf conf =
            new TreeInfoConf(System.getProperty("user.dir") + "/src/main/resources/cpp.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode fileTree = builder.buildFromCST(cstTrees);
    while (!fileTree.type.equals("FILE"))
      fileTree = ((List<URINode>) fileTree.children.values().toArray()[0]).get(0);
    System.out.println(fileTree.type);
  }
}
