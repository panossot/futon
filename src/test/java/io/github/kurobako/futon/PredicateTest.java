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

import static io.github.kurobako.futon.Predicate.FALSE;
import static io.github.kurobako.futon.Predicate.TRUE;
import static io.github.kurobako.futon.Predicate.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PredicateTest {

  @Test
  public void testAnd() {
    assert$True(TRUE().and(TRUE()));
    assert$False(FALSE().and(FALSE()));
    assert$False(TRUE().and(FALSE()));
    assert$False(FALSE().and(TRUE()));
    assert$False(FALSE().and(fail));
  }

  @Test(expected = NullPointerException.class)
  public void testAndNPE() {
    //noinspection ConstantConditions
    fail.and(null);
  }

  @Test
  public void testOr() {
    assert$True(TRUE().or(FALSE()));
    assert$True(FALSE().or(TRUE()));
    assert$True(TRUE().and(TRUE()));
    assert$False(FALSE().and(FALSE()));
    assert$True(TRUE().or(fail));
  }

  @Test(expected = NullPointerException.class)
  public void testOrNPE() {
    //noinspection ConstantConditions
    fail.or(null);
  }

  @Test
  public void testXor() {
    assert$True(TRUE().xor(FALSE()));
    assert$True(FALSE().xor(TRUE()));
    assert$False(TRUE().xor(TRUE()));
    assert$False(FALSE().xor(FALSE()));
  }

  @Test(expected = NullPointerException.class)
  public void testXorNPE() {
    //noinspection ConstantConditions
    fail.xor(null);
  }

  @Test
  public void testNot() {
    assert$True(not(FALSE()));
    assert$False(not(TRUE()));
  }

  @Test(expected = NullPointerException.class)
  public void testNotNPE() {
    //noinspection ConstantConditions
    not(null);
  }


  @Test
  public void testTRUE() {
    assertTrue(TRUE().$(new Object()));
    assertTrue(TRUE().$(null));
  }

  @Test
  public void testFALSE() {
    assertFalse(FALSE().$(new Object()));
    assertFalse(FALSE().$(null));
  }

  private final Predicate<Object> fail = a -> {throw new RuntimeException();};

  private static void assert$True(final Predicate<Object> TRUE) {
    assert TRUE != null;
    assertTrue(TRUE.$(new Object()));
  }

  private static void assert$False(final Predicate<Object> FALSE) {
    assert FALSE != null;
    assertFalse(FALSE.$(new Object()));
  }
}
