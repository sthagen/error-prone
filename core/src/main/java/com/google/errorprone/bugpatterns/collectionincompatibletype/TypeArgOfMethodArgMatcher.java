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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Matches an instance method like {@link Collection#removeAll}, for which we need to extract the
 * type argument to the method argument.
 */
class TypeArgOfMethodArgMatcher extends AbstractCollectionIncompatibleTypeMatcher {

  private final Matcher<ExpressionTree> methodMatcher;
  private final String receiverTypeName;
  private final int receiverTypeArgIndex;
  private final int methodArgIndex;
  private final String methodArgTypeName;
  private final int methodArgTypeArgIndex;

  /**
   * @param receiverTypeName The fully-qualified name of the type of the method receiver whose
   *     descendants to match on
   * @param signature The signature of the method to match on
   * @param receiverTypeArgIndex The index of the type argument that should match the method
   *     argument
   * @param methodArgIndex The index of the method argument whose type argument we should extract
   * @param methodArgTypeName The fully-qualified name of the type of the method argument whose type
   *     argument we should extract
   * @param methodArgTypeArgIndex The index of the type argument to extract from the method argument
   */
  public TypeArgOfMethodArgMatcher(
      String receiverTypeName,
      String signature,
      int receiverTypeArgIndex,
      int methodArgIndex,
      String methodArgTypeName,
      int methodArgTypeArgIndex) {
    this.methodMatcher = instanceMethod().onDescendantOf(receiverTypeName).withSignature(signature);
    this.receiverTypeName = receiverTypeName;
    this.receiverTypeArgIndex = receiverTypeArgIndex;
    this.methodArgIndex = methodArgIndex;
    this.methodArgTypeName = methodArgTypeName;
    this.methodArgTypeArgIndex = methodArgTypeArgIndex;
  }

  @Override
  Matcher<ExpressionTree> methodMatcher() {
    return methodMatcher;
  }

  @Override
  ExpressionTree extractSourceTree(MethodInvocationTree tree, VisitorState state) {
    return Iterables.get(tree.getArguments(), methodArgIndex);
  }

  @Override
  @Nullable ExpressionTree extractSourceTree(MemberReferenceTree tree, VisitorState state) {
    return tree;
  }

  @Override
  Type extractSourceType(MethodInvocationTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        getType(Iterables.get(tree.getArguments(), methodArgIndex)),
        state.getSymbolFromString(methodArgTypeName),
        methodArgTypeArgIndex,
        state.getTypes());
  }

  @Override
  @Nullable Type extractSourceType(MemberReferenceTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        getType(tree).allparams().get(methodArgIndex),
        state.getSymbolFromString(methodArgTypeName),
        methodArgTypeArgIndex,
        state.getTypes());
  }

  @Override
  Type extractTargetType(MethodInvocationTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        ASTHelpers.getReceiverType(tree),
        state.getSymbolFromString(receiverTypeName),
        receiverTypeArgIndex,
        state.getTypes());
  }

  @Override
  @Nullable Type extractTargetType(MemberReferenceTree tree, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        ASTHelpers.getReceiverType(tree),
        state.getSymbolFromString(receiverTypeName),
        receiverTypeArgIndex,
        state.getTypes());
  }

  String getMethodArgTypeName() {
    return methodArgTypeName;
  }

  @Override
  Optional<Fix> buildFix(MatchResult result) {
    String fullyQualifiedType = getMethodArgTypeName();
    String simpleType = Iterables.getLast(Splitter.on('.').split(fullyQualifiedType));
    return Optional.of(
        SuggestedFix.builder()
            .prefixWith(result.sourceTree(), String.format("(%s<?>) ", simpleType))
            .addImport(fullyQualifiedType)
            .build());
  }

  @Override
  public String message(MatchResult result, String sourceType, String targetType) {
    String sourceTreeType = Signatures.prettyType(getType(result.sourceTree()));
    return String.format(
        "Argument '%s' should not be passed to this method; its type %s has a type argument "
            + "%s that is not compatible with its collection's type argument %s",
        result.sourceTree(), sourceTreeType, sourceType, targetType);
  }
}
