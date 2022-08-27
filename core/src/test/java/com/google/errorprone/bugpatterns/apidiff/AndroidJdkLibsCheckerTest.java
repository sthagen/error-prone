/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.apidiff;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link AndroidJdkLibsChecker}Test. */
@RunWith(JUnit4.class)
public class AndroidJdkLibsCheckerTest extends Java7ApiCheckerTest {

  private final CompilationTestHelper allowJava8Helper =
      CompilationTestHelper.newInstance(AndroidJdkLibsChecker.class, getClass())
          .setArgs(ImmutableList.of("-XepOpt:Android:Java8Libs"));

  public AndroidJdkLibsCheckerTest() {
    super(AndroidJdkLibsChecker.class);
  }

  @Test
  public void repeatedAnnotationAllowed() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Repeatable;",
            "@Repeatable(Test.Container.class)",
            "public @interface Test {",
            "  public @interface Container {",
            "    Test[] value();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeAnnotationAllowed() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})",
            "public @interface Test {",
            "}")
        .doTest();
  }

  @Test
  public void defaultMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Map;",
            "public class Test {",
            "  abstract static class A implements Map<Object, Object> {}",
            "  abstract static class B implements Map<Object, Object> {",
            "    @Override",
            "    public Object getOrDefault(Object key, Object defaultValue) {",
            "      return null;",
            "    }",
            "  }",
            "  void f(A a, B b) {",
            "    // BUG: Diagnostic contains: java.util.Map#getOrDefault(java.lang.Object,V)"
                + " is not available in Test.A",
            "    a.getOrDefault(null, null);",
            "    b.getOrDefault(null, null); // OK: overrides getOrDefault",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeKind() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  javax.lang.model.type.TypeKind tk;",
            "}")
        .doTest();
  }

  @Test
  public void stopwatchElapsed() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Stopwatch;",
            "import java.util.concurrent.TimeUnit;",
            "public class Test {",
            "  void o() {",
            "    // BUG: Diagnostic contains:",
            "    Stopwatch.createStarted().elapsed();",
            "    Stopwatch.createStarted().elapsed(TimeUnit.MILLISECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowJava8Flag_packageAllowed() {
    allowJava8Helper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "import java.util.stream.Stream;",
            "import com.google.common.base.Predicates;",
            "import java.util.Arrays;",
            "public class Test {",
            "  Duration d = Duration.ofSeconds(10);",
            "  public static void test(Stream s) {",
            "    s.forEach(i -> {});",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowJava8Flag_memberAllowed() {
    allowJava8Helper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "public class Test {",
            "  public static void main(String... args) {",
            "    Arrays.stream(args);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowJava8Flag_memberBanned() {
    allowJava8Helper
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "public class Test {",
            "  public static void test(Stream s) {",
            "    // BUG: Diagnostic contains: parallel",
            "    s.parallel();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowJava8Flag_getTimeZone() {
    allowJava8Helper
        .addSourceLines(
            "Test.java",
            "import java.time.ZoneId;",
            "import java.util.TimeZone;",
            "public class Test {",
            "  public static void test() {",
            "    TimeZone.getTimeZone(\"a\");",
            "    TimeZone.getTimeZone(ZoneId.of(\"a\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowJava8Flag_explicitNestedClass() {
    allowJava8Helper
        .addSourceLines(
            "Test.java",
            "import java.util.Spliterator;",
            "public abstract class Test implements Spliterator.OfInt {",
            "}")
        .doTest();
  }

  @Test
  public void forEach() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "class T {",
            "  void f(Iterable<?> i, Collection<?> c) {",
            "    // BUG: Diagnostic contains: java.lang.Iterable#forEach",
            "    i.forEach(System.err::println);",
            "    // BUG: Diagnostic contains: java.lang.Iterable#forEach",
            "    c.forEach(System.err::println);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowJava8Flag_forEach() {
    allowJava8Helper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "class T {",
            "  void f(Iterable<?> i, Collection<?> c) {",
            "    i.forEach(System.err::println);",
            "    c.forEach(System.err::println);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void moduleInfo() {
    compilationHelper
        .addSourceLines(
            "module-info.java", //
            "module testmodule {",
            "  requires java.base;",
            "}")
        .doTest();
  }
}
