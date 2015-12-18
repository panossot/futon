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
public class StoreTest {

  @Theory
  public void testPos(final Store<Character, Integer> data) {
    assertEquals(data.pos(), data.run().right);
  }

  @Theory
  public void testPeek(final Store<Character, Integer> data) {
    int index = 2;
    assertEquals(data.peek().$(index), data.run().left.$(index));
  }

  @Theory
  public void testPeeks(final Store<Character, Integer> data) {
    Function<Integer, Integer> inc = i -> i++;
    assertEquals(data.peeks(inc).charValue(), CHARS.charAt(inc.$(0)));
  }

  @Theory
  public void testPeeksNPE(final Store<Character, Integer> data) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    data.peeks(null);
  }

  @Theory
  public void testSeek(final Store<Character, Integer> data) {
    int i = 1;
    assertEquals(data.seek(i).extract().charValue(), CHARS.charAt(i));
  }

  @Theory
  public void testSeeks(final Store<Character, Integer> data) {
    Function<Integer, Integer> inc = i -> i++;
    assertEquals(data.peeks(inc).charValue(), CHARS.charAt(inc.$(0)));
  }

  @Theory
  public void testSeeksNPE(final Store<Character, Integer> data) {
    thrown.expect(NullPointerException.class);
    //noinspection ConstantConditions
    data.peeks(null);
  }

  @Theory
  public void testExtract(final Store<Character, Integer> data) {
    assertEquals(data.extract().charValue(), CHARS.charAt(0));
  }

  @Theory
  public void testStore(final Store<Character, Integer> data) {
    Store<Character, Integer> clone = Store.store(data.peek(), data.pos());
    assertEquals(clone.extract(), data.extract());
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public static final String CHARS = "The quick brown fox jumps over the lazy dog";

  @DataPoint
  public static Store<Character, Integer> charAt0() {
    return () -> pair(CHARS::charAt, 0);
  }
}
