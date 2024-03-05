import ai.serenade.treesitter.Languages;
import ai.serenade.treesitter.Parser;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;
import org.junit.jupiter.api.Test;
import org.treesitter.*;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeSitterTest {
  static {
    System.load(
            System.getProperty("user.home")
                    + "/Projects/tree-sitter/java-tree-sitter/libjava-tree-sitter.dylib");
  }

  @Test
  void testWalk() throws UnsupportedEncodingException {
    try (Parser parser = new Parser()) {
      parser.setLanguage(Languages.python());
      try (Tree tree = parser.parseString("def foo(bar, baz):\n  print(bar)\n  print(baz)")) {
        try (TreeCursor cursor = tree.getRootNode().walk()) {
          assertEquals("module", cursor.getCurrentTreeCursorNode().getType());
          assertEquals("module", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoFirstChild());
          assertEquals("function_definition", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoFirstChild());

          assertEquals("def", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals("identifier", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals("parameters", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals(":", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals("block", cursor.getCurrentNode().getType());
          assertEquals("body", cursor.getCurrentFieldName());
          assertEquals(false, cursor.gotoNextSibling());

          assertEquals(true, cursor.gotoParent());
          assertEquals("function_definition", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoFirstChild());
        }
      }
    }
  }

  @Test
  void testNewWalk() throws UnsupportedEncodingException {
    TSParser parser = new TSParser();
    TSLanguage language = new TreeSitterXml();
    parser.setLanguage(language);
    TSTree tree = parser.parseString(null, "<foo><bar/></foo>");
    TSNode root = tree.getRootNode();
    System.out.println(root.getType());
  }

  @Test
  void testNewWalkPython() throws UnsupportedEncodingException {
    TSParser parser = new TSParser();
    TSLanguage language = new TreeSitterPython();
    parser.setLanguage(language);
    TSTree tree = parser.parseString(null, "def foo(bar, baz):\n  print(bar)\n  print(baz)");
    TSNode root = tree.getRootNode();
    TSTreeCursor cursor = new TSTreeCursor(root);
    assertEquals("module", cursor.currentNode().getType());
    assertEquals("module", cursor.currentNode().getType());
    assertEquals(true, cursor.gotoFirstChild());
    assertEquals("function_definition", cursor.currentNode().getType());
    assertEquals(true, cursor.gotoFirstChild());

    assertEquals("def", cursor.currentNode().getType());
    assertEquals(true, cursor.gotoNextSibling());
    assertEquals("identifier", cursor.currentNode().getType());
    assertEquals(true, cursor.gotoNextSibling());
    assertEquals("parameters", cursor.currentNode().getType());
    assertEquals(true, cursor.gotoNextSibling());
    assertEquals(":", cursor.currentNode().getType());
    assertEquals(true, cursor.gotoNextSibling());
    assertEquals("block", cursor.currentNode().getType());
    assertEquals("body", cursor.currentFieldName());
    assertEquals(false, cursor.gotoNextSibling());

    assertEquals(true, cursor.gotoParent());
    assertEquals("function_definition", cursor.currentNode().getType());
    assertEquals(true, cursor.gotoFirstChild());
    System.out.println(root.getType());
  }
}
