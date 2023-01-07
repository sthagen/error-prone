/*
 * Copyright 2012 The Error Prone Authors.
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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class StaticQualifiedUsingExpressionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(StaticQualifiedUsingExpression.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(StaticQualifiedUsingExpression.class, getClass());

  @Test
  public void positiveCase1() {
    compilationHelper.addSourceFile("StaticQualifiedUsingExpressionPositiveCase1.java").doTest();
  }

  @Test
  public void positiveCase2() {
    compilationHelper.addSourceFile("StaticQualifiedUsingExpressionPositiveCase2.java").doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper.addSourceFile("StaticQualifiedUsingExpressionNegativeCases.java").doTest();
  }

  @Test
  public void clash() {
    refactoringHelper
        .addInputLines(
            "a/Lib.java", //
            "package a;",
            "public class Lib {",
            "  public static final int CONST = 0;",
            "}")
        .expectUnchanged()
        .addInputLines(
            "b/Lib.java", //
            "package b;",
            "public class Lib {",
            "  public static final int CONST = 0;",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            "import a.Lib;",
            "class Test {",
            "  void test() {",
            "    int x = Lib.CONST + new b.Lib().CONST;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import a.Lib;",
            "class Test {",
            "  void test() {",
            "    new b.Lib();",
            "    int x = Lib.CONST + b.Lib.CONST;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void expr() {
    refactoringHelper
        .addInputLines(
            "I.java", //
            "interface I {",
            "  int CONST = 42;",
            "  I id();",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java", //
            "class Test {",
            "  void f(I i) {",
            "    System.err.println(((I) null).CONST);",
            "    System.err.println(i.id().CONST);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "class Test {",
            "  void f(I i) {",
            "    System.err.println(I.CONST);",
            "    i.id();",
            "    System.err.println(I.CONST);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void superAccess() {
    refactoringHelper
        .addInputLines(
            "I.java", //
            "interface I {",
            "  interface Builder {",
            "    default void f() {}",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java", //
            "interface J extends I {",
            "  interface Builder extends I.Builder {",
            "    default void f() {}",
            "    default void aI() {",
            "      I.Builder.super.f();",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void enumConstantAccessedViaInstance() {
    refactoringHelper
        .addInputLines(
            "Enum.java", //
            "enum Enum {",
            "A, B;",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  Enum foo(Enum e) {",
            "    return e.B;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  Enum foo(Enum e) {",
            "    return Enum.B;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualified() {
    refactoringHelper
        .addInputLines(
            "C.java",
            "class C {",
            " static Object x;",
            " void f() {",
            "   Object x = this.x;",
            " }",
            " void g() {",
            "   Object y = this.x;",
            " }",
            "}")
        .addOutputLines(
            "C.java",
            "class C {",
            " static Object x;",
            " void f() {",
            "   Object x = C.x;",
            " }",
            " void g() {",
            "   Object y = x;",
            " }",
            "}")
        .doTest();
  }
}
