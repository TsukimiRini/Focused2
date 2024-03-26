import model.CSTTree;
import model.Language;
import model.SharedStatus;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class CSTBuilderTest {
  @Test
  public void testCSTBuilder() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
        "android", System.getProperty("user.home") + "/coding/xll/android/CloudReader");
    Map<Language, Map<String, CSTTree>> cstTrees = CSTBuilder.buildCST();
    System.out.println(cstTrees);
  }

  @Test
  public void testCSTBuilderNG() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
            "android", System.getProperty("user.home") + "/coding/xll/android/CloudReader");
    Map<Language, Map<String, CSTTree>> cstTrees = CSTBuilderNG.buildCST();
    System.out.println(cstTrees);
  }

  @Test
  public void testWebCST() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
            "web", System.getProperty("user.home") + "/coding/xll/web/latex-css");
    Map<Language, Map<String, CSTTree>> cstTrees = CSTBuilder.buildCST();
    System.out.println(cstTrees);
  }

  @Test
  public void testWebNG() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
            "web", System.getProperty("user.home") + "/coding/xll/web/latex-css");
    Map<Language, Map<String, CSTTree>> cstTrees = CSTBuilderNG.buildCST();
    System.out.println(cstTrees);
  }

  @Test
  public void testCPython() throws UnsupportedEncodingException {
//    SharedStatus.initProjectInfo(
//            "cpython", System.getProperty("user.home") + "/Projects/ffi-navigator/tests/dgl");
    SharedStatus.initProjectInfo(
            "cpython", System.getProperty("user.home") + "/coding/xll/cpython/python-ldap");
    Map<Language, Map<String, CSTTree>> cstTrees = CSTBuilderNG.buildCST();
    System.out.println(cstTrees);
  }
}
