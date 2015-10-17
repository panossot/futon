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

import org.junit.Test;

import static io.github.kurobako.futon.Pair.pair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PairTest {

  @Test
  public void testSwap() {
    Pair<String, String> swapped = pair("a", "b").swap();
    assertEquals(swapped.left, "b");
    assertEquals(swapped.right, "a");
  }

  @Test
  public void testHashCode() {
    Pair<String, String> first = pair("a", "b");
    Pair<String, String> second = pair("a", "b");
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void testHashCodeNulls() {
    Pair<String, String> nulls = pair(null, null);
    //noinspection ResultOfMethodCallIgnored
    nulls.hashCode();
  }

  @Test
  public void testEquals() {
    assertEquals(pair("a", "b"), pair("a", "b"));
    assertNotEquals(pair("a", "b"), pair("a", new Object()));
    assertNotEquals(pair("a", "b"), pair(new Object(), "b"));
    assertNotEquals(pair("a", "b"), new Object());
    assertNotEquals(pair("a", "b"), null);
  }

  @Test
  public void testEqualsNulls() {
    assertEquals(pair(null, null), pair(null, null));
  }
}
