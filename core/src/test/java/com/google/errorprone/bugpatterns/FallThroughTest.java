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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FallThrough}Test */
@RunWith(JUnit4.class)
public class FallThroughTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(FallThrough.class, getClass());

  @Test
  public void positive() {
    testHelper.addSourceFile("FallThroughPositiveCases.java").doTest();
  }

  @Test
  public void negative() {
    testHelper.addSourceFile("FallThroughNegativeCases.java").doTest();
  }

  @Test
  public void foreverLoop() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 1:",
            "        for (;;) {}",
            "      case 2:",
            "        break;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void commentInBlock() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 0: {",
            "        // fall through",
            "      }",
            "      case 1: {",
            "        System.err.println();",
            "        // fall through",
            "      }",
            "      case 2:",
            "        break;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void emptyBlock() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(char c, boolean b) {",
            "    switch (c) {",
            "      case 'a': {}",
            "      // fall through",
            "      default:",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void arrowSwitch() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Case { ONE, TWO }",
            "  void m(Case c) {",
            "    switch (c) {",
            "      case ONE -> {}",
            "      case TWO -> {}",
            "      default -> {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Ignore("https://github.com/google/error-prone/issues/2638")
  @Test
  public void i2118() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Case { ONE, TWO }",
            "  void m(Case c) {",
            "    switch (c) {",
            "      case ONE:",
            "        switch (c) {",
            "          case ONE -> m(c);",
            "          case TWO -> m(c);",
            "        }",
            "      default:",
            "        assert false;",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
