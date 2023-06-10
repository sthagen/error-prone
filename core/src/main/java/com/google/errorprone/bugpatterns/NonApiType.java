/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.predicates.TypePredicates.anyOf;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.predicates.TypePredicates.isExactType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.methodIsPublicAndNotAnOverride;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayDeque;
import java.util.Deque;

/** Flags instances of non-API types from being accepted or returned in public APIs. */
@BugPattern(
    summary = "Certain types should not be passed across public API boundaries.",
    // something about reducing build visibility
    severity = WARNING)
public final class NonApiType extends BugChecker implements MethodTreeMatcher {
  // TODO(kak): consider creating an annotation (e.g., `@NonApiType` or `@NotForPublicApi`) that
  // users could apply to their own types.

  private static final String FLOGGER_LINK = "";
  private static final String TYPE_GENERALITY_LINK = "";
  private static final String INTERFACES_NOT_IMPLS_LINK = "";
  private static final String PRIMITIVE_ARRAYS_LINK = "";
  private static final String PROTO_TIME_SERIALIZATION_LINK = "";
  private static final String OPTIONAL_AS_PARAM_LINK = "";
  private static final String PREFER_JDK_OPTIONAL_LINK = "";

  private static final ImmutableSet<TypeToCheck> NON_API_TYPES =
      ImmutableSet.of(
          // primitive arrays
          withPublicVisibility(
              anyOf(
                  (t, s) -> isSameType(t, s.getTypes().makeArrayType(s.getSymtab().intType), s),
                  (t, s) -> isSameType(t, makeArrayType("java.lang.Integer", s), s)),
              "Prefer an ImmutableIntArray instead. " + PRIMITIVE_ARRAYS_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              anyOf(
                  (t, s) -> isSameType(t, s.getTypes().makeArrayType(s.getSymtab().doubleType), s),
                  (t, s) -> isSameType(t, makeArrayType("java.lang.Double", s), s)),
              "Prefer an ImmutableDoubleArray instead. " + PRIMITIVE_ARRAYS_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              anyOf(
                  (t, s) -> isSameType(t, s.getTypes().makeArrayType(s.getSymtab().longType), s),
                  (t, s) -> isSameType(t, makeArrayType("java.lang.Long", s), s)),
              "Prefer an ImmutableLongArray instead. " + PRIMITIVE_ARRAYS_LINK,
              ApiElementType.ANY),

          // Optionals
          withPublicVisibility(
              isExactType("java.util.Optional"),
              "Avoid Optional parameters. " + OPTIONAL_AS_PARAM_LINK,
              ApiElementType.PARAMETER),
          withPublicVisibility(
              isExactType("com.google.common.base.Optional"),
              "Prefer a java.util.Optional instead. " + PREFER_JDK_OPTIONAL_LINK,
              ApiElementType.ANY),

          // ImmutableFoo as params
          withPublicVisibility(
              isExactType("com.google.common.collect.ImmutableCollection"),
              "Consider accepting a java.util.Collection or Iterable instead. "
                  + TYPE_GENERALITY_LINK,
              ApiElementType.PARAMETER),
          withPublicVisibility(
              isExactType("com.google.common.collect.ImmutableList"),
              "Consider accepting a java.util.List or Iterable instead. " + TYPE_GENERALITY_LINK,
              ApiElementType.PARAMETER),
          withPublicVisibility(
              isExactType("com.google.common.collect.ImmutableSet"),
              "Consider accepting a java.util.Set or Iterable instead. " + TYPE_GENERALITY_LINK,
              ApiElementType.PARAMETER),
          withPublicVisibility(
              isExactType("com.google.common.collect.ImmutableMap"),
              "Consider accepting a java.util.Map instead. " + TYPE_GENERALITY_LINK,
              ApiElementType.PARAMETER),

          // collection implementation classes
          withAnyVisibility(
              anyOf(isExactType("java.util.ArrayList"), isExactType("java.util.LinkedList")),
              "Prefer a java.util.List instead. " + INTERFACES_NOT_IMPLS_LINK,
              ApiElementType.ANY),
          withAnyVisibility(
              anyOf(
                  isExactType("java.util.HashSet"),
                  isExactType("java.util.LinkedHashSet"),
                  isExactType("java.util.TreeSet")),
              "Prefer a java.util.Set instead. " + INTERFACES_NOT_IMPLS_LINK,
              ApiElementType.ANY),
          withAnyVisibility(
              anyOf(
                  isExactType("java.util.HashMap"),
                  isExactType("java.util.LinkedHashMap"),
                  isExactType("java.util.TreeMap")),
              "Prefer a java.util.Map instead. " + INTERFACES_NOT_IMPLS_LINK,
              ApiElementType.ANY),

          // ProtoTime
          withPublicVisibility(
              isExactType("com.google.protobuf.Duration"),
              "Prefer a java.time.Duration instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              isExactType("com.google.protobuf.Timestamp"),
              "Prefer a java.time.Instant instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              isExactType("com.google.type.Date"),
              "Prefer a java.time.LocalDate instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              isExactType("com.google.type.DateTime"),
              "Prefer a java.time.LocalDateTime instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              isExactType("com.google.type.DayOfWeek"),
              "Prefer a java.time.DayOfWeek instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              isExactType("com.google.type.Month"),
              "Prefer a java.time.Month instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              isExactType("com.google.type.TimeOfDay"),
              "Prefer a java.time.LocalTime instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          withPublicVisibility(
              isExactType("com.google.type.TimeZone"),
              "Prefer a java.time.ZoneId instead. " + PROTO_TIME_SERIALIZATION_LINK,
              ApiElementType.ANY),
          // TODO(kak): consider com.google.type.Interval -> Range<Instant>

          // Flogger
          withAnyVisibility(
              anyOf(
                  isDescendantOf("com.google.common.flogger.FluentLogger"),
                  isDescendantOf("com.google.common.flogger.GoogleLogger"),
                  isDescendantOf("com.google.common.flogger.android.AndroidFluentLogger")),
              "There is no advantage to passing around a logger rather than declaring one in the"
                  + " class that needs it. "
                  + FLOGGER_LINK,
              ApiElementType.ANY));

