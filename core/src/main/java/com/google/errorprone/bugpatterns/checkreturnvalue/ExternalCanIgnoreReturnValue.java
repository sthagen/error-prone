/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ApiFactory.fullyErasedAndUnannotatedType;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.Rules.ErrorProneMethodRule;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.List;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/** External source of information about @CanIgnoreReturnValue-equivalent API's. */
public final class ExternalCanIgnoreReturnValue extends ErrorProneMethodRule {

  /** Returns a rule using an external list of APIs to ignore. */
  public static ResultUseRule<VisitorState, Symbol> externalIgnoreList() {
    return new ExternalCanIgnoreReturnValue();
  }

  private ExternalCanIgnoreReturnValue() {}

  private static final String EXTERNAL_API_EXCLUSION_LIST = "CheckReturnValue:ApiExclusionList";
  private static final String EXCLUSION_LIST_PARSER = "CheckReturnValue:ApiExclusionListParser";

  private static final Supplier<MethodPredicate> EXTERNAL_RULE_EVALUATOR =
      VisitorState.memoize(
          state ->
              state
                  .errorProneOptions()
                  .getFlags()
                  .get(EXTERNAL_API_EXCLUSION_LIST)
                  .filter(s -> !s.isEmpty())
                  .map(
                      filename ->
                          loadConfigListFromFile(
                              filename,
                              state
                                  .errorProneOptions()
                                  .getFlags()
                                  .getEnum(EXCLUSION_LIST_PARSER, ConfigParser.class)
                                  .orElse(ConfigParser.AS_STRINGS)))
                  .orElse((m, s) -> false));

  @Override
  public String id() {
    return "EXTERNAL_API_EXCLUSION_LIST";
  }

  @Override
  public Optional<ResultUsePolicy> evaluateMethod(MethodSymbol method, VisitorState state) {
    return EXTERNAL_RULE_EVALUATOR.get(state).methodMatches(method, state)
        ? Optional.of(ResultUsePolicy.UNSPECIFIED)
        : Optional.empty();
  }

  /** Encapsulates asking "does this API match the list of APIs I care about"? */
  @FunctionalInterface
  interface MethodPredicate {
    boolean methodMatches(MethodSymbol methodSymbol, VisitorState state);
  }

  // TODO(b/232240203): Api Parsing at analysis time is expensive - there are many ways to
  // load and use the config file.
  // Decide on what works best, taking into account hit rate, load time, etc.
  enum ConfigParser {
    AS_STRINGS {
      @Override
      MethodPredicate load(String file) throws IOException {
        return configByInterpretingMethodsAsStrings(MoreFiles.asCharSource(Paths.get(file), UTF_8));
      }
    },
    PARSE_TOKENS {
      @Override
      MethodPredicate load(String file) throws IOException {
        return configByParsingApiObjects(MoreFiles.asCharSource(Paths.get(file), UTF_8));
      }
    };

    abstract MethodPredicate load(String file) throws IOException;
  }

  static MethodPredicate loadConfigListFromFile(String filename, ConfigParser configParser) {
    try {
      return configParser.load(filename);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Could not load external resource for CanIgnoreReturnValue", e);
    }
  }

  private static MethodPredicate configByInterpretingMethodsAsStrings(CharSource file)
      throws IOException {
    ImmutableSet<String> apis;
    // NB: No whitespace stripping here
    try (Stream<String> lines = file.lines()) {
      apis = lines.collect(toImmutableSet());
    }
    return new MethodPredicate() {
      @Override
      public boolean methodMatches(MethodSymbol methodSymbol, VisitorState state) {
        // Construct an API identifier for this method, which involves erasing parameter types
        return apis.contains(apiSignature(methodSymbol, state.getTypes()));
      }

      private String apiSignature(MethodSymbol methodSymbol, Types types) {
        return methodSymbol.owner.getQualifiedName()
            + "#"
            + methodNameAndParams(methodSymbol, types);
      }
    };
  }

  private static MethodPredicate configByParsingApiObjects(CharSource file) throws IOException {
    ImmutableSetMultimap<String, Api> apis;
    try (Stream<String> lines = file.lines()) {
      apis = lines.map(Api::parse).collect(toImmutableSetMultimap(Api::className, api -> api));
    }
    return (methodSymbol, state) ->
        apis.get(surroundingClass(methodSymbol)).stream()
            .anyMatch(
                api ->
                    methodSymbol.getSimpleName().contentEquals(api.methodName())
                        && methodParametersMatch(
                            api.parameterTypes(), methodSymbol.params(), state.getTypes()));
  }

  public static String surroundingClass(MethodSymbol methodSymbol) {
    return methodSymbol.enclClass().getQualifiedName().toString();
  }

  public static String methodNameAndParams(MethodSymbol methodSymbol, Types types) {
    return methodSymbol.name + "(" + paramsString(types, methodSymbol.params()) + ")";
  }

  private static boolean methodParametersMatch(
      ImmutableList<String> parameters, List<VarSymbol> methodParams, Types types) {
    return Iterables.elementsEqual(
        parameters,
        Iterables.transform(methodParams, p -> fullyErasedAndUnannotatedType(p.type, types)));
  }

  private static String paramsString(Types types, List<VarSymbol> params) {
    if (params.isEmpty()) {
      return "";
    }
    return String.join(
        ",", Iterables.transform(params, p -> fullyErasedAndUnannotatedType(p.type, types)));
  }
}
