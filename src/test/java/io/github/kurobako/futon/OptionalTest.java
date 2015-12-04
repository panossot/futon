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
public class OptionalTest {

  @Theory
  public void testBind(final Optional<Object> mock) {
    Optional<String> transformed = mock.bind(o -> Optional.some(o.toString()));
    if (mock.isSome()) {
      assertTrue(transformed.isSome());
      assertEquals(transformed.value(), o.toString());
    } else {
      assertTrue(transformed.isNone());
    }
  }

  @Theory
  public void testBindNPE(final Optional<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.bind(null);
  }

  @Theory
  public void testApply(final Optional<Object> mock) {
    if (mock.isSome()) {
      assertEquals(mock.apply(Optional.<Function<Object, String>>some(Object::toString)).value(), mock.value().toString());
    } else {
      assertTrue(mock.apply(Optional.some(o -> o)).isNone());
    }

  }

  @Theory
  public void testApplyNPE(final Optional<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.apply(null);
  }

  @Theory
  public void testFilter(final Optional<Object> mock) {
    Optional<Object> filtered = mock.filter(obj -> obj == o);
    if (mock.isSome()) {
      assertTrue(filtered.isSome());
      assertEquals(filtered.value(), mock.value());
    } else {
      assertTrue(filtered.isNone());
    }
  }

  @Theory
  public void testFilterNPE(final Optional<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.filter(null);
  }

  @Theory
  public void testAnd(final Optional<Object> mock) {
    if (mock.isSome()) {
      assertTrue(mock.and(Optional.some(new Object())).isSome());
      assertTrue(mock.and(Optional.none()).isNone());
    } else {
      assertTrue(mock.and(Optional.some(new Object())).isNone());
      assertTrue(mock.and(Optional.none()).isNone());
    }
  }

  @Theory
  public void testAndNPE(final Optional<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.and(null);
  }

  @Theory
  public void testOr(final Optional<Object> mock) {
    if (mock.isSome()) {
      assertTrue(mock.or(Optional.some(new Object())).isSome());
      assertTrue(mock.or(Optional.none()).isSome());
    } else {
      assertTrue(mock.or(Optional.some(new Object())).isSome());
      assertTrue(mock.or(Optional.none()).isNone());
    }
  }

  @Theory
  public void testOrNPE(final Optional<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.or(null);
  }

  @Theory
  public void testXor(final Optional<Object> mock) {
    if (mock.isSome()) {
      assertTrue(mock.xor(Optional.none()).isSome());
      assertTrue(mock.xor(Optional.some(new Object())).isNone());
    } else {
      assertTrue(mock.xor(Optional.none()).isNone());
      assertTrue(mock.xor(Optional.some(new Object())).isSome());
    }
  }

  @Theory
  public void testXorNPE(final Optional<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.xor(null);
  }

  @Theory
  public void testJust(final Optional<Object> mock) {
    if (mock.isSome()) {
      assertFalse(mock.isNone());
      assertEquals(mock.value(), o);
    }
  }

  @Theory
  public void testNothing(final Optional<Object> mock) {
    if (mock.isNone()) {
      assertFalse(mock.isSome());
      assertNull(mock.value());
    }
  }

  @Theory
  public void testMap(final Optional<Object> mock) {
    if (mock.isSome()) {
      assertEquals(mock.map(o -> o).value(), mock.value());
    } else {
      assertNull(mock.map(o -> o).value());
    }
  }

  @Theory
  public void testMapNPE(final Optional<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.map(null);
  }

  @Theory
  public void testStaticJoin(final Optional<Object> mock) {
    assertEquals(Optional.join(Optional.some(mock)).value(), mock.value());
  }

  @Theory
  public void testStaticJoinNPE() {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    Optional.join(null);
  }

  @Theory
  public void testStaticJust() {
    Object value = new Object();
    Optional<Object> just = Optional.some(value);
    assertEquals(just.value(), value);
  }

  @Theory
  public void testStaticJustNPE() {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    Optional.some(null);
  }

  @Theory
  public void testStaticNothing() {
    Optional<Object> nothing = Optional.none();
    assertTrue(nothing.isNone());
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final Object o = new Object();

  @DataPoint
  public static final Optional<Object> just = Optional.some(o);

  @DataPoint
  public static final Optional<Object> nothing = Optional.none();

  @DataPoint
  public static final Optional<Object> justBind = just.bind(Optional::some);

  @DataPoint
  public static final Optional<Object> nothingBind = nothing.bind(Optional::some);

  @DataPoint
  public static final Optional<Object> justFilterTRUE = just.filter(Predicate.TRUE());

  @DataPoint
  public static final Optional<Object> justFilterFALSE = just.filter(Predicate.FALSE());

  @DataPoint
  public static final Optional<Object> nothingFilterTRUE = nothing.filter(Predicate.TRUE());

  @DataPoint
  public static final Optional<Object> getNothingFilterFALSE = nothing.filter(Predicate.FALSE());

  @DataPoint
  public static final Optional<Object> justMap = just.map(o -> o);

  @DataPoint
  public static final Optional<Object> nothingMap = nothing.map(o -> o);

  @DataPoint
  public static final Optional<Object> justJoin = Optional.join(Optional.some(just));

  @DataPoint
  public static final Optional<Object> nothingJoin = Optional.join(Optional.some(nothing));
}
