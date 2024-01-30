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

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth8;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of internal methods and flag parsing for new-style Error Prone command-line flags. */
@RunWith(JUnit4.class)
public final class ErrorProneFlagsTest {

  @Test
  public void parseAndGetStringValue() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:SomeArg=SomeValue")
            .parseFlag("-XepOpt:Other:Arg:More:Parts=Long")
            .parseFlag("-XepOpt:EmptyArg=")
            .build();
    Truth8.assertThat(flags.get("SomeArg")).hasValue("SomeValue");
    Truth8.assertThat(flags.get("Other:Arg:More:Parts")).hasValue("Long");
    Truth8.assertThat(flags.get("EmptyArg")).hasValue("");
    Truth8.assertThat(flags.get("absent")).isEmpty();
  }

  @Test
  public void parseAndGetBoolean() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            // Boolean parsing should ignore case.
            .parseFlag("-XepOpt:Arg1=tRuE")
            .parseFlag("-XepOpt:Arg2=FaLsE")
            .parseFlag("-XepOpt:Arg3=yes")
            .parseFlag("-XepOpt:Arg4")
            .build();
    Truth8.assertThat(flags.getBoolean("Arg1")).hasValue(true);
    Truth8.assertThat(flags.getBoolean("Arg2")).hasValue(false);
    assertThrows(IllegalArgumentException.class, () -> flags.getBoolean("Arg3"));
    Truth8.assertThat(flags.getBoolean("Arg4")).hasValue(true);
    Truth8.assertThat(flags.getBoolean("absent")).isEmpty();
  }

  @Test
  public void parseAndGetImplicitTrue() {
    ErrorProneFlags flags = ErrorProneFlags.builder().parseFlag("-XepOpt:SomeArg").build();
    Truth8.assertThat(flags.getBoolean("SomeArg")).hasValue(true);
  }

  @Test
  public void parseAndGetInteger() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:Arg1=10")
            .parseFlag("-XepOpt:Arg2=20.6")
            .parseFlag("-XepOpt:Arg3=thirty")
            .build();
    Truth8.assertThat(flags.getInteger("Arg1")).hasValue(10);
    assertThrows(NumberFormatException.class, () -> flags.getInteger("Arg2"));
    assertThrows(NumberFormatException.class, () -> flags.getInteger("Arg3"));
    Truth8.assertThat(flags.getInteger("absent")).isEmpty();
  }

  @Test
  public void parseAndGetList() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:ArgA=1,2,3")
            .parseFlag("-XepOpt:ArgB=4,")
            .parseFlag("-XepOpt:ArgC=5,,,6")
            .parseFlag("-XepOpt:ArgD=7")
            .parseFlag("-XepOpt:ArgE=")
            .build();
    assertThat(flags.getListOrEmpty("ArgA")).containsExactly("1", "2", "3").inOrder();
    assertThat(flags.getListOrEmpty("ArgB")).containsExactly("4", "").inOrder();
    assertThat(flags.getListOrEmpty("ArgC")).containsExactly("5", "", "", "6").inOrder();
    assertThat(flags.getListOrEmpty("ArgD")).containsExactly("7");
    assertThat(flags.getListOrEmpty("ArgE")).containsExactly("");
    assertThat(flags.getListOrEmpty("absent")).isEmpty();
  }

  @Test
  public void plus_secondShouldOverwriteFirst() {
    ErrorProneFlags flags1 =
        ErrorProneFlags.builder().putFlag("a", "FIRST_A").putFlag("b", "FIRST_B").build();
    ErrorProneFlags flags2 =
        ErrorProneFlags.builder().putFlag("b", "b2").putFlag("c", "c2").build();

    ImmutableMap<String, String> expectedCombinedMap =
        ImmutableMap.<String, String>builder()
            .put("a", "FIRST_A")
            .put("b", "b2")
            .put("c", "c2")
            .buildOrThrow();

    ImmutableMap<String, String> actualCombinedMap = flags1.plus(flags2).getFlagsMap();

    assertThat(actualCombinedMap).containsExactlyEntriesIn(expectedCombinedMap);
  }

  @Test
  public void empty() {
    ErrorProneFlags emptyFlags = ErrorProneFlags.empty();
    assertThat(emptyFlags.isEmpty()).isTrue();
    assertThat(emptyFlags.getFlagsMap().isEmpty()).isTrue();

    ErrorProneFlags nonEmptyFlags = ErrorProneFlags.fromMap(ImmutableMap.of("a", "b"));
    assertThat(nonEmptyFlags.isEmpty()).isFalse();
    assertThat(nonEmptyFlags.getFlagsMap().isEmpty()).isFalse();
  }

  /** An enum for testing. */
  public enum Colour {
    RED,
    YELLOW,
    GREEN
  }

  @Test
  public void enumFlags() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:Colour=RED")
            .parseFlag("-XepOpt:Colours=YELLOW,GREEN")
            .parseFlag("-XepOpt:CaseInsensitiveColours=yellow,green")
            .parseFlag("-XepOpt:EmptyColours=")
            .build();
    Truth8.assertThat(flags.getEnum("Colour", Colour.class)).hasValue(Colour.RED);
    Truth8.assertThat(flags.getEnumSet("Colours", Colour.class))
        .hasValue(ImmutableSet.of(Colour.YELLOW, Colour.GREEN));
    Truth8.assertThat(flags.getEnumSet("CaseInsensitiveColours", Colour.class))
        .hasValue(ImmutableSet.of(Colour.YELLOW, Colour.GREEN));
    Truth8.assertThat(flags.getEnumSet("EmptyColours", Colour.class)).hasValue(ImmutableSet.of());
    Truth8.assertThat(flags.getEnumSet("NoSuchColours", Colour.class)).isEmpty();
  }

  @Test
  public void invalidEnumFlags() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:Colour=NOSUCH")
            .parseFlag("-XepOpt:Colours=YELLOW,NOSUCH")
            .build();
    assertThrows(IllegalArgumentException.class, () -> flags.getEnum("Colour", Colour.class));
    assertThrows(IllegalArgumentException.class, () -> flags.getEnumSet("Colours", Colour.class));
  }
}
