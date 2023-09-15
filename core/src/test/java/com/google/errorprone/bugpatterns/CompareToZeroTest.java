/*
 * Copyright 2019 The Error Prone Authors.
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

/** Tests for {@link CompareToZero} bugpattern. */
@RunWith(JUnit4.class)
public final class CompareToZeroTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CompareToZero.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(CompareToZero.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean test(Integer i) {",
            "    // BUG: Diagnostic contains: compared",
            "    return i.compareTo(2) == -1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveStaticCompare() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean test(boolean x, boolean y) {",
            "    // BUG: Diagnostic contains: compared",
            "    return Boolean.compare(x, y) == -1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveSuggestionForConsistency() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean test(Integer i) {",
            "    // BUG: Diagnostic contains: consistency",
            "    return i.compareTo(2) <= -1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_gte1_has_1_finding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean test(Integer i) {",
            "    // BUG: Diagnostic matches: KEY",
            "    return i.compareTo(2) >= 1;",
            "  }",
            "}")
        .expectErrorMessage(
            "KEY", msg -> msg.contains("consistency") && !msg.contains("implementation"))
        .doTest();
  }

  @Test
  public void positiveAddition() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int test(Integer i) {",
            "    // BUG: Diagnostic contains:",
            "    return i.compareTo(2) + i.compareTo(3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringConcat_ignored() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  String test(Integer i) {",
            "    return \"\" + i.compareTo(3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void test(Integer i) {",
            "    boolean b1 = i.compareTo(2) == -1;",
            "    boolean b2 = i.compareTo(2) > -1;",
            "    boolean b3 = -1 < i.compareTo(2);",
            "    boolean b4 = i.compareTo(2) < 1;",
            "    boolean b5 = i.compareTo(2) != -1;",
            "    boolean b6 = i.compareTo(2) != 1;",
            "    boolean b7 = i.compareTo(2) <= -1;",
            "    boolean b8 = ((i.compareTo(2))) >= 1;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void test(Integer i) {",
            "    boolean b1 = i.compareTo(2) < 0;",
            "    boolean b2 = i.compareTo(2) >= 0;",
            "    boolean b3 = i.compareTo(2) >= 0;",
            "    boolean b4 = i.compareTo(2) <= 0;",
            "    boolean b5 = i.compareTo(2) >= 0;",
            "    boolean b6 = i.compareTo(2) <= 0;",
            "    boolean b7 = i.compareTo(2) < 0;",
            "    boolean b8 = ((i.compareTo(2))) > 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test(Integer i) {",
            "    boolean b1 = i.compareTo(2) < 0;",
            "    boolean b2 = i.compareTo(2) > 0;",
            "    boolean b3 = i.compareTo(2) == 0;",
            "  }",
            "}")
        .doTest();
  }
}
