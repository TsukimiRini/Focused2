import model.CSTTree;
import model.Language;
import model.SharedStatus;
import model.TreeInfo.TreeInfoConf;
import model.URI.URINode;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import java.io.FileOutputStream;
import java.io.PrintStream;

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
            CSTBuilder.buildCST(
                    Language.HTML,
                    List.of(
                            System.getProperty("user.home")
                                    + "/coding/xll/static-web/latex-css/index.html"));
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
            CSTBuilder.buildCST(
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
  public void testRust() throws UnsupportedEncodingException, FileNotFoundException {
    // Redirect System.out to a file
    PrintStream out = new PrintStream(new FileOutputStream("1.txt"));
    System.setOut(out);

    SharedStatus.initProjectInfo(
            "rust", System.getProperty("user.home") + "/home/code/projects/playground"
    );
    Map<String, CSTTree> cstTrees =
            CSTBuilderNG.buildCST(
                    Language.Rust,
                    List.of(
                            System.getProperty("user.home")
                                    + "/home/code/projects/playground/src/main.rs"));
    TreeInfoConf conf =
            new TreeInfoConf(System.getProperty("user.dir") + "/src/main/resources/rust.tree");
    URITreeBuilder builder = new URITreeBuilder(conf);
    URINode fileTree = builder.buildFromCST(cstTrees);
    StringBuilder sb = URINode.renderURINode(fileTree);
    System.out.println(sb.toString());
  }
}
