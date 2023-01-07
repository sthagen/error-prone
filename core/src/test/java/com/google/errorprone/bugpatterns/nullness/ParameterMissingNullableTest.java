/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ParameterMissingNullable}Test */
@RunWith(JUnit4.class)
public class ParameterMissingNullableTest {
  private final CompilationTestHelper conservativeHelper =
      CompilationTestHelper.newInstance(ParameterMissingNullable.class, getClass());
  private final CompilationTestHelper aggressiveHelper =
      CompilationTestHelper.newInstance(ParameterMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");
  private final BugCheckerRefactoringTestHelper aggressiveRefactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ParameterMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");

  @Test
  public void positiveIf() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveIfWithUnrelatedThrow() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(boolean b, Integer i) {",
            "    if (b) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      int val = i == null ? 0 : i;",
            "      if (val < 0) {",
            "        throw new RuntimeException();",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveDespiteWhileLoop() {
    // TODO(cpovirk): This doesn't look "positive" to me.
    // TODO(cpovirk): Also, I *think* the lack of braces on the while() loop is intentional?
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import static com.google.common.base.Preconditions.checkArgument;",
            "class Foo {",
            "  void foo(Object o) {",
            "    while (true)",
            "      checkArgument(o != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTernary() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  int i;",
            "  void foo(Integer i) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.i = i == null ? 0 : i;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCallToMethod() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {}",
            "  void bar() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    foo(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCallToTopLevelConstructor() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  Foo(Integer i) {}",
            "  void bar() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    new Foo(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCallToNestedConstructor() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  static class Nested {",
            "    Nested(Integer i) {}",
            "  }",
            "  void bar() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    new Foo.Nested(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCallToNestedConstructor() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  class Nested {",
            "    Nested(Integer i) {}",
            "  }",
            "  void bar() {",
            // TODO(cpovirk): Recognize this.
            "    new Nested(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void declarationAnnotatedLocation() {
    aggressiveRefactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import javax.annotation.Nullable;",
            "class Foo {",
            "  void foo(java.lang.Integer i) {",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import javax.annotation.Nullable;",
            "class Foo {",
            "  void foo(@Nullable java.lang.Integer i) {",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void typeAnnotatedLocation() {
    aggressiveRefactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Foo {",
            "  void foo(java.lang.Integer i) {",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Foo {",
            "  void foo(java.lang.@Nullable Integer i) {",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negativeAlreadyAnnotated() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import javax.annotation.Nullable;",
            "class Foo {",
            "  void foo(@Nullable Integer i) {",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCasesAlreadyTypeAnnotatedInnerClass() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class Foo {",
            "  class Inner {}",
            "  @Nullable Inner message;",
            "  void foo(@Nullable Inner i) {",
            "    if (i == null) {",
            "      return;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativePreconditionCheckMethod() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import static com.google.common.base.Preconditions.checkArgument;",
            "class Foo {",
            "  void foo(Integer i) {",
            "    checkArgument(i != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeOtherCheckMethod() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void assertNot(boolean b) {}",
            "  void foo(Integer i) {",
            "    assertNot(i == null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeAssert() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    assert (i != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCheckNotAgainstNull() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    if (i == 7) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCheckOfNonParameter() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    Integer j = 7;",
            "    if (j == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeThrow() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    if (i == null) {",
            "      throw something();",
            "    }",
            "  }",
            "  RuntimeException something() {",
            "    return new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCreateException() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    if (i == null) {",
            "      throwIt(new RuntimeException());",
            "    }",
            "  }",
            "  void throwIt(RuntimeException x) {",
            "    throw x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLambdaParameter() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "interface Foo {",
            "  Foo FOO = o -> o == null ? 0 : o;",
            "  int toInt(Integer o);",
            "}")
        .doTest();
  }

  @Test
  public void negativeDoWhileLoop() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  Foo next;",
            "  void foo(Foo foo) {",
            "    do {",
            "      foo = foo.next;",
            "    } while (foo != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeWhileLoop() {
    /*
     * It would be safe to annotate this parameter as @Nullable, but it's somewhat unclear whether
     * people would prefer that in most cases. We could consider adding @Nullable if people would
     * find it useful.
     */
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  Foo next;",
            "  void foo(Foo foo) {",
            "    while (foo != null) {",
            "      foo = foo.next;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeForLoop() {
    // Similar to testNegativeWhileLoop, @Nullable would be defensible here.
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  Foo next;",
            "  void foo(Foo foo) {",
            "    for (; foo != null; foo = foo.next) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCallArgNotNull() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {}",
            "  void bar() {",
            "    foo(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCallAlreadyAnnotated() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import javax.annotation.Nullable;",
            "class Foo {",
            "  void foo(@Nullable Integer i) {}",
            "  void bar() {",
            "    foo(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCallTypeVariable() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  <T> void foo(T t) {}",
            "  void bar() {",
            "    foo(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCallOtherCompilationUnit() {
    conservativeHelper
        .addSourceLines(
            "Foo.java", //
            "class Foo {",
            "  void foo(Integer i) {}",
            "}")
        .addSourceLines(
            "Bar.java", //
            "class Bar {",
            "  void bar(Foo foo) {",
            "    foo.foo(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCallVarargs() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer... i) {}",
            "  void bar() {",
            "    foo(null, 1);",
            "  }",
            "}")
        .doTest();
  }
}
