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

import static org.junit.Assert.*;

@RunWith(Theories.class)
public class MaybeTest {

  @Theory
  public void testBind(final Maybe<Object> mock) {
    Maybe<String> transformed = mock.bind(o -> Maybe.just(o.toString()));
    if (mock.isJust()) {
      assertTrue(transformed.isJust());
      assertEquals(transformed.value(), o.toString());
    } else {
      assertTrue(transformed.isNothing());
    }
  }

  @Theory
  public void testBindNPE(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.bind(null);
  }

  @Theory
  public void testZipWithFunction(final Maybe<Object> one, final Maybe<Object> another) {
    Maybe<Pair<Object, Object>> zipped = one.zip(another, (fst, snd) -> Maybe.just(Pair.pair(fst, snd)));
    Object left = zipped.value().left;
    Object right = zipped.value().right;
    if (one.isJust()) assertEquals(left, one.value());
    else assertNull(left);
    if (another.isJust()) assertEquals(right, another.value());
    else assertNull(right);
  }

  @Theory
  public void testZipWithFunctionNPEArg0(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.zip(null, (a, b) -> Maybe.just(a));
  }

  @Theory
  public void testZipWithFunctionNPEArg1(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.zip(Maybe.nothing(), null);
  }

  @Theory
  public void testZip(final Maybe<Object> one, final Maybe<Object> another) {
    Maybe<Pair<Object, Object>> zipped = one.zip(another);
    Object left = zipped.value().left;
    Object right = zipped.value().right;
    if (one.isJust()) assertEquals(left, one.value());
    else assertNull(left);
    if (another.isJust()) assertEquals(right, another.value());
    else assertNull(right);
  }

  @Theory
  public void testZipNPE(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.zip(null);
  }

  @Theory
  public void testFilter(final Maybe<Object> mock) {
    Maybe<Object> filtered = mock.filter(obj -> obj == o);
    if (mock.isJust()) {
      assertTrue(filtered.isJust());
      assertEquals(filtered.value(), mock.value());
    } else {
      assertTrue(filtered.isNothing());
    }
  }

  @Theory
  public void testFilterNPE(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.filter(null);
  }

  @Theory
  public void testAnd(final Maybe<Object> mock) {
    if (mock.isJust()) {
      assertTrue(mock.and(Maybe.just(new Object())).isJust());
      assertTrue(mock.and(Maybe.nothing()).isNothing());
    } else {
      assertTrue(mock.and(Maybe.just(new Object())).isNothing());
      assertTrue(mock.and(Maybe.nothing()).isNothing());
    }
  }

  @Theory
  public void testAndNPE(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.and(null);
  }

  @Theory
  public void testOr(final Maybe<Object> mock) {
    if (mock.isJust()) {
      assertTrue(mock.or(Maybe.just(new Object())).isJust());
      assertTrue(mock.or(Maybe.nothing()).isJust());
    } else {
      assertTrue(mock.or(Maybe.just(new Object())).isJust());
      assertTrue(mock.or(Maybe.nothing()).isNothing());
    }
  }

  @Theory
  public void testOrNPE(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.or(null);
  }

  @Theory
  public void testXor(final Maybe<Object> mock) {
    if (mock.isJust()) {
      assertTrue(mock.xor(Maybe.nothing()).isJust());
      assertTrue(mock.xor(Maybe.just(new Object())).isNothing());
    } else {
      assertTrue(mock.xor(Maybe.nothing()).isNothing());
      assertTrue(mock.xor(Maybe.just(new Object())).isJust());
    }
  }

  @Theory
  public void testXorNPE(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.xor(null);
  }

  @Theory
  public void testJust(final Maybe<Object> mock) {
    if (mock.isJust()) {
      assertFalse(mock.isNothing());
      assertEquals(mock.value(), o);
    }
  }

  @Theory
  public void testNothing(final Maybe<Object> mock) {
    if (mock.isNothing()) {
      assertFalse(mock.isJust());
      assertNull(mock.value());
    }
  }

  @Theory
  public void testMap(final Maybe<Object> mock) {
    if (mock.isJust()) {
      assertEquals(mock.map(o -> o).value(), mock.value());
    } else {
      assertNull(mock.map(o -> o).value());
    }
  }

  @Theory
  public void testMapNPE(final Maybe<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.map(null);
  }

  @Theory
  public void testStaticJoin(final Maybe<Object> mock) {
    assertEquals(Maybe.join(Maybe.just(mock)).value(), mock.value());
  }

  @Theory
  public void testStaticJoinNPE() {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    Maybe.join(null);
  }

  @Theory
  public void testStaticJust() {
    Object value = new Object();
    Maybe<Object> just = Maybe.just(value);
    assertEquals(just.value(), value);
  }

  @Theory
  public void testStaticJustNPE() {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    Maybe.just(null);
  }

  @Theory
  public void testStaticNothing() {
    Maybe<Object> nothing = Maybe.nothing();
    assertTrue(nothing.isNothing());
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final Object o = new Object();

  @DataPoint
  public static final Maybe<Object> just = Maybe.just(o);

  @DataPoint
  public static final Maybe<Object> nothing = Maybe.nothing();

  @DataPoint
  public static final Maybe<Object> justBind = just.bind(Maybe::just);

  @DataPoint
  public static final Maybe<Object> nothingBind = nothing.bind(Maybe::just);

  @DataPoint
  public static final Maybe<Object> justFilterTRUE = just.filter(Predicate.TRUE());

  @DataPoint
  public static final Maybe<Object> justFilterFALSE = just.filter(Predicate.FALSE());

  @DataPoint
  public static final Maybe<Object> nothingFilterTRUE = nothing.filter(Predicate.TRUE());

  @DataPoint
  public static final Maybe<Object> getNothingFilterFALSE = nothing.filter(Predicate.FALSE());

  @DataPoint
  public static final Maybe<Object> justMap = just.map(o -> o);

  @DataPoint
  public static final Maybe<Object> nothingMap = nothing.map(o -> o);

  @DataPoint
  public static final Maybe<Object> justJoin = Maybe.join(Maybe.just(just));

  @DataPoint
  public static final Maybe<Object> nothingJoin = Maybe.join(Maybe.just(nothing));
}
