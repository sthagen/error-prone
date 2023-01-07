/*
 * Copyright 2013 The Error Prone Authors.
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
 * @author rburny@google.com (Radoslaw Burny)
 */
@RunWith(JUnit4.class)
public class JUnit3TestNotRunTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnit3TestNotRun.class, getClass());
  private final BugCheckerRefactoringTestHelper refactorHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnit3TestNotRun.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper.addSourceFile("JUnit3TestNotRunPositiveCases.java").doTest();
  }

  @Test
  public void misspelledTest() {
    refactorHelper
        .addInputLines(
            "in/PositiveCases.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class PositiveCases extends TestCase {",
            "  public void tesName1() {}",
            "  public void ttestName2() {}",
            "  public void teestName3() {}",
            "  public void tstName4() {}",
            "  public void tetName5() {}",
            "  public void etstName6() {}",
            "  public void tsetName7() {}",
            "  public void teatName8() {}",
            "  public void TestName9() {}",
            "  public void TEST_NAME_10() {}",
            "  public void tesname11() {}",
            "}")
        .addOutputLines(
            "out/PositiveCases.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class PositiveCases extends TestCase {",
            "  public void testName1() {}",
            "  public void testName2() {}",
            "  public void testName3() {}",
            "  public void testName4() {}",
            "  public void testName5() {}",
            "  public void testName6() {}",
            "  public void testName7() {}",
            "  public void testName8() {}",
            "  public void testName9() {}",
            "  public void test_NAME_10() {}",
            "  public void testname11() {}",
            "}")
        .doTest();
  }

  @Test
  public void substitutionShouldBeWellFormed() {
    refactorHelper
        .addInputLines(
            "in/PositiveCases.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class PositiveCases extends TestCase {",
            "  public void tesBasic() {}",
            "  public    void    tesMoreSpaces(  )    {}",
            "  public void",
            "      tesMultiline() {}",
            "}")
        .addOutputLines(
            "out/PositiveCases.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class PositiveCases extends TestCase {",
            "  public void testBasic() {}",
            "  public void testMoreSpaces() {}",
            "  public void testMultiline() {}",
            "}")
        .doTest();
  }

  @Test
  public void privateNamedTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import junit.framework.TestCase;",
            "public class Test extends TestCase {",
            "  // BUG: Diagnostic contains:",
            "  private void testDoesStuff() {}",
            "}")
        .doTest();
  }

  @Test
  public void privateMisspelledTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import junit.framework.TestCase;",
            "public class Test extends TestCase {",
            "  // BUG: Diagnostic contains:",
            "  private void tsetDoesStuff() {}",
            "}")
        .doTest();
  }

  @Test
  public void hasModifiersAndThrows() {
    refactorHelper
        .addInputLines(
            "in/DoesStuffTest.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class DoesStuffTest extends TestCase {",
            "  private static void tsetDoesStuff() throws Exception {}",
            "}")
        .addOutputLines(
            "out/DoesStuffTest.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class DoesStuffTest extends TestCase {",
            "  public void testDoesStuff() throws Exception {}",
            "}")
        .doTest();
  }

  @Test
  public void hasParameters_butOtherwiseLooksLikeATestMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import junit.framework.TestCase;",
            "public class Test extends TestCase {",
            "  // BUG: Diagnostic contains:",
            "  public void testDoesStuff(boolean param) {}",
            "}")
        .doTest();
  }

  @Test
  public void suppressionWorks() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import junit.framework.TestCase;",
            "public class Test extends TestCase {",
            "  @SuppressWarnings(\"JUnit3TestNotRun\")",
            "  public void testDoesStuff(boolean param) {}",
            "}")
        .doTest();
  }

  @Test
  public void hasParameters_butInABaseClass() {
    compilationHelper
        .addSourceLines(
            "TestBase.java",
            "import junit.framework.TestCase;",
            "public class TestBase extends TestCase {",
            "  public void testDoesStuff(boolean param) {}",
            "}")
        .doTest();
  }

  @Test
  public void hasParameters_calledElsewhere_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import junit.framework.TestCase;",
            "public class Test extends TestCase {",
            "  public void testActually() { testDoesStuff(true); }",
            "  public void testDoesStuff(boolean param) {}",
            "}")
        .doTest();
  }

  @Test
  public void hasParameters_isOverride_noFinding() {
    compilationHelper
        .addSourceLines(
            "Foo.java", //
            "interface Foo {",
            "  void testDoesStuff(boolean param);",
            "}")
        .addSourceLines(
            "Test.java",
            "import junit.framework.TestCase;",
            "public class Test extends TestCase implements Foo {",
            "  public void testDoesStuff(boolean param) {}",
            "}")
        .doTest();
  }

  @Test
  public void noModifiers() {
    refactorHelper
        .addInputLines(
            "in/DoesStuffTest.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class DoesStuffTest extends TestCase {",
            "  void tsetDoesStuff() {}",
            "}")
        .addOutputLines(
            "out/DoesStuffTest.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "public class DoesStuffTest extends TestCase {",
            "  public void testDoesStuff() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeCase1() {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase1.java").doTest();
  }

  @Test
  public void negativeCase2() {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase2.java").doTest();
  }

  @Test
  public void negativeCase3() {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase3.java").doTest();
  }

  @Test
  public void negativeCase4() {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase4.java").doTest();
  }

  @Test
  public void negativeCase5() {
    compilationHelper
        .addSourceFile("JUnit3TestNotRunNegativeCase3.java") // needed as a dependency
        .addSourceFile("JUnit3TestNotRunNegativeCase5.java")
        .doTest();
  }
}
