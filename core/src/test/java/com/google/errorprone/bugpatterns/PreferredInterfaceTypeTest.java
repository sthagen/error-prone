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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PreferredInterfaceType}. */
@RunWith(JUnit4.class)
public final class PreferredInterfaceTypeTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(PreferredInterfaceType.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(PreferredInterfaceType.class, getClass());

  @Test
  public void assigned() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.graph.Graph;",
            "import com.google.common.graph.GraphBuilder;",
            "import com.google.common.graph.ImmutableGraph;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  private static final Iterable<Integer> FOO = new ArrayList<>();",
            "  public static final Iterable<Integer> BAR = new ArrayList<>();",
            "  public static final Iterable<Integer> RAW = new ArrayList<>();",
            "  public static final Graph<Integer> GRAPH ="
                + " ImmutableGraph.copyOf(GraphBuilder.undirected().build());",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.graph.Graph;",
            "import com.google.common.graph.GraphBuilder;",
            "import com.google.common.graph.ImmutableGraph;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  private static final List<Integer> FOO = new ArrayList<>();",
            "  public static final List<Integer> BAR = new ArrayList<>();",
            "  public static final List<Integer> RAW = new ArrayList<>();",
            "  public static final ImmutableGraph<Integer> GRAPH ="
                + " ImmutableGraph.copyOf(GraphBuilder.undirected().build());",
            "}")
        .doTest();
  }

  @Test
  public void assignedMultipleTypesCompatibleWithSuper() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.Collection;",
            "import java.util.HashSet;",
            "class Test {",
            "  void test() {",
            "    Collection<Integer> foo = new ArrayList<>();",
            "    foo = new HashSet<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void referringToCollectionAsIterable_noFinding() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "import java.util.HashSet;",
            "class Test {",
            "  Collection<Integer> test() {",
            "    Iterable<Integer> foo = test();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void alreadyTightenedType() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    List<Integer> foo = new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void parameters_notFlagged() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void test(Iterable<Object> xs) {",
            "    xs = new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonPrivateNonFinalFields_notFlagged() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  Iterable<Object> xs;",
            "  Test(List<Object> xs) {",
            "    this.xs = xs;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalButPrivateFields() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private Iterable<Object> xs;",
            "  void test(List<Object> xs) {",
            "    this.xs = xs;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalButFieldInPrivateClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  private static class Inner {",
            "    // BUG: Diagnostic contains:",
            "    Iterable<Object> xs;",
            "    void test(List<Object> xs) {",
            "      this.xs = xs;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutables() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    List<Integer> foo = ImmutableList.of(1);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    ImmutableList<Integer> foo = ImmutableList.of(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableMap() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    Map<Integer, Integer> foo = ImmutableMap.of(1, 1);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    ImmutableMap<Integer, Integer> foo = ImmutableMap.of(1, 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  Test() { }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeVoid_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void foo() { }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalNonPrivateNonStaticMethodInNonFinalClass_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void providesAnnotatedMethod_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import dagger.Provides;",
            "import java.util.List;",
            "class Test {",
            "  @Provides",
            "  static List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void producesAnnotatedMethod_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import dagger.producers.Produces;",
            "import java.util.List;",
            "class Test {",
            "  @Produces",
            "  static List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalNonPrivateNonStaticMethodInFinalClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "final class Test {",
            "  // BUG: Diagnostic contains: immutable type",
            "  List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalMethodInNonFinalClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: final ImmutableList<String> foo()",
            "  final List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void privateMethodInNonFinalClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: private ImmutableList<String> foo()",
            "  private List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticMethodInNonFinalClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: static ImmutableList<String> foo()",
            "  static List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeImmutableList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  final ImmutableList<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeImmutableCollection_suggestsTighterType() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: convey more information",
            "  final ImmutableCollection<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_singleReturnStatementImmutableList_suggestsImmutableList() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: final ImmutableList<String> foo()",
            "  final List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_singleReturnStatementArrayList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  final List<String> foo() {",
            "    return new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_singleReturnStatementList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  final List<String> foo() {",
            "    return new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_multipleReturnStatementsImmutableList_suggestsImmutableList() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: ImmutableList<String> foo()",
            "  final List<String> foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return ImmutableList.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_multipleReturnStatementsArrayListImmutableList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  final List<String> foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return new ArrayList<>();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      returnTypeList_multipleReturnStatementsImmutableSetImmutableList_suggestsImmutableCollection() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Collection;",
            "class Test {",
            "  // BUG: Diagnostic contains: ImmutableCollection<String> foo()",
            "  final Collection<String> foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return ImmutableSet.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_multipleReturnStatementsImmutableSetImmutableMap_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  final Object foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return ImmutableMap.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      returnTypeList_multipleReturnStatementsImmutableMapImmutableBiMap_suggestsImmutableMap() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableBiMap;",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "class Test {",
            "  // BUG: Diagnostic contains: final ImmutableMap<String, String> foo()",
            "  final Map<String, String> foo() {",
            "    if (true) {",
            "      return ImmutableBiMap.of();",
            "    } else {",
            "      return ImmutableMap.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_insideAnonymousNested_suggestsImmutableList() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.Provider;",
            "import java.util.List;",
            "class Test {",
            "  private static final int getFooLength() {",
            "    final Provider<List<String>> fooProvider = ",
            "      new Provider<List<String>>() {",
            "        @Override",
            "        // BUG: Diagnostic contains: ImmutableList<String> get",
            "        public List<String> get() {",
            "          return ImmutableList.of(\"foo\", \"bar\");",
            "        }",
            "      };",
            "    List<String> foo = fooProvider.get();",
            "    return foo.size();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_anonymousLambda_suggestsNothing() {
    testHelper
        .addSourceLines(
            "ApplyInterface.java",
            "import java.util.function.Function;",
            "import java.util.List;",
            "public interface ApplyInterface {",
            "  int applyAndGetSize(Function<String, List<String>> fun);",
            "}")
        .addSourceLines(
            "ApplyImpl.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.function.Function;",
            "import java.util.List;",
            "public class ApplyImpl implements ApplyInterface {",
            "  public int applyAndGetSize(Function<String, List<String>> fun) {",
            "    List<String> result = fun.apply(\"foo,bar\");",
            "    return result.size();",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.function.Function;",
            "import java.util.List;",
            "class Test {",
            "  private static final ApplyInterface APPLY = new ApplyImpl();",
            "  private int doApply() {",
            "    int result = APPLY.applyAndGetSize(str -> {",
            "        ImmutableList<String> res = ImmutableList.of(str, str);",
            "        return res;",
            "      });",
            "    return result;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overridingMethod_specialNotice() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.Provider;",
            "import java.util.List;",
            "class Test {",
            "  private static final int getFooLength() {",
            "    final Provider<List<String>> fooProvider = ",
            "      new Provider<List<String>>() {",
            "        @Override",
            "        // BUG: Diagnostic contains: even when overriding a method",
            "        public List<String> get() {",
            "          return ImmutableList.of(\"foo\", \"bar\");",
            "        }",
            "      };",
            "    List<String> foo = fooProvider.get();",
            "    return foo.size();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void iterable_givenMoreTypeInformationAvailable_refactored() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  final ImmutableList<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableMap_notReplacedWithBiMap() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableBiMap;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  final ImmutableMap<String, String> foo() {",
            "    return ImmutableBiMap.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void diagnosticMessage_whenReplacingWithNonImmutableType() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ArrayListMultimap;",
            "import com.google.common.collect.HashMultimap;",
            "import com.google.common.collect.Multimap;",
            "import com.google.common.collect.ImmutableListMultimap;",
            "import com.google.common.collect.ImmutableMultimap;",
            "import com.google.common.collect.ImmutableSetMultimap;",
            "class Test {",
            "  // BUG: Diagnostic contains: convey more information",
            "  final Multimap<?, ?> foo() {",
            "    return ArrayListMultimap.create();",
            "  }",
            "  // BUG: Diagnostic contains: convey more information",
            "  final Multimap<?, ?> bar() {",
            "    return HashMultimap.create();",
            "  }",
            "  // BUG: Diagnostic contains: convey more information",
            "  final ImmutableMultimap<?, ?> baz() {",
            "    return ImmutableListMultimap.of();",
            "  }",
            "  // BUG: Diagnostic contains: convey more information",
            "  final ImmutableMultimap<?, ?> quux() {",
            "    return ImmutableSetMultimap.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalIterableInitializedInDeclarationWithImmutableSetOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> FOO =",
            "  static final Iterable<String> FOO = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void bind() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import java.util.List;",
            "class Test {",
            "   @Bind ",
            "   private static final List<String> LABELS = ImmutableList.of(\"MiniCluster\");",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalMapInitializedInDeclarationWithImmutableBiMapOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableBiMap;",
            "import java.util.Map;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableMap<String, String> FOO =",
            "  static final Map<String, String> FOO = ImmutableBiMap.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalSetInitializedInDeclarationWithImmutableSetOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> FOO =",
            "  static final Set<String> FOO = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalRawSetInitializedInDeclarationWithImmutableSetOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet FOO =",
            "  static final Set FOO = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalSetInitializedInDeclarationWithImmutableSetBuilder() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> FOO =",
            "  static final Set<String> FOO = ImmutableSet.<String>builder().build();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalListInitializedInDeclarationWithImmutableListOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableList<String> FOO =",
            "  static final List<String> FOO = ImmutableList.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalMapInitializedInDeclarationWithImmutableMapOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableMap<Integer, String> FOO =",
            "  static final Map<Integer, String> FOO = ImmutableMap.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalImmutableSetInitializedInDeclarationWithImmutableSet_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  static final ImmutableSet<String> FOO = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalSetInitializedInStaticBlock() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  private static final ImmutableSet<String> FOO;",
            "  static {",
            "    FOO = ImmutableSet.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonStatic() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  final ImmutableSet<String> NON_STATIC = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void nonFinal() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  static Set<String> NON_FINAL = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void nonCapitalCase_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  static final ImmutableSet<String> nonCapitalCase = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void mutable_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  static Iterable<String> mutable = new ArrayList<>();",
            "}")
        .doTest();
  }

  @Test
  public void replacementNotSubtypeOfDeclaredType_noFinding() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Deque;",
            "import java.util.LinkedList;",
            "class Test {",
            "  private final Deque<String> foos = new LinkedList<>();",
            "}")
        .doTest();
  }

  @Test
  public void charSequences() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: String",
            "  private final CharSequence a = \"foo\";",
            "}")
        .doTest();
  }

  @Test
  public void obeysKeep() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.annotations.Keep;",
            "import java.util.ArrayList;",
            "class Test {",
            "  @Keep private static final Iterable<Integer> FOO = new ArrayList<>();",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
