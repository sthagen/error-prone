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

/** Tests for {@link TheoryButNoTheories}. */
@RunWith(JUnit4.class)
public final class TheoryButNoTheoriesTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(TheoryButNoTheories.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(TheoryButNoTheories.class, getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import org.junit.experimental.theories.Theory;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              @Theory
              public void test() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import org.junit.experimental.theories.Theories;
            import org.junit.experimental.theories.Theory;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(Theories.class)
            public class Test {
              @Theory
              public void test() {}
            }
            """)
        .doTest();
  }

  @Test
  public void alreadyParameterized_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.experimental.theories.Theories;
            import org.junit.experimental.theories.Theory;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(Theories.class)
            public class Test {
              @Theory
              public void test() {}
            }
            """)
        .doTest();
  }

  @Test
  public void noParameters_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {}
            """)
        .doTest();
  }
}
