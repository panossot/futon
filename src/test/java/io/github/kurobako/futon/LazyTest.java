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

import static io.github.kurobako.futon.Lazy.join;
import static io.github.kurobako.futon.Lazy.lazy;
import static io.github.kurobako.futon.Pair.pair;
import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class LazyTest {

  @Theory
  public void testBind(final Lazy<Object> mock) {
    assertEquals(mock.bind(o -> o::toString).$(), mock.$().toString());
  }

  @Theory
  public void testBindNPE(final Lazy<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.bind(null);
  }

  @Theory
  public void testZipWithFunction(final Lazy<Object> one, final Lazy<Object> another) {
    Lazy<Pair<Object, Object>> zipped = one.zip(another, (fst, snd) -> () -> pair(fst, snd));
    assertEquals(one.$(), zipped.$().left);
    assertEquals(another.$(), zipped.$().right);
  }

  @Theory
  public void testZipWithFunctionNPEArg0(final Lazy<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.zip(null, (a, b) -> lazy(a));
  }

  @Theory
  public void testZipWithFunctionNPEArg1(final Lazy<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.zip(lazy(new Object()), null);
  }

  @Theory
  public void testZip(final Lazy<Object> one, final Lazy<Object> another) {
    Lazy<Pair<Object, Object>> zipped = one.zip(another);
    assertEquals(one.$(), zipped.$().left);
    assertEquals(another.$(), zipped.$().right);
  }

  @Theory
  public void testZipNPE(final Lazy<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.zip(null);
  }

  @Theory
  public void testMap(final Lazy<Object> mock) {
    assertEquals(mock.map(Object::toString).$(), mock.$().toString());
  }

  @Theory
  public void testMapNPE(final Lazy<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.map(null);
  }

  @Theory
  public void testFoldRight(final Lazy<Object> mock) {
    assertEquals(mock.foldRight((o, s) -> o.toString() + s, ""), mock.$().toString());
  }

  @Theory
  public void testFoldRightNPE(final Lazy<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.foldRight(null, "");
  }

  @Theory
  public void testFoldLeft(final Lazy<Object> mock) {
    assertEquals(mock.foldLeft((s, o) -> o.toString() + s, ""), mock.$().toString());
  }

  @Theory
  public void testFoldLeftNPE(final Lazy<Object> mock) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    mock.foldLeft(null, "");
  }

  @Theory
  public void testStaticLazy() {
    Object o = new Object();
    assertEquals(lazy(o).$(), o);
  }

  @Theory
  public void testStaticJoin() {
    Object o = new Object();
    assertEquals(join(() -> () -> o).$(), o);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataPoint
  public static final Lazy<Object> mock = new Lazy<Object>() {
    private final Object val = new Object();

    @Override
    public Object $() {
      return val;
    }
  };

  @DataPoint
  public static final Lazy<Object> pure = lazy(new Object());
}
