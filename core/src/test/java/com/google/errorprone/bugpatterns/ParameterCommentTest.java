/*
 * Copyright 2017 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ParameterComment}Test */
@RunWith(JUnit4.class)
public class ParameterCommentTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(ParameterComment.class, getClass());

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(ParameterComment.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int x, int y) {}",
            "  {",
            "    f(0/*x*/, 1/*y=*/);",
            "    f(0/*x*/, 1); // y",
            "    f(/* x */ 0, /* y */ 1);",
            "    f(0 /* x */, /* y */ 1);",
            "    f(/* x */ 0, 1 /* y */);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int x, int y) {}",
            "  {",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negative() {
    compilationTestHelper
        .addSourceLines(
            "in/Test.java",
            "class Test {",
            "  void f(int x, int y) {}",
            "  {",
            "    f(/* x= */0, /* y = */1);",
            "    f(0 /*y=*/, 1 /*x=*/); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargs() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int y, int... xs) {}",
            "  {",
            "    f(0/*y*/);",
            "    f(0/*y*/, 1/*xs*/);",
            "    f(0, new int[]{0}/*xs*/);",
            "    f(0, 1, 2/*xs*/, 3/*xs*/);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int y, int... xs) {}",
            "  {",
            "    f(/* y= */ 0);",
            "    f(/* y= */ 0, /* xs= */ 1);",
            "    f(0, /* xs= */ new int[]{0});",
            "    f(0, 1, /* xs= */ 2, /* xs= */ 3);",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void noParams() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "class Test {",
            "  void f() {}",
            "  {",
            "    f();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positiveConstructor() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Test (int x, int y) {}",
            "  {",
            "    new Test(0/*x*/, 1/*y=*/);",
            "    new Test(0/*x*/, 1); // y",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Test (int x, int y) {}",
            "  {",
            "    new Test(/* x= */ 0, /* y= */ 1);",
            "    new Test(/* x= */ 0, /* y= */ 1); ",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parameterComment_doesNotChange_whenNestedComment() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(/* second= */ second));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(/* second= */ second));",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parameterComment_nestedComment() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(second /* second */));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(/* second= */ second));",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negative_multiLineTernary() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public static int foo(int x) {",
            "    int y = true ? ",
            "      foo(/* x= */ x) : foo(/* x= */ x);",
            "    int z = true ? ",
            "      foo(/* x= */ x) :",
            "      foo(/* x= */ x);",
            "    return 0;",
            "  }",
            "}")
        .expectNoDiagnostics()
        .doTest();
  }

  @Test
  public void negative_nestedLambda() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Consumer;",
            "public class Test {",
            "  private void testcase(String s, Consumer<Boolean> c) {",
            "    outer(",
            "        p -> {",
            "          System.out.println(s);",
            "          inner(",
            "              /* myFunc= */ c,",
            "              /* i1= */ 200,",
            "              /* i2= */ 300);",
            "        },",
            "        /* b1= */ true,",
            "        /* b2= */ false);",
            "  }",
            "  private void outer(Consumer<Boolean> myFunc, boolean b1, boolean b2) {}",
            "  private void inner(Consumer<Boolean> myFunc, int i1, int i2) {}",
            "}")
        .expectNoDiagnostics()
        .doTest();
  }

  @Test
  public void matchingCommentsAfterwards() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "class Test {",
            "  public Test a(int a) { return this; }",
            "  public int b(int b) { return 1; }",
            "  public void test(Test x) {",
            "    assertThat(x.a(/* a= */ 1).b(/* b= */ 0)).isEqualTo(1);",
            "    assertThat(x.a(/* a= */ 2).b(/* b= */ 0)).isEqualTo(1);",
            "  }",
            "}")
        .doTest();
  }
}
