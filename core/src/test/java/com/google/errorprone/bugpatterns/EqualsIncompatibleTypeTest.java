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

import static org.junit.Assume.assumeFalse;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
@RunWith(JUnit4.class)
public class EqualsIncompatibleTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(EqualsIncompatibleType.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper.addSourceFile("EqualsIncompatibleTypePositiveCases.java").doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper.addSourceFile("EqualsIncompatibleTypeNegativeCases.java").doTest();
  }

  @Test
  public void negativeCase_recursive() {
    compilationHelper.addSourceFile("EqualsIncompatibleTypeRecursiveTypes.java").doTest();
  }

  @Test
  public void primitiveBoxingIntoObject() {
    assumeFalse(RuntimeVersion.isAtLeast12()); // https://bugs.openjdk.java.net/browse/JDK-8028563
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void something(boolean b, Object o) {",
            "     o.equals(b);",
            "  }",
            "}")
        .setArgs(Arrays.asList("-source", "1.6", "-target", "1.6"))
        .doTest();
  }

  @Test
  public void i547() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  interface B {}",
            "  <T extends B> void t(T x) {",
            "    // BUG: Diagnostic contains: T and String",
            "    x.equals(\"foo\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void prettyNameForConflicts() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  interface B {}",
            "  interface String {}",
            "  void t(String x) {",
            "    // BUG: Diagnostic contains: types Test.String and java.lang.String",
            "    x.equals(\"foo\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReference_incompatibleTypes_finding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "class Test {",
            "  boolean t(Stream<Integer> xs, String x) {",
            "    // BUG: Diagnostic contains:",
            "    return xs.anyMatch(x::equals);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReference_comparableTypes_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "class Test {",
            "  boolean t(Stream<Integer> xs, Object x) {",
            "    return xs.anyMatch(x::equals);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wildcards_whenIncompatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "",
            "public class Test {",
            "  public void test(Class<? extends Integer> a, Class<? extends String> b) {",
            "    // BUG: Diagnostic contains:",
            "    a.equals(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unconstrainedWildcard_compatibleWithAnything() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "",
            "public class Test {",
            "  public void test(java.lang.reflect.Method m, Class<?> c) {",
            "    TestProtoMessage.class.equals(m.getParameterTypes()[0]);",
            "    TestProtoMessage.class.equals(c);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enumsCanBeEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum E {A, B}",
            "  public void test() {",
            "    E.A.equals(E.B);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void protoBuildersCannotBeEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestOneOfMessage;",
            "",
            "public class Test {",
            "  public void test() {",
            "    // BUG: Diagnostic contains: . Though",
            "    TestProtoMessage.newBuilder().equals(TestProtoMessage.newBuilder());",
            "    // BUG: Diagnostic contains:",
            "    TestProtoMessage.newBuilder().equals(TestOneOfMessage.newBuilder());",
            "    // BUG: Diagnostic contains:",
            "    TestProtoMessage.newBuilder().equals(TestOneOfMessage.getDefaultInstance());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enumsNamedBuilderCanBeEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  enum FooBuilder { A }",
            "  public boolean test(FooBuilder a, FooBuilder b) {",
            "    return a.equals(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void flaggedOff_protoBuildersNotConsideredIncomparable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "",
            "public class Test {",
            "  public void test() {",
            "    TestProtoMessage.newBuilder().equals(TestProtoMessage.newBuilder());",
            "    TestProtoMessage.getDefaultInstance()",
            "        .equals(TestProtoMessage.getDefaultInstance());",
            "  }",
            "}")
        .setArgs("-XepOpt:TypeCompatibility:TreatBuildersAsIncomparable=false")
        .doTest();
  }

  @Test
  public void protoBuilderComparedWithinAutoValue() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract TestProtoMessage.Builder b();",
            "}")
        .addSourceLines(
            "AutoValue_Test.java",
            "import javax.annotation.processing.Generated;",
            "@Generated(\"com.google.auto.value.processor.AutoValueProcessor\")",
            "abstract class AutoValue_Test extends Test {",
            "  @Override",
            "  public boolean equals(Object o) {",
            "    return ((Test) o).b().equals(b());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void predicateIsEqual_incompatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static java.util.function.Predicate.isEqual;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  boolean test(Stream<Long> xs) {",
            "    // BUG: Diagnostic contains:",
            "    return xs.allMatch(isEqual(1));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void predicateIsEqual_compatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static java.util.function.Predicate.isEqual;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  boolean test(Stream<Long> xs) {",
            "    return xs.allMatch(isEqual(1L));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void predicateIsEqual_methodRef() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.function.Predicate;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  boolean test(Function<Long, Predicate<Integer>> fn) {",
            "    // BUG: Diagnostic contains:",
            "    return test(Predicate::isEqual);",
            "  }",
            "}")
        .doTest();
  }
}
