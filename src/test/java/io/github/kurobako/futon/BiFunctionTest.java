/*
 * Copyright (C) 2015 Fedor Gavrilov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package io.github.kurobako.futon;

import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static io.github.kurobako.futon.Pair.pair;
import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class BiFunctionTest {

  @Theory
  public void test$Pair(final BiFunction<Character, Character, String> mock) {
    assertEquals(mock.$(pair('a', 'b')), mock.$('a', 'b'));
  }

  @Theory()
  public void test$PairNPE(final BiFunction<Character, Character, String> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.$(null);
  }

  @Theory
  public void testFlip(final BiFunction<Character, Character, String> mock) {
    assertEquals(mock.flip().$('a', 'b'), "ba");
  }

  @Theory
  public void testUncurry(final BiFunction<Character, Character, String> mock) {
    assertEquals(mock.$('a', 'b'), mock.curry().$('a').$('b'));
  }

  @Theory
  public void testStaticCurry(final BiFunction<Character, Character, String> mock) {
    Function<Character, Function<Character, String>> curried = mock.curry();
    assertEquals(BiFunction.uncurry(curried).$('a', 'b'), mock.$('a', 'b'));
  }

  @Theory
  public void testStaticCurryNPE(final BiFunction<Character, Character, String> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    BiFunction.uncurry(null);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataPoint
  public static final BiFunction<Character, Character, String> mock = (a, b) -> new String(new char[]{a, b});
}
