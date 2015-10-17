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
import static org.junit.Assert.assertNotEquals;

@RunWith(Theories.class)
public class PairTest {

  @Theory
  public void testSwap(final Pair<String, String> mock) {
    Pair<String, String> swapped = mock.swap();
    assertEquals(swapped.left, "b");
    assertEquals(swapped.right, "a");
  }

  @Theory
  public void testHashCode(final Pair<String, String> mock) {
    assertEquals(mock.hashCode(), pair("a", "b").hashCode());
  }

  @Theory
  public void testEquals(final Pair<String, String> mock) {
    assertEquals(mock, pair("a", "b"));
    assertNotEquals(mock, new Object());
    assertNotEquals(mock, pair("a", "a"));
    assertNotEquals(mock, pair("b", "b"));
    assertNotEquals(mock, null);
  }


  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataPoint
  public static final Pair<String, String> ab = pair("a", "b");
}
