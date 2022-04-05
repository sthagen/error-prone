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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link TestExceptionChecker}Test */
@RunWith(JUnit4.class)
public class TestExceptionCheckerTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(TestExceptionChecker.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test(expected = IOException.class, timeout = 0L)",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    Files.readAllBytes(p);",
            "    Files.readAllBytes(p);",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test(timeout = 0L)",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    Files.readAllBytes(p);",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_markerAnnotation() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test(expected = IOException.class)",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    Files.readAllBytes(p);",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void oneStatement() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test(expected = IOException.class)",
            "  public void test() throws Exception {",
            "    Files.readAllBytes(Paths.get(\"NOSUCH\"));",
            "  }",
            "}")
        .addOutputLines(
            "in/ExceptionTest.java",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(Paths.get(\"NOSUCH\")));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void oneStatement_withFullyQualifiedTestAnnotation() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "class ExceptionTest {",
            "  @org.junit.Test(expected = IOException.class)",
            "  public void test() throws Exception {",
            "    Files.readAllBytes(Paths.get(\"NOSUCH\"));",
            "  }",
            "}")
        .addOutputLines(
            "in/ExceptionTest.java",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(Paths.get(\"NOSUCH\")));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void empty() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test(expected = IOException.class)",
            "  public void test() throws Exception {",
            "  }",
            "}")
        .addOutputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "  }",
            "}")
        .doTest();
  }
}
