/*
 * Copyright 2015 The Error Prone Authors.
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

/** Test cases for {@link MisusedWeekYear}. */
@RunWith(JUnit4.class)
public class MisusedWeekYearTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MisusedWeekYear.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper.addSourceFile("MisusedWeekYearPositiveCases.java").doTest();
  }

  @Test
  public void positiveCases2() {
    compilationHelper.addSourceFile("MisusedWeekYearPositiveCases2.java").doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper.addSourceFile("MisusedWeekYearNegativeCases.java").doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(MisusedWeekYear.class, getClass())
        .addInputLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {", //
            "  private static final String PATTERN = \"YYYY\";",
            "  static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {", //
            "  private static final String PATTERN = \"yyyy\";",
            "  static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);",
            "}")
        .doTest();
  }
}