  private static Type makeArrayType(String typeName, VisitorState state) {
    return state.getTypes().makeArrayType(state.getTypeFromString(typeName));
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    boolean isPublicApi =
        methodIsPublicAndNotAnOverride(getSymbol(tree), state)
            && state.errorProneOptions().isPubliclyVisibleTarget();

    for (Tree parameter : tree.getParameters()) {
      checkType(parameter, ApiElementType.PARAMETER, isPublicApi, state);
    }
    checkType(tree.getReturnType(), ApiElementType.RETURN_TYPE, isPublicApi, state);

    // the accumulated matches (if any) are reported via state.reportMatch(...)
    return NO_MATCH;
  }

  private void checkType(
      Tree tree, ApiElementType elementType, boolean isPublicApi, VisitorState state) {
    if (isSuppressed(tree, state)) {
      return;
    }
    Type type = getType(tree);
    if (type == null) {
      return;
    }
    for (TypeToCheck typeToCheck : NON_API_TYPES) {
      if (typeToCheck.matches(type, state)) {
        if (typeToCheck.elementType() == ApiElementType.ANY
            || typeToCheck.elementType() == elementType) {
          if (isPublicApi || typeToCheck.visibility() == ApiVisibility.ANY) {
            state.reportMatch(
                buildDescription(tree).setMessage(typeToCheck.failureMessage()).build());
          }
        }
      }
    }
  }

  enum ApiElementType {
    PARAMETER,
    RETURN_TYPE,
    ANY
  }

  enum ApiVisibility {
    PUBLIC,
    ANY
  }

  private static TypeToCheck withPublicVisibility(
      TypePredicate typePredicate, String failureMessage, ApiElementType elementType) {
    return new AutoValue_NonApiType_TypeToCheck(
        typePredicate, failureMessage, ApiVisibility.PUBLIC, elementType);
  }

  private static TypeToCheck withAnyVisibility(
      TypePredicate typePredicate, String failureMessage, ApiElementType elementType) {
    return new AutoValue_NonApiType_TypeToCheck(
        typePredicate, failureMessage, ApiVisibility.ANY, elementType);
  }

  @AutoValue
  abstract static class TypeToCheck {

    final boolean matches(Type type, VisitorState state) {
      Deque<Type> types = new ArrayDeque<>();
      types.add(type);
      while (!types.isEmpty()) {
        Type head = types.poll();
        if (typePredicate().apply(head, state)) {
          return true;
        }
        types.addAll(head.getTypeArguments());
      }
      // TODO(kak): do we want to check var-args as well?
      return false;
    }

    abstract TypePredicate typePredicate();

    abstract String failureMessage();

    abstract ApiVisibility visibility();

    abstract ApiElementType elementType();
  }
}
