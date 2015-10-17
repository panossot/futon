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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(Theories.class)
public class LazyTest {

  @Theory
  public void test$Void(final Lazy<Object> mock) {
    assertSame(mock.$(null), mock.$());
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
}
