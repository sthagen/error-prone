/*
 * Copyright 2020 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit test for {@link UnnecessaryMethodReference}. */
@RunWith(JUnit4.class)
public final class UnnecessaryMethodReferenceTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnnecessaryMethodReference.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryMethodReference.class, getClass());

  @Test
  public void positiveCase() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, Function<Integer, String> fn) {",
            "    // BUG: Diagnostic contains: (fn)",
            "    return xs.map(fn::apply);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCase_refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, Function<Integer, String> fn) {",
            "    return xs.map(fn::apply);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, Function<Integer, String> fn) {",
            "    return xs.map(fn);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithExtraInheritance() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, A fn) {",
            "    // BUG: Diagnostic contains: (fn)",
            "    return xs.map(fn::apply);",
            "  }",
            "  abstract static class A implements Function<Integer, String> {",
            "    @Override",
            "    public String apply(Integer i) {",
            "      return i.toString();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeWithExtraInheritance() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, A fn) {",
            "    return xs.map(fn::frobnicate);",
            "  }",
            "  abstract static class A implements Function<Integer, String> {",
            "    abstract String frobnicate(Integer i);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void withNonAbstractMethodOnInterface() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "abstract class Test {",
            "  void test(A a) {",
            "    // BUG: Diagnostic contains:",
            "    foo(a::foo);",
            "    foo(a::bar);",
            "  }",
            "  abstract void foo(A a);",
            "  interface A {",
            "    String foo(Integer i);",
            "    default String bar(Integer i) {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCaseViaKnownDelegate() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Predicate;",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<Integer> map(Stream<Integer> xs, Predicate<Integer> p) {",
            "    // BUG: Diagnostic contains: (p)",
            "    return xs.filter(p::apply);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCaseViaConvert() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Converter;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, Converter<Integer, String> fn) {",
            "    // BUG: Diagnostic contains: (fn)",
            "    return xs.map(fn::convert);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCaseViaConvert_viaIntermediateType() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Converter;",
            "import com.google.common.base.Function;",
            "class Test {",
            "  void a(Converter<Integer, String> fn) {",
            "    // BUG: Diagnostic contains: b(fn)",
            "    b(fn::convert);",
            "  }",
            "  void b(Function<Integer, String> fn) {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreSuper() {
    helper
        .addSourceLines(
            "S.java", //
            "class S implements Runnable {",
            "  public void run() {}",
            "}")
        .addSourceLines(
            "Test.java",
            "abstract class Test extends S {",
            "  abstract void r(Runnable r);",
            "  public void run() {",
            "    r(super::run);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void subType() {
    helper
        .addSourceLines(
            "T.java",
            "import java.util.function.Consumer;",
            "abstract class T {",
            "  void f(Consumer<String> c) {}",
            "  void g(Consumer<Object> c) {",
            "    f(c::accept);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void range_isJavaPredicate() {
    helper
        .addSourceLines(
            "T.java",
            "import com.google.common.collect.Range;",
            "import java.util.stream.Stream;",
            "abstract class T {",
            "  Stream<Long> g(Stream<Long> x, Range<Long> range) {",
            "    // BUG: Diagnostic contains: filter(range)",
            "    return x.filter(range::contains);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void range_isGuavaPredicate() {
    helper
        .addSourceLines(
            "T.java",
            "import com.google.common.base.Predicate;",
            "import com.google.common.collect.Range;",
            "import java.util.stream.Stream;",
            "abstract class T {",
            "  void g(Range<Long> range) {",
            "    // BUG: Diagnostic contains: b(range)",
            "    b(range::contains);",
            "  }",
            "  abstract void b(Predicate<Long> p);",
            "}")
        .doTest();
  }
}
