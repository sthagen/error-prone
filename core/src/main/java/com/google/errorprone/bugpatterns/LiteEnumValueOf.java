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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.allOf;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.predicates.TypePredicates.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Points out if #valueOf() is called on a Protocol Buffer Enum.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary =
        "Instead of converting enums to string and back, its numeric value should be used instead"
            + " as it is the stable part of the protocol defined by the enum.",
    severity = WARNING)
public class LiteEnumValueOf extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> PROTO_MSG_VALUE_OF_MATCHER =
      staticMethod()
          .onClass(
              allOf(
                  isDescendantOf("com.google.protobuf.Internal.EnumLite"),
                  not(isDescendantOf("com.google.protobuf.ProtocolMessageEnum")),
                  not(isDescendantOf("com.google.protobuf.AbstractMessageLite.InternalOneOfEnum"))))
          .named("valueOf")
          .withParameters("java.lang.String");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!PROTO_MSG_VALUE_OF_MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    // AutoValue Parcelable generated code uses the API in #createForParcel implementation.
    // We explicitly suppress the matches.
    if (ASTHelpers.getGeneratedBy(state)
        .contains("com.ryanharter.auto.value.parcel.AutoValueParcelExtension")) {
      return Description.NO_MATCH;
    }
    return describeMatch(methodInvocationTree);
  }
}
