/*
 * Copyright 2022 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ASTHelpersSuggestionsTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(ASTHelpersSuggestions.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "Test.java",
            "import com.sun.tools.javac.code.Symbol;",
            "class Test {",
            "  void f(Symbol s) {",
            "    s.isStatic();",
            "    s.packge();",
            "    s.members().anyMatch(x -> x.isStatic());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.util.ASTHelpers.enclosingPackage;",
            "import static com.google.errorprone.util.ASTHelpers.isStatic;",
            "import static com.google.errorprone.util.ASTHelpers.scope;",
            "import com.sun.tools.javac.code.Symbol;",
            "class Test {",
            "  void f(Symbol s) {",
            "    isStatic(s);",
            "    enclosingPackage(s);",
            "    scope(s.members()).anyMatch(x -> isStatic(x));",
            "  }",
            "}")
        .addModules(
            "jdk.compiler/com.sun.tools.javac.code", "jdk.compiler/com.sun.tools.javac.util")
        .doTest();
  }

  @Test
  public void onSymbolSubtype() {
    testHelper
        .addInputLines(
            "Test.java",
            "import com.sun.tools.javac.code.Symbol.VarSymbol;",
            "class Test {",
            "  void f(VarSymbol s) {",
            "    s.isStatic();",
            "    s.packge();",
            "    s.members().anyMatch(x -> x.isStatic());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.util.ASTHelpers.enclosingPackage;",
            "import static com.google.errorprone.util.ASTHelpers.isStatic;",
            "import static com.google.errorprone.util.ASTHelpers.scope;",
            "import com.sun.tools.javac.code.Symbol.VarSymbol;",
            "class Test {",
            "  void f(VarSymbol s) {",
            "    s.isStatic();",
            "    enclosingPackage(s);",
            "    scope(s.members()).anyMatch(x -> isStatic(x));",
            "  }",
            "}")
        .addModules(
            "jdk.compiler/com.sun.tools.javac.code", "jdk.compiler/com.sun.tools.javac.util")
        .doTest();
  }

  @Test
  public void symbolGetEnclosedElements() {
    testHelper
        .addInputLines(
            "Test.java",
            "import com.sun.tools.javac.code.Symbol.ClassSymbol;",
            "class Test {",
            "  void f(ClassSymbol s) {",
            "    s.getEnclosedElements();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.util.ASTHelpers.getEnclosedElements;",
            "import com.sun.tools.javac.code.Symbol.ClassSymbol;",
            "class Test {",
            "  void f(ClassSymbol s) {",
            "    getEnclosedElements(s);",
            "  }",
            "}")
        .addModules(
            "jdk.compiler/com.sun.tools.javac.code", "jdk.compiler/com.sun.tools.javac.util")
        .doTest();
  }

  @Test
  public void selfMatch() {
    testHelper
        .addInputLines(
            "ASTHelpers.java",
            "package com.google.errorprone.util;",
            "import com.sun.tools.javac.code.Symbol;",
            "public final class ASTHelpers {",
            "  public static boolean isStatic(Symbol symbol) {",
            "    return symbol.isStatic();",
            "  }",
            "}")
        .expectUnchanged()
        .addModules(
            "jdk.compiler/com.sun.tools.javac.code", "jdk.compiler/com.sun.tools.javac.util")
        .doTest();
  }
}
