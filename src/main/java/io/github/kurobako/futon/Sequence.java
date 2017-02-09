/*
 * Copyright (C) 2017 Fedor Gavrilov
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Finite-size immutable sequence of values supporting both deque operations and random access.</p>
 * <p>Besides that sequence might be appended and prepended by both individual values and supports a set of transformations.
 * The maximum amount of elements that can be stored in a Sequence is {@value MAX_MEASURE}. Like other classes in this
 * library, null values are not supported.</p>
 * <p>{@link #map(Function)} makes Sequence a functor.</p>
 * <p>{@link #apply(Sequence)} and {@link #sequence(A)} form an applicative functor.</p>
 * <p>{@link #bind(Kleisli)} and {@link #sequence(A)} form a monad.</p>
 * <p>{@link #append(Sequence) and {@link #sequence()}} form a monoid.</p>
 * <p>This class is not supposed to be extended by users.</p>
 * @param <A> element type.
 */
@Immutable
public abstract class Sequence<A> implements Semigroup<Sequence<A>>, Foldable<A>, Iterable<A> {
  private static final int RESERVED_BITS = 2;
  private static final byte AFX_TYPE_1 = 0x0;
  private static final byte AFX_TYPE_2 = 0x1;
  private static final byte AFX_TYPE_3 = 0x2;
  private static final byte AFX_TYPE_4 = 0x3;
  private static final int MAX_MEASURE = 0xffffffff >>> RESERVED_BITS;

  // not for extension
  Sequence() {}

  /**
   * Retrieves an element by its index. <b>O(log(min(i,n-i)))</b> where <b>i</b> is the index. In other words, the worst
   * case is when the element in the middle of the sequence is retrieved.
   * @param idx non-negative index.
   * @return a sequence element. Can't be null.
   * @throws IndexOutOfBoundsException if the index less than 0 or more than this sequence's length.
   */
  public abstract @Nonnull A get(@Nonnegative int idx);

  /**
   * Retrieves the first element of this sequence. <b>O(1)</b>.
   * @return first element of the sequence. Can't be null.
   * @throws NoSuchElementException if the sequence is empty.
   */
  public abstract @Nonnull A head();

  /**
   * Returns a Sequence with its first element replaced by the given element. <b>O(1)</b>.
   * @param element new first element of the sequence. Can't be null.
   * @return new Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws NoSuchElementException if the sequence is empty.
   */
  public abstract @Nonnull Sequence<A> head(A element);

  /**
   * Returns a Sequence whose elements are all elements of this sequence except for the first one. <b>O(1)</b>.
   * @return new Sequence. Can't be null.
   * @throws NoSuchElementException if the sequence if empty.
   */
  public abstract @Nonnull Sequence<A> tail();

  /**
   * Retrieves the last element of this sequence. <b>O(1)</b>.
   * @return last element of the sequence. Can't be null.
   * @throws NoSuchElementException if the sequence is empty.
   */
  public abstract @Nonnull A last();

  /**
   * Returns a Sequence with its last element replaced by the given element. <b>O(1)</b>.
   * @param element new last element of the sequence. Can't be null.
   * @return new Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws NoSuchElementException if the sequence is empty.
   */
  public abstract @Nonnull Sequence<A> last(A element);

  /**
   * Returns a Sequence whose elements are all elements of this sequence except for the last one. <b>O(1)</b>.
   * @return new Sequence. Can't be null.
   * @throws NoSuchElementException if the sequence if empty.
   */
  public abstract @Nonnull Sequence<A> init();

  /**
   * Returns a Sequence with the given element prepended. <b>O(1)</b>.
   * @param element an element to be the first element of the new sequence. Can't be null.
   * @return new Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws IllegalStateException if max capacity is exceeded.
   */
  public abstract @Nonnull Sequence<A> prepend(A element);

  /**
   * Returns a Sequence with elements of the given sequence prepended in order. <b>O(1)</b>.
   * @param sequence a sequence to place before this sequence. Can't be null.
   * @return new Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws IllegalStateException if max capacity is exceeded.
   */
  public abstract @Nonnull Sequence<A> prepend(Sequence<A> sequence);

  /**
   * Returns a Sequence with the given element appended. <b>O(1)</b>.
   * @param element an element to be the last element of the new sequence. Can't be null.
   * @return new Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws IllegalStateException if max capacity is exceeded.
   */
  public abstract @Nonnull Sequence<A> append(A element);

  /**
   * Returns a Sequence with elements of the given sequence appended in order. <b>O(1)</b>.
   * @param sequence a sequence to place after this sequence. Can't be null.
   * @return new Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws IllegalStateException if max capacity is exceeded.
   */
  @Override
  public abstract @Nonnull Sequence<A> append(Sequence<A> sequence);

  /**
   * Returns the amout of elements in this sequence. <b>O(1)</b>.
   * @return non-negative lenght.
   */
  public abstract int length();

  /**
   * Checks if this sequence is empty. <b>O(1)</b>.
   * @return true if empty, false otherwise.
   */
  public abstract boolean isEmpty();

  /**
   * Returns a Sequence whose elements are the product of applying the given function to elements of this Sequence.
   * If this sequence is empty an empty sequence is returned.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new element type.
   * @return transformed Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Sequence<B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    return foldLeft((seq, a) -> seq.append(function.$(a)), sequence());
  }

  /**
   * Returns a Sequence whose elements are the product of applying the functions stored as the given Sequence's elements
   * to this Sequence values. Each function is applied to each value in order.
   * If this or the argument Sequence is empty an empty sequence is returned.
   * @param sequence <b>Sequence&lt;A -&gt; B&gt;</b>: a sequence of transformations. Can't be null.
   * @param <B> new element type.
   * @return transformed Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Sequence<B> apply(final Sequence<? extends Function<? super A, ? extends B>> sequence) {
    nonNull(sequence);
    return foldLeft((seq, a) -> seq.append(sequence.foldLeft((seq2, f) -> seq2.append(f.$(a)), sequence())), sequence());
  }

  /**
   * Returns a Sequence which is the product of applying the given function to this Sequence's elements. If this or the
   * Sequence returned by the argument function is empty no transformation happens.
   * @param kleisli <b>A -&gt; Sequence&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new element type.
   * @return transformed Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Sequence<B> bind(final Kleisli<? super A, B> kleisli) {
    nonNull(kleisli);
    return foldLeft((seq, a) -> seq.append(kleisli.run(a)), sequence());
  }

  /**
   * Zips this with the given Sequence using the given function. The length of the resulting sequence will be the minimum
   * of the elements in this and in the argument sequence, i.e. elements without pair will be discarded.
   * @param sequence a Sequence to zip with.
   * @param function <b>A -&gt; B -&gt; C</b> transformation. Can't be null.
   * @param <B> element type of the Sequence to zip with.
   * @param <C> new element type.
   * @return a Sequence zipped with the given Sequence. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public @Nonnull <B, C> Sequence<C> zip(final Sequence<B> sequence, final BiFunction<? super A, ? super B, ? extends C> function) {
    nonNull(sequence);
    nonNull(function);
    final int size = Math.min(this.length(), sequence.length());
    Sequence<C> result = sequence();
    for (int i = 0; i < size; i++) result = result.append(function.$(this.get(i), sequence.get(i)));
    return result;
  }

  /**
   * Unzips this into a {@link Pair} of Sequences using the given function. Both sequences have the same length equal
   * to the length of the original sequence..
   * @param function <b>A -&gt; (B, C)</b> transformation. Can't be null.
   * @param <B> element type of the first result.
   * @param <C> element type of the second result.
   * @return a Pair of unzipped Sequences.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B, C> Pair<Sequence<B>, Sequence<C>> unzip(final Function<? super A, Pair<B, C>> function) {
    nonNull(function);
    Sequence<B> bs = sequence();
    Sequence<C> cs = sequence();
    for (int i = 0; i < length(); i++) {
      final Pair<B, C> bc = function.$(get(i));
      bs = bs.append(bc.first);
      cs = cs.append(bc.second);
    }
    return pair(bs, cs);
  }

  /**
   * Filters this Sequence: if the predicate holds, the Sequence returned by this method will contain the tested element,
   * if it does not, it will not. If this Sequence was empty, predicate is not applied and empty sequence is returned.
   * @param predicate filtering function. Can't be null.
   * @return filtered Sequence. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull Sequence<A> filter(final Predicate<? super A> predicate) {
    nonNull(predicate);
    return foldLeft((seq, a) -> predicate.$(a) ? seq.append(a) : sequence(), sequence());
  }

  /**
   * Returns a new Sequence of the same elements as this one but their order reversed. <b>O(n)</b>.
   * @return reversed Sequence. Can't be null.
   */
  public @Nonnull Sequence<A> reverse() {
    return foldRight((a, seq) -> seq.append(a), sequence());
  }

  @Override
  public abstract @Nonnull <B> B foldRight(BiFunction<? super A, ? super B, ? extends B> function, B initial);

  @Override
  public abstract @Nonnull <B> B foldLeft(BiFunction<? super B, ? super A, ? extends B> function, B initial);

  /**
   * Transforms elements of this Sequence using the given function, wrapping returned {@link Sequence} of values into
   * another sequence. If this Sequence is empty, a sequence of size 1 containing this sole element is returned.
   * @param function traversal function: <b>A -&gt; [B]</b>. Can't be null.
   * @param <B> new element type.
   * @return a sequence of sequences returned by the function. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Sequence<Sequence<B>> traverse(final Function<? super A, Sequence<B>> function) {
    nonNull(function);
    return foldLeft((ss, a) -> ss.append(function.$(a)), sequence());
  }

  /**
   * Reduces this sequence using the given binary function and initial value similar to how {@link #foldRight(BiFunction, B)}
   * does, but instead returns a sequence of reduction steps rather than the final value.
   * @param function <b>A -&gt; B -&gt; B</b>: function accepting accumulated result and a new value, producing new accumulated value.
   * @param initial initial accumulator.
   * @param <B> accumulator type.
   * @par
   * @return a sequence of accumulated results. Can' be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Sequence<B> scanRight(final BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
    nonNull(function);
    return foldRight((a, bs)-> bs.append(function.$(a, bs.last())), sequence(initial));
  }

  /**
   * Reduces this sequence using the given binary function and initial value similar to how {@link #foldLeft(BiFunction, B)}
   * does, but instead returns a sequence of reduction steps rather than the final value.
   * @param function <b>B -&gt; A -&gt; B</b>: function accepting accumulated result and a new value, producing new accumulated value.
   * @param initial initial accumulator.
   * @param <B> accumulator type.
   * @return a sequence of accumulated results. Can' be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Sequence<B> scanLeft(final BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
    nonNull(function);
    return foldLeft((bs, a)-> bs.append(function.$(bs.last(), a)), sequence(initial));
  }

  @Override
  public @Nonnull Iterator<A> iterator() {
    return new Iterator<A>() {
      private int nextIdx;

      @Override
      public boolean hasNext() {
        return nextIdx < length();
      }

      @Override
      public A next() {
        if (!hasNext()) throw new NoSuchElementException();
        return get(nextIdx++);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Sequence)) return false;
    final Sequence<?> that = (Sequence<?>) o;
    if (this.length() != that.length()) return false;
    for (int i = 0; i < length(); i++) {
      if (!this.get(i).equals(that.get(i))) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return foldLeft((total, a) -> 31*total + a.hashCode(), 1);
  }

  abstract @Nonnull Sequence<A> append0(Simple<A> right);

  abstract @Nonnull Sequence<A> append0(Deep<A> right);

  abstract @Nonnull Sequence<A> prepend0(Simple<A> left);

  abstract @Nonnull Sequence<A> prepend0(Deep<A> left);

  static void checkCapacity(int current, int increment) {
    if (current + increment > MAX_MEASURE) throw new IllegalStateException("Max capacity exceeded.");
  }

  @SuppressWarnings("unchecked")
  static @Nonnull <A> Sequence<A> concat(final Deep<A> left, final Deep<A> right) {
    final A[] buffer = (A[]) new Object[8];
    int i = 0;
    switch (left.sfxType) {
      case AFX_TYPE_1: {
        buffer[i++] = left.sfx0;
        break;
      }
      case AFX_TYPE_2: {
        buffer[i++] = left.sfx1;
        buffer[i++] = left.sfx0;
        break;
      }
      case AFX_TYPE_3: {
        buffer[i++] = left.sfx2;
        buffer[i++] = left.sfx1;
        buffer[i++] = left.sfx0;
        break;
      }
      case AFX_TYPE_4: {
        buffer[i++] = left.sfx3;
        buffer[i++] = left.sfx2;
        buffer[i++] = left.sfx1;
        buffer[i++] = left.sfx0;
        break;
      }
      default:
        assert false;
    }
    switch (right.pfxType) {
      case AFX_TYPE_1: {
        buffer[i++] = right.pfx0;
        break;
      }
      case AFX_TYPE_2: {
        buffer[i++] = right.pfx0;
        buffer[i++] = right.pfx1;
        break;
      }
      case AFX_TYPE_3: {
        buffer[i++] = right.pfx0;
        buffer[i++] = right.pfx1;
        buffer[i++] = right.pfx2;
        break;
      }
      case AFX_TYPE_4: {
        buffer[i++] = right.pfx0;
        buffer[i++] = right.pfx1;
        buffer[i++] = right.pfx2;
        buffer[i++] = right.pfx3;
        break;
      }
      default:
        assert false;
    }
    final Node<A>[] nodes = new Node[i/2];
    int subMeasure = left.subMeasure + right.subMeasure;
    if (i%2 != 0) {
      nodes[i/2-1] = new Node.Three(buffer[i-3], 1, buffer[i-2], 1, buffer[i-1], 1);
      subMeasure += 3;
    } else {
      nodes[i/2-1] = new Node.Two(buffer[i-2], 1, buffer[i-1], 1);
      subMeasure += 2;
    }
    for (int j = 0; j < i/2-1; j++) {
      nodes[j] = new Node.Two(buffer[j*2], 1, buffer[j*2+1], 1);
      subMeasure += 2;
    }
    final Nodes<A> newSub = left.sub.append0(nodes, right.sub);
    return new Deep<>(left.pfxType, left.pfx0, left.pfx1, left.pfx2, left.pfx3, subMeasure, newSub, right.sfxType, right.sfx3, right.sfx2, right.sfx1, right.sfx0);
  }

  @SuppressWarnings("unchecked")
  public static @Nonnull <A> Sequence<A> sequence() {
    return (Sequence<A>) Simple.EMPTY;
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0) {
    return new Simple<>(nonNull(e0));
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0, final A e1) {
    return new Deep<>(nonNull(e0), nonNull(e1));
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0, final A e1, final A e2) {
    return new Deep<>(nonNull(e0), nonNull(e1), nonNull(e2));
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0, final A e1, final A e2, final A e3) {
    return new Deep<>(nonNull(e0), nonNull(e1), nonNull(e2), nonNull(e3));
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0, final A e1, final A e2, final A e3, final A e4) {
    return new Deep<>(nonNull(e0), nonNull(e1), nonNull(e2), nonNull(e3), nonNull(e4));
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0, final A e1, final A e2, final A e3, final A e4, final A e5) {
    return new Deep<>(nonNull(e0), nonNull(e1), nonNull(e2), nonNull(e3), nonNull(e4), nonNull(e5));
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0, final A e1, final A e2, final A e3, final A e4, final A e5, final A e6) {
    return new Deep<>(nonNull(e0), nonNull(e1), nonNull(e2), nonNull(e3), nonNull(e4), nonNull(e5), nonNull(e6));
  }

  public static @Nonnull <A> Sequence<A> sequence(final A e0, final A e1, final A e2, final A e3, final A e4, final A e5, final A e6, final A e7) {
    return new Deep<>(nonNull(e0), nonNull(e1), nonNull(e2), nonNull(e3), nonNull(e4), nonNull(e5), nonNull(e6), nonNull(e7));
  }

  public static @Nonnull <A, B> Sequence<A> unfoldRight(final Function<B, Option<Pair<? extends A, B>>> function, B seed) {
    Sequence<A> result = sequence();
    Option<Pair<? extends A, B>> option = function.$(seed);
    while (option.isSome()) {
      final Pair<? extends A, B> p = option.asNullable();
      result = result.append(p.first);
      seed = p.second;
      option = function.$(seed);
    }
    return sequence();
  }

  public static @Nonnull <A, B> Sequence<A> unfoldLeft(final Function<B, Option<Pair<B, ? extends A>>> function, B seed) {
    Sequence<A> result = sequence();
    Option<Pair<B, ? extends A>> option = function.$(seed);
    while (option.isSome()) {
      final Pair<B, ? extends A> p = option.asNullable();
      result = result.prepend(p.second);
      seed = p.first;
      option = function.$(seed);
    }
    return sequence();
  }

  public static @Nonnull <A> Sequence<A> flatten(final @Nonnull Sequence<Sequence<A>> sequences) {
    return nonNull(sequences).foldLeft(Sequence::append, sequence());
  }

  private static final class Deep<A> extends Sequence<A> {
    final byte pfxType;
    final byte sfxType;
    final @Nonnull A pfx0;
    final @Nullable A pfx1;
    final @Nullable A pfx2;
    final @Nullable A pfx3;
    final int subMeasure;
    final @Nonnull Nodes<A> sub;
    final @Nullable A sfx3;
    final @Nullable A sfx2;
    final @Nullable A sfx1;
    final @Nonnull A sfx0;

    Deep(final byte pfxType, final A pfx0, final @Nullable A pfx1, final @Nullable A pfx2, final @Nullable A pfx3, final int subMeasure, final Nodes<A> sub, final byte sfxType, final @Nullable A sfx3, final @Nullable A sfx2, final @Nullable A sfx1, final A sfx0) {
      this.pfxType = pfxType;
      this.pfx0 = pfx0;
      this.pfx1 = pfx1;
      this.pfx2 = pfx2;
      this.pfx3 = pfx3;
      this.subMeasure = subMeasure;
      this.sub = sub;
      this.sfxType = sfxType;
      this.sfx3 = sfx3;
      this.sfx2 = sfx2;
      this.sfx1 = sfx1;
      this.sfx0 = sfx0;
    }

    Deep(final A pfx0, final A sfx0) {
      this(AFX_TYPE_1, pfx0, null, null, null, 0, Nodes.SimpleNodes.empty(), AFX_TYPE_1, null, null, null, sfx0);
    }

    Deep(final A pfx0, final A pfx1, final A sfx0) {
      this(AFX_TYPE_2, pfx0, pfx1, null, null, 0, Nodes.SimpleNodes.empty(), AFX_TYPE_1, null, null, null, sfx0);
    }

    Deep(final A pfx0, final A pfx1, final A sfx1, final A sfx0) {
      this(AFX_TYPE_2, pfx0, pfx1, null, null, 0, Nodes.SimpleNodes.empty(), AFX_TYPE_2, null, null, sfx1, sfx0);
    }

    Deep(final A pfx0, final A pfx1, final A pfx2, final A sfx1, final A sfx0) {
      this(AFX_TYPE_3, pfx0, pfx1, pfx2, null, 0, Nodes.SimpleNodes.empty(), AFX_TYPE_2, null, null, sfx1, sfx0);
    }

    Deep(final A pfx0, final A pfx1, final A pfx2, final A sfx2, final A sfx1, final A sfx0) {
      this(AFX_TYPE_3, pfx0, pfx1, pfx2, null, 0, Nodes.SimpleNodes.empty(), AFX_TYPE_3, null, sfx2, sfx1, sfx0);
    }

    Deep(final A pfx0, final A pfx1, final A pfx2, final A pfx3, final A sfx2, final A sfx1, final A sfx0) {
      this(AFX_TYPE_4, pfx0, pfx1, pfx2, pfx3, 0, Nodes.SimpleNodes.empty(), AFX_TYPE_3, null, sfx2, sfx1, sfx0);
    }

    Deep(final A pfx0, final A pfx1, final A pfx2, final A pfx3, final A sfx3, final A sfx2, final A sfx1, final A sfx0) {
      this(AFX_TYPE_4, pfx0, pfx1, pfx2, pfx3, 0, Nodes.SimpleNodes.empty(), AFX_TYPE_4, sfx3, sfx2, sfx1, sfx0);
    }

    int pfxMeasure() {
      return pfxType + 1;
    }

    int sfxMeasure() {
      return sfxType + 1;
    }

    @Override
    public @Nonnull A get(final int idx) {
      final A result = get0(idx);
      if (result == null) throw new IndexOutOfBoundsException();
      return result;
    }

    public A get0(int idx) {
      if (idx < 0) return null;
      if (idx < pfxMeasure()) {
        switch (idx) {
          case 0: return pfx0;
          case 1: return pfx1;
          case 2: return pfx2;
          case 3: return pfx3;
          default: assert false;
        }
      }
      if (idx < pfxMeasure() + subMeasure) {
        idx = idx - pfxMeasure();
        return sub.get(idx);
      }
      if (idx < pfxMeasure() + subMeasure + sfxMeasure()) {
        idx = idx - pfxMeasure() - subMeasure;
        switch (sfxType) {
          case AFX_TYPE_1: return sfx0;
          case AFX_TYPE_2: switch (idx) {
            case 0: return sfx1;
            case 1: return sfx0;
            default: assert false;
          }
          case AFX_TYPE_3: switch (idx) {
            case 0: return sfx2;
            case 1: return sfx1;
            case 2: return sfx0;
            default: assert false;
          }
          case AFX_TYPE_4: switch (idx) {
            case 0: return sfx3;
            case 1: return sfx2;
            case 2: return sfx1;
            case 3: return sfx0;
            default: assert false;
          }
          default: assert false;
        }
      }
      return null;
    }

    @Override
    public @Nonnull A head() {
      return pfx0;
    }

    public @Nonnull Sequence<A> head(final A element) {
      return new Deep<>(pfxType, nonNull(element), pfx1, pfx2, pfx3, subMeasure, sub, sfxType, sfx3, sfx2, sfx1, sfx0);
    }

    public @Nonnull Sequence<A> tail() {
      if (pfxType != AFX_TYPE_1) {
        return new Deep<>((byte)(pfxType - 1), pfx1, pfx2, pfx3, null, subMeasure, sub, sfxType, sfx3, sfx2, sfx1, sfx0);
      }
      if (!sub.isEmpty()) {
        final Node<A> node = sub.headNode();
        switch (node.type()) {
          case Node.NODE_TYPE_2: return new Deep<>(AFX_TYPE_2, node.at(0), node.at(1), null, null, subMeasure - 2, sub.tailNodes(), sfxType, sfx3, sfx2, sfx1, sfx0);
          case Node.NODE_TYPE_3: return new Deep<>(AFX_TYPE_3, node.at(0), node.at(1), node.at(2), null, subMeasure - 3, sub.tailNodes(), sfxType, sfx3, sfx2, sfx1, sfx0);
          default: assert false;
        }
      }
      switch (sfxType) {
        case AFX_TYPE_1: return new Simple<>(sfx0);
        case AFX_TYPE_2: return new Deep<>(sfx1, sfx0);
        case AFX_TYPE_3: return new Deep<>(sfx2, sfx1, sfx0);
        case AFX_TYPE_4: return new Deep<>(sfx3, sfx2, sfx1, sfx0);
        default: assert false;
      }
      assert false;
      return null;
    }

    @Override
    public @Nonnull A last() {
      return sfx0;
    }

    public @Nonnull Sequence<A> last(final A element) {
      return new Deep<>(pfxType, pfx0, pfx1, pfx2, pfx3, subMeasure, sub, sfxType, sfx3, sfx2, sfx1, element);
    }

    public @Nonnull Sequence<A> init() {
      if (sfxType != AFX_TYPE_1) {
        return new Deep<>(pfxType, pfx0, pfx1, pfx2, pfx3, subMeasure, sub, (byte) (sfxType - 1), null, sfx3, sfx2, sfx1);
      }
      if (!sub.isEmpty()) {
        final Node<A> node = sub.lastNode();
        switch (node.type()) {
          case Node.NODE_TYPE_2: return new Deep<>(pfxType, pfx0, pfx1, pfx2, pfx3, subMeasure - 2, sub.initNodes(), AFX_TYPE_2, null, null, node.at(1), node.at(0));
          case Node.NODE_TYPE_3: return new Deep<>(pfxType, pfx0, pfx1, pfx2, pfx3, subMeasure - 3, sub.initNodes(), AFX_TYPE_3, null, node.at(2), node.at(1), node.at(0));
          default: assert false;
        }
      }
      switch (pfxType) {
        case AFX_TYPE_1: return new Simple<>(pfx0);
        case AFX_TYPE_2: return new Deep<>(pfx0, pfx1);
        case AFX_TYPE_3: return new Deep<>(pfx0, pfx1, pfx2);
        case AFX_TYPE_4: return new Deep<>(pfx0, pfx1, pfx2, pfx3);
        default: assert false;

      }
      assert false;
      return null;
    }

    @Override
    public @Nonnull Sequence<A> prepend(final A element) {
      nonNull(element);
      checkCapacity(length(), 1);
      if (pfxType != AFX_TYPE_4) return new Deep<>((byte)(pfxType + 1), element, pfx0, pfx1, pfx2, subMeasure, sub, sfxType, sfx3, sfx2, sfx1, sfx0);
      final Nodes<A> newSub = sub.prepend(new Node.Three<>(pfx1, 1, pfx2, 1, pfx3, 1));
      return new Deep<>(AFX_TYPE_2, element, pfx0, null, null, subMeasure + 3, newSub, sfxType, sfx3, sfx2, sfx1, sfx0);
    }

    @Override
    public @Nonnull Sequence<A> append(final A element) {
      nonNull(element);
      checkCapacity(length(), 1);
      if (sfxType != AFX_TYPE_4) return new Deep<>(pfxType, pfx0, pfx1, pfx2, pfx3, subMeasure, sub, (byte)(sfxType + 1), sfx2, sfx1, sfx0, element);
      final Nodes<A> newSub = sub.append(new Node.Three<>(sfx3, 1, sfx2, 1, sfx1, 1));
      return new Deep<>(pfxType, pfx0, pfx1, pfx2, pfx3, subMeasure + 3, newSub, AFX_TYPE_2, null, null, sfx0, element);
    }

    @Override
    public int length() {
      return pfxMeasure() + subMeasure + sfxMeasure();
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public @Nonnull Sequence<A> prepend(final Sequence<A> sequence) {
      checkCapacity(this.length(), sequence.length());
      return sequence.append0(this);
    }

    @Override
    public @Nonnull Sequence<A> append(final Sequence<A> sequence) {
      checkCapacity(this.length(), sequence.length());
      return sequence.prepend0(this);
    }

    @Override
    public @Nonnull <B> B foldRight(final BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
      nonNull(function);
      B result = nonNull(initial);
      switch (sfxType) {
        case AFX_TYPE_1: {
          result = function.$(sfx0, result);
          break;
        }
        case AFX_TYPE_2: {
          result = function.$(sfx0, result);
          result = function.$(sfx1, result);
          break;
        }
        case AFX_TYPE_3: {
          result = function.$(sfx0, result);
          result = function.$(sfx1, result);
          result = function.$(sfx2, result);
          break;
        }
        case AFX_TYPE_4: {
          result = function.$(sfx0, result);
          result = function.$(sfx1, result);
          result = function.$(sfx2, result);
          result = function.$(sfx3, result);
          break;
        }
        default: assert false;
      }
      result = sub.foldRight(function, result);
      switch (pfxType) {
        case AFX_TYPE_4: {
          result = function.$(pfx3, result);
          result = function.$(pfx2, result);
          result = function.$(pfx1, result);
          result = function.$(pfx0, result);
          break;
        }
        case AFX_TYPE_3: {
          result = function.$(pfx2, result);
          result = function.$(pfx1, result);
          result = function.$(pfx0, result);
          break;
        }
        case AFX_TYPE_2: {
          result = function.$(pfx1, result);
          result = function.$(pfx0, result);
          break;
        }
        case AFX_TYPE_1: {
          result = function.$(pfx0, result);
          break;
        }
        default: assert false;
      }
      return result;
    }

    @Override
    public @Nonnull <B> B foldLeft(final BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
      nonNull(function);
      B result = nonNull(initial);
      switch (pfxType) {
        case AFX_TYPE_1: {
          result = function.$(result, pfx0);
          break;
        }
        case AFX_TYPE_2: {
          result = function.$(result, pfx0);
          result = function.$(result, pfx1);
          break;
        }
        case AFX_TYPE_3: {
          result = function.$(result, pfx0);
          result = function.$(result, pfx1);
          result = function.$(result, pfx2);
          break;
        }
        case AFX_TYPE_4: {
          result = function.$(result, pfx0);
          result = function.$(result, pfx1);
          result = function.$(result, pfx2);
          result = function.$(result, pfx3);
          break;
        }
        default: assert false;
      }
      result = sub.foldLeft(function, result);
      switch (sfxType) {
        case AFX_TYPE_4: {
          result = function.$(result, sfx3);
          result = function.$(result, sfx2);
          result = function.$(result, sfx1);
          result = function.$(result, sfx0);
          break;
        }
        case AFX_TYPE_3: {
          result = function.$(result, sfx2);
          result = function.$(result, sfx1);
          result = function.$(result, sfx0);
          break;
        }
        case AFX_TYPE_2: {
          result = function.$(result, sfx1);
          result = function.$(result, sfx0);
          break;
        }
        case AFX_TYPE_1: {
          result = function.$(result, sfx0);
          break;
        }
        default: assert false;
      }
      return result;
    }

    @Override
    @Nonnull Sequence<A> append0(final Simple<A> right) {
      final A rightValue = right.value;
      return rightValue != null ? append(rightValue) : this;
    }

    @Override
    @Nonnull Sequence<A> append0(final Deep<A> right) {
      return concat(this, right);
    }

    @Override
    @Nonnull Sequence<A> prepend0(final Simple<A> left) {
      final A leftValue = left.value;
      return leftValue != null ? prepend(leftValue) : this;
    }

    @Override
    @Nonnull Sequence<A> prepend0(final Deep<A> left) {
      return concat(left, this);
    }
  }

  private static final class Simple<A> extends Sequence<A> {
    static final @Nonnull Simple<Object> EMPTY = new Simple<>();

    final A value;

    Simple(final A value) {
      this.value = value;
    }

    Simple() {
      this.value = null;
    }

    @Override
    public @Nonnull A get(final int idx) {
      if (idx != 0 || value == null) throw new IndexOutOfBoundsException();
      return value;
    }

    @Override
    public @Nonnull A head() {
      if (value == null) throw new NoSuchElementException();
      return value;
    }

    @Override
    public @Nonnull A last() {
      if (value == null) throw new NoSuchElementException();
      return value;
    }

    @Override
    public @Nonnull Sequence<A> prepend(final A element) {
      nonNull(element);
      return value == null ? sequence(element) : sequence(element, value);
    }

    @Override
    public @Nonnull Sequence<A> append(final A element) {
      nonNull(element);
      return value == null ? sequence(element) : sequence(value, element);
    }

    @Override
    public @Nonnull Sequence<A> head(A element) {
      nonNull(element);
      if (value == null) throw new NoSuchElementException();
      return sequence(element);
    }

    @Override
    public @Nonnull Sequence<A> last(A element) {
      nonNull(element);
      if (value == null) throw new NoSuchElementException();
      return sequence(element);
    }

    @Override
    public @Nonnull Sequence<A> tail() {
      if (value == null) throw new NoSuchElementException();
      return sequence();
    }

    @Override
    public @Nonnull Sequence<A> init() {
      if (value == null) throw new NoSuchElementException();
      return sequence();
    }

    @Override
    public int length() {
      return value == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
      return value == null;
    }

    @Override
    public @Nonnull Sequence<A> prepend(final Sequence<A> sequence) {
      checkCapacity(this.length(), sequence.length());
      return sequence.append0(this);
    }

    @Override
    public @Nonnull Sequence<A> append(final Sequence<A> sequence) {
      checkCapacity(this.length(), sequence.length());
      return sequence.prepend0(this);
    }

    @Override
    public @Nonnull <B> B foldRight(final BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
      nonNull(function);
      nonNull(initial);
      return value == null ? initial : function.$(value, initial);
    }

    @Override
    public @Nonnull <B> B foldLeft(final BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
      nonNull(function);
      nonNull(initial);
      return value == null ? initial : function.$(initial, value);
    }

    @Override
    @Nonnull Sequence<A> append0(final Simple<A> right) {
      final A rightValue = right.value;
      return rightValue != null ? append(rightValue) : this;
    }

    @Override
    @Nonnull Sequence<A> append0(final Deep<A> right) {
      return value != null ? right.prepend(value) : right;
    }

    @Override
    @Nonnull Sequence<A> prepend0(final Simple<A> left) {
      final A leftValue = left.value;
      return leftValue != null ? prepend(leftValue) : this;
    }

    @Override
    @Nonnull Sequence<A> prepend0(final Deep<A> left) {
      return value != null ? left.append(value) : left;
    }
  }

  private static abstract class Node<A> {
    static final int NODE_TYPE_2 = 2;
    static final int NODE_TYPE_3 = 3;

    abstract int measure();

    abstract @Nonnull A get(int measure);

    abstract @Nonnull A get(int measure, IntHolder offset);

    abstract @Nonnull A at(int idx);

    abstract int type();

    abstract @Nonnull <B> B foldRight(BiFunction<? super A, ? super B, ? extends B> function, B initial);

    abstract @Nonnull <B> B foldLeft(BiFunction<? super B, ? super A, ? extends B> function, B initial);

    private static final class Two<A> extends Node<A> {
      final @Nonnull A first;
      final int firstMeasure;
      final @Nonnull A second;
      final int secondMeasure;

      Two(final A first, final int firstMeasure, final A second, final int secondMeasure) {
        this.first = first;
        this.firstMeasure = firstMeasure;
        this.second = second;
        this.secondMeasure = secondMeasure;
      }

      @Override
      int measure() {
        return firstMeasure + secondMeasure;
      }

      @Override
      @Nonnull A get(final int measure) {
        if (measure < firstMeasure) return first;
        if (measure < firstMeasure + secondMeasure) return second;
        assert false;
        return null;
      }

      @Override
      @Nonnull A get(final int measure, final IntHolder offset) {
        if (measure < firstMeasure) return first;
        if (measure < firstMeasure + secondMeasure) {
          offset.value += firstMeasure;
          return second;
        }
        assert false;
        return null;
      }

      @Override
      @Nonnull A at(int idx) {
        assert idx >= 0;
        switch (idx) {
          case 0: return first;
          case 1: return second;
          default: {
            assert false;
            return null;
          }
        }
      }

      @Override int type() {
        return NODE_TYPE_2;
      }


      @Override
      @Nonnull <B> B foldRight(final BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
        B result = initial;
        result = function.$(second, result);
        result = function.$(first, result);
        return result;
      }

      @Override
      @Nonnull <B> B foldLeft(final BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
        B result = initial;
        result = function.$(result, first);
        result = function.$(result, second);
        return result;
      }
    }

    private static final class Three<A> extends Node<A> {
      final @Nonnull A first;
      final int firstMeasure;
      final @Nonnull A second;
      final int secondMeasure;
      final @Nonnull A third;
      final int thirdMeasure;

      Three(final A first, final int firstMeasure, final A second, final int secondMeasure, final A third, final int thirdMeasure) {
        this.first = first;
        this.firstMeasure = firstMeasure;
        this.second = second;
        this.secondMeasure = secondMeasure;
        this.third = third;
        this.thirdMeasure = thirdMeasure;
      }

      @Override
      int measure() {
        return firstMeasure + secondMeasure + thirdMeasure;
      }

      @Override
      @Nonnull A get(final int measure) {
        if (measure < firstMeasure) return first;
        if (measure < firstMeasure + secondMeasure) return second;
        if (measure < firstMeasure + secondMeasure + thirdMeasure) return third;
        assert false;
        return null;
      }

      @Override
      @Nonnull A get(final int measure, IntHolder offset) {
        if (measure < firstMeasure) return first;
        if (measure < firstMeasure + secondMeasure) {
          offset.value += firstMeasure;
          return second;
        }
        if (measure < firstMeasure + secondMeasure + thirdMeasure) {
          offset.value += firstMeasure + secondMeasure;
          return third;
        }
        assert false;
        return null;
      }

      @Override
      @Nonnull A at(int idx) {
        assert idx >= 0;
        switch (idx) {
          case 0: return first;
          case 1: return second;
          case 2: return third;
          default: {
            assert false;
            return null;
          }
        }
      }

      @Override int type() {
        return NODE_TYPE_3;
      }

      @Override
      @Nonnull <B> B foldRight(final BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
        B result = initial;
        result = function.$(third, result);
        result = function.$(second, result);
        result = function.$(first, result);
        return result;
      }

      @Override
      @Nonnull <B> B foldLeft(final BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
        B result = initial;
        result = function.$(result, first);
        result = function.$(result, second);
        result = function.$(result, third);
        return result;
      }
    }
  }

  private static final class IntHolder {
    int value;

    private IntHolder() {
      this.value = 0;
    }
  }

  private static abstract class Nodes<A> {
    abstract @Nonnull A get(int idx);

    abstract @Nonnull A get(int idx, IntHolder offsetHolder);

    abstract @Nonnull Nodes<A> prepend(Node<A> node);

    abstract @Nonnull Nodes<A> append(Node<A> node);

    abstract @Nonnull Node<A> headNode();

    abstract @Nonnull Nodes<A> tailNodes();

    abstract @Nonnull Node<A> lastNode();

    abstract @Nonnull Nodes<A> initNodes();

    abstract boolean isEmpty();

    abstract @Nonnull <B> B foldRight(BiFunction<? super A, ? super B, ? extends B> function, B initial);

    abstract @Nonnull <B> B foldLeft(BiFunction<? super B, ? super A, ? extends B> function, B initial);

    abstract @Nonnull Nodes<A> append0(Node<A>[] mid, Nodes<A> right);

    abstract @Nonnull Nodes<A> prepend0(SimpleNodes<A> left, Node<A>[] mid);

    abstract @Nonnull Nodes<A> prepend0(DeepNodes<A> left, Node<A>[] mid);

    private static @Nonnull <A> Nodes<A> concat(final SimpleNodes<A> left, final Node<A>[] mid, final SimpleNodes<A> right) {
      assert mid.length <= 8;
      Nodes<A> result = left;
      for (Node<A> a : mid) result = result.append(a);
      final Node<A> rightValue = right.value;
      if (rightValue != null) result = result.append(rightValue);
      return result;
    }

    private static @Nonnull <A> Nodes<A> concat(final SimpleNodes<A> left, final Node<A>[] mid, final DeepNodes<A> right) {
      assert mid.length <= 8;
      Nodes<A> result = right;
      for (int i = mid.length - 1; i >= 0; i--) result = result.prepend(mid[i]);
      final Node<A> leftValue = left.value;
      if (leftValue != null) result.prepend(leftValue);
      return result;
    }

    private static @Nonnull <A> Nodes<A> concat(final DeepNodes<A> left, final Node<A>[] mid, final SimpleNodes<A> right) {
      assert mid.length <= 8;
      Nodes<A> result = left;
      for (Node<A> a : mid) result = result.append(a);
      final Node<A> rightValue = right.value;
      if (rightValue != null) result = result.append(rightValue);
      return result;
    }

    @SuppressWarnings("unchecked")
    private static @Nonnull <A> Nodes<A> concat(final DeepNodes<A> left, final Node<A>[] mid, final DeepNodes<A> right) {
      assert mid.length <= 8;
      int subMeasure = left.subMeasure + left.sfxMeasure() + right.pfxMeasure() + right.subMeasure;
      for (Node<A> node : mid) subMeasure += node.measure();
      final Nodes<Node<A>> leftSub = left.sub();
      final Nodes<Node<A>> rightSub = right.sub();
      final Value<Nodes<Node<A>>> sub = () -> {
        final Node<A>[] buffer = (Node<A>[]) new Node[mid.length + 8];
        int i = 0;
        switch (left.sfxType()) {
          case AFX_TYPE_1: {
            buffer[i++] = left.sfx0;
            break;
          }
          case AFX_TYPE_2: {
            buffer[i++] = left.sfx1;
            buffer[i++] = left.sfx0;
            break;
          }
          case AFX_TYPE_3: {
            buffer[i++] = left.sfx2;
            buffer[i++] = left.sfx1;
            buffer[i++] = left.sfx0;
            break;
          }
          case AFX_TYPE_4: {
            buffer[i++] = left.sfx3;
            buffer[i++] = left.sfx2;
            buffer[i++] = left.sfx1;
            buffer[i++] = left.sfx0;
            break;
          }
          default: assert false;
        }
        for (Node<A> a : mid) {
          buffer[i++] = a;
        }
        switch (right.pfxType()) {
          case AFX_TYPE_1: {
            buffer[i++] = right.pfx0;
            break;
          }
          case AFX_TYPE_2: {
            buffer[i++] = right.pfx0;
            buffer[i++] = right.pfx1;
            break;
          }
          case AFX_TYPE_3: {
            buffer[i++] = right.pfx0;
            buffer[i++] = right.pfx1;
            buffer[i++] = right.pfx2;
            break;
          }
          case AFX_TYPE_4: {
            buffer[i++] = right.pfx0;
            buffer[i++] = right.pfx1;
            buffer[i++] = right.pfx2;
            buffer[i++] = right.pfx3;
            break;
          }
          default: assert false;
        }
        final int nodesLen = i/2;
        final Node<Node<A>>[] nodes = new Node[nodesLen];
        if (i % 2 != 0) {
          nodes[nodesLen-1] = new Node.Three(buffer[i-3], buffer[i-3].measure(), buffer[i-2], buffer[i-2].measure(), buffer[i-1], buffer[i-1].measure());
        } else {
          nodes[nodesLen-1] = new Node.Two(buffer[i-2], buffer[i-2].measure(), buffer[i-1], buffer[i-1].measure());
        }
        for (int j = 0; j < nodesLen - 1; j++) {
          nodes[j] = new Node.Two(buffer[j*2], buffer[j*2].measure(), buffer[j*2+1], buffer[j*2+1].measure());
        }
        return leftSub.append0(nodes, rightSub);
      };
      return new DeepNodes<>(left.pfxInfo, left.pfx0, left.pfx1, left.pfx2, left.pfx3, subMeasure, sub, null, right.sfxInfo, right.sfx3, right.sfx2, right.sfx1, right.sfx0);
    }

    private static final class DeepNodes<A> extends Nodes<A> {
      private final int pfxInfo;
      private final @Nonnull Node<A> pfx0;
      private final Node<A> pfx1;
      private final Node<A> pfx2;
      private final Node<A> pfx3;
      private final int subMeasure;
      private volatile Value<Nodes<Node<A>>> subComputation;
      private Nodes<Node<A>> subResult;
      private final int sfxInfo;
      private final Node<A> sfx3;
      private final Node<A> sfx2;
      private final Node<A> sfx1;
      private final @Nonnull Node<A> sfx0;

      DeepNodes(final int pfxInfo, final Node<A> pfx0, final @Nullable Node<A> pfx1, final @Nullable Node<A> pfx2, final @Nullable Node<A> pfx3,
                final int subMeasure, final @Nullable Value<Nodes<Node<A>>> subComputation, final @Nullable Nodes<Node<A>> subResult,
                final int sfxInfo, final @Nullable Node<A> sfx3, final @Nullable Node<A> sfx2, final @Nullable Node<A> sfx1, final Node<A> sfx0) {
        this.pfxInfo = pfxInfo;
        this.pfx0 = pfx0;
        this.pfx1 = pfx1;
        this.pfx2 = pfx2;
        this.pfx3 = pfx3;
        this.subMeasure = subMeasure;
        this.subComputation = subComputation;
        this.subResult = subResult;
        this.sfxInfo = sfxInfo;
        this.sfx3 = sfx3;
        this.sfx2 = sfx2;
        this.sfx1 = sfx1;
        this.sfx0 = sfx0;
      }

      private @Nonnull Nodes<Node<A>> sub() {
        boolean calculated = (subComputation == null);
        if (!calculated) {
          synchronized (this) {
            calculated = (subComputation == null);
            if (!calculated) {
              subResult = subComputation.get();
              subComputation = null;
            }
          }
        }
        return subResult;
      }

      DeepNodes(final Node<A> e0, final Node<A> e1) {
        this(packAfxInfo(e0.measure(), AFX_TYPE_1), e0, null, null, null, 0, null, SimpleNodes.empty(), packAfxInfo(e1.measure(), AFX_TYPE_1), null, null, null, e1);
      }

      DeepNodes(final Node<A> e0, final Node<A> e1, final Node<A> e2) {
        this(packAfxInfo(e0.measure() + e1.measure(), AFX_TYPE_2), e0, e1, null, null, 0, null, SimpleNodes.empty(), packAfxInfo(e2.measure(), AFX_TYPE_1), null, null, null, e2);
      }

      DeepNodes(final Node<A> e0, final Node<A> e1, final Node<A> e2, final Node<A> e3) {
        this(packAfxInfo(e0.measure() + e1.measure(), AFX_TYPE_2), e0, e1, null, null, 0, null, SimpleNodes.empty(), packAfxInfo(e2.measure() + e3.measure(), AFX_TYPE_2), null, null, e2, e3);
      }

      static int packAfxInfo(int measure, byte affixType) {
        return (measure << RESERVED_BITS) | affixType;
      }

      int pfxMeasure() {
        return pfxInfo >>> RESERVED_BITS;
      }

      int sfxMeasure() {
        return sfxInfo >>> RESERVED_BITS;
      }

      byte pfxType() {
        return (byte) (pfxInfo & ((1 << RESERVED_BITS) - 1));
      }

      byte sfxType() {
        return (byte) (sfxInfo & ((1 << RESERVED_BITS) - 1));
      }

      @Override
      @Nonnull A get(int idx) {
        assert idx >= 0;
        if (idx < pfxMeasure()) {
          final int pfx0Measure = pfx0.measure();
          if (idx < pfx0Measure) return pfx0.get(idx);
          idx = idx - pfx0Measure;
          final int pfx1Measure = pfx1.measure();
          if (idx < pfx1Measure) return pfx1.get(idx);
          idx = idx - pfx1Measure;
          final int pfx2Measure = pfx2.measure();
          if (idx < pfx2Measure) return pfx2.get(idx);
          idx = idx - pfx2Measure;
          return pfx3.get(idx);
        }
        if (idx < pfxMeasure() + subMeasure) {
          idx = idx - pfxMeasure();
          final IntHolder offsetHolder = new IntHolder();
          final Node<A> node = sub().get(idx, offsetHolder);
          return node.get(idx - offsetHolder.value);
        }
        if (idx < pfxMeasure() + subMeasure + sfxMeasure()) {
          idx = idx - pfxMeasure() - subMeasure;
          final byte sfxType = sfxType();
          if (sfxType == AFX_TYPE_4) {
            final int sfx3Measure = sfx3.measure();
            if (idx < sfx3Measure) return sfx3.get(idx);
            idx = idx - sfx3Measure;
          }
          if (sfxType >= AFX_TYPE_3) {
            final int sfx2Measure = sfx2.measure();
            if (idx < sfx2Measure) return sfx2.get(idx);
            idx = idx - sfx2Measure;
          }
          if (sfxType >= AFX_TYPE_2) {
            final int sfx1Measure = sfx1.measure();
            if (idx < sfx1Measure) return sfx1.get(idx);
            idx = idx - sfx1Measure;
          }
          return sfx0.get(idx);
        }
        assert false;
        return null;
      }

      @Override
      @Nonnull A get(int idx, final IntHolder offsetHolder) {
        assert idx >= 0;
        int skipped = 0;
        if (idx < pfxMeasure()) {
          final int pfx0Measure = pfx0.measure();
          if (idx < pfx0Measure) {
            offsetHolder.value += skipped;
            return pfx0.get(idx, offsetHolder);
          }
          idx = idx - pfx0Measure;
          skipped += pfx0Measure;
          final int pfx1Measure = pfx1.measure();
          if (idx < pfx1Measure) {
            offsetHolder.value += skipped;
            return pfx1.get(idx, offsetHolder);
          }
          idx = idx - pfx1Measure;
          skipped += pfx1Measure;
          final int pfx2Measure = pfx2.measure();
          if (idx < pfx2Measure) {
            offsetHolder.value += skipped;
            return pfx2.get(idx, offsetHolder);
          }
          idx = idx - pfx2Measure;
          skipped += pfx2Measure;
          offsetHolder.value += skipped;
          return pfx3.get(idx, offsetHolder);
        }
        skipped += pfxMeasure();
        if (idx < pfxMeasure() + subMeasure) {
          idx = idx - pfxMeasure();
          final Node<A> node = sub().get(idx, offsetHolder);
          final A result = node.get(idx - offsetHolder.value, offsetHolder);
          offsetHolder.value += skipped;
          return result;
        }
        skipped += subMeasure;
        final int sfxMeasure = sfxMeasure();
        if (idx < pfxMeasure() + subMeasure + sfxMeasure) {
          idx = idx - pfxMeasure() - subMeasure;
          final byte sfxType = sfxType();
          if (sfxType == AFX_TYPE_4) {
            final int sfx3Measure = sfx3.measure();
            if (idx < sfx3Measure) {
              offsetHolder.value += skipped;
              return sfx3.get(idx, offsetHolder);
            }
            idx = idx - sfx3Measure;
            skipped += sfx3Measure;
          }
          if (sfxType >= AFX_TYPE_3) {
            final int sfx2Measure = sfx2.measure();
            if (idx < sfx2Measure) {
              offsetHolder.value += skipped;
              return sfx2.get(idx, offsetHolder);
            }
            idx = idx - sfx2Measure;
            skipped += sfx2Measure;
          }
          if (sfxType >= AFX_TYPE_2) {
            final int sfx1Measure = sfx1.measure();
            if (idx < sfx1Measure) {
              offsetHolder.value += skipped;
              return sfx1.get(idx, offsetHolder);
            }
            idx = idx - sfx1Measure;
            skipped += sfx1Measure;
          }
          offsetHolder.value += skipped;
          return sfx0.get(idx, offsetHolder);
        }
        assert false;
        return null;
      }

      @Override
      @Nonnull Nodes<A> prepend(final Node<A> node) {
        if (pfxType() != AFX_TYPE_4) {
          final int newPfxInfo = packAfxInfo(pfxMeasure() + node.measure(), (byte) (pfxType() + 1));
          return new DeepNodes<>(newPfxInfo, node, pfx0, pfx1, pfx2, subMeasure, subComputation, subResult, sfxInfo, sfx3, sfx2, sfx1, sfx0);
        }
        final int pfx1Measure = pfx1.measure();
        final int pfx2Measure = pfx2.measure();
        final int pfx3Measure = pfx3.measure();
        final Node<Node<A>> triple = new Node.Three<>(pfx1, pfx1Measure, pfx2, pfx2Measure, pfx3, pfx3Measure);
        final int newPfxInfo = packAfxInfo(node.measure() + pfx0.measure(), AFX_TYPE_2);
        final int newSubMeasure = subMeasure + pfx1Measure + pfx2Measure + pfx3Measure;
        final Nodes<Node<A>> parentSub = sub();
        return new DeepNodes<>(newPfxInfo, node, pfx0, null, null, newSubMeasure, () -> parentSub.prepend(triple), null, sfxInfo, sfx3, sfx2, sfx1, sfx0);
      }

      @Override
      @Nonnull Nodes<A> append(final Node<A> node) {
        if (sfxType() != AFX_TYPE_4) {
          final int newSfxInfo = packAfxInfo(sfxMeasure() + node.measure(), (byte) (sfxType() + 1));
          return new DeepNodes<>(pfxInfo, pfx0, pfx1, pfx2, pfx3, subMeasure, subComputation, subResult, newSfxInfo, sfx2, sfx1, sfx0, node);
        }
        final int sfx3Measure = sfx3.measure();
        final int sfx2Measure = sfx2.measure();
        final int sfx1Measure = sfx1.measure();
        final Node<Node<A>> triple = new Node.Three<>(sfx3, sfx3Measure, sfx2, sfx2Measure, sfx1, sfx1Measure);
        final int newSfxInfo = packAfxInfo(sfx0.measure() + node.measure(), AFX_TYPE_2);
        final int newSubMeasure = subMeasure + sfx3Measure + sfx2Measure + sfx1Measure;
        final Nodes<Node<A>> parentSub = sub();
        return new DeepNodes<>(pfxInfo, pfx0, pfx1, pfx2, pfx3, newSubMeasure, () -> parentSub.append(triple), null, newSfxInfo, null, null, sfx0, node);
      }

      @Override
      @Nonnull Node<A> headNode() {
        return pfx0;
      }

      @Override
      @Nonnull Nodes<A> tailNodes() {
        if (pfxType() != AFX_TYPE_1) {
          int newPfxInfo = packAfxInfo(pfxMeasure() - pfx0.measure(), (byte) (pfxType() - 1));
          return new DeepNodes<>(newPfxInfo, pfx1, pfx2, pfx3, null, subMeasure, subComputation, subResult, sfxInfo, sfx3, sfx2, sfx1, sfx0);
        }
        if (subMeasure != 0) {
          final Nodes<Node<A>> parentSub = sub();
          final Node<Node<A>> head = parentSub.headNode();
          final int headMeasure = head.measure();
          switch (head.type()) {
            case Node.NODE_TYPE_2: {
              final int newPfxInfo = packAfxInfo(headMeasure, AFX_TYPE_2);
              return new DeepNodes<>(newPfxInfo, head.at(0), head.at(1), null, null, subMeasure - headMeasure, parentSub::tailNodes, null, sfxInfo, sfx3, sfx2, sfx1, sfx0);
            }
            case Node.NODE_TYPE_3: {
              final int newPfxInfo = packAfxInfo(headMeasure, AFX_TYPE_3);
              return new DeepNodes<>(newPfxInfo, head.at(0), head.at(1), head.at(2), null, subMeasure - headMeasure, parentSub::tailNodes, null, sfxInfo, sfx3, sfx2, sfx1, sfx0);
            }
            default: assert false;
          }
        }
        switch (sfxType()) {
          case AFX_TYPE_1: return new SimpleNodes<>(sfx0);
          case AFX_TYPE_2: return new DeepNodes<>(sfx1, sfx0);
          case AFX_TYPE_3: return new DeepNodes<>(sfx2, sfx1, sfx0);
          case AFX_TYPE_4: return new DeepNodes<>(sfx3, sfx2, sfx1, sfx0);
          default: assert false;
        }
        return null;
      }

      @Override
      @Nonnull Node<A> lastNode() {
        return sfx0;
      }

      @Override
      @Nonnull Nodes<A> initNodes() {
        if (sfxType() != AFX_TYPE_1) {
          int newSfxInfo = packAfxInfo(sfxMeasure() - sfx0.measure(), (byte) (sfxType() - 1));
          return new DeepNodes<>(pfxInfo, pfx0, pfx1, pfx2, pfx3, subMeasure, subComputation, subResult, newSfxInfo, null, sfx3, sfx2, sfx1);
        }
        if (subMeasure != 0) {
          final Nodes<Node<A>> parentSub = sub();
          final Node<Node<A>> last = sub().lastNode();
          final int lastMeasure = last.measure();
          switch (last.type()) {
            case Node.NODE_TYPE_2: {
              final int newSfxInfo = packAfxInfo(lastMeasure, AFX_TYPE_2);
              return new DeepNodes<>(pfxInfo, pfx0, pfx1, pfx2, pfx3, subMeasure - lastMeasure, parentSub::initNodes, null, newSfxInfo, null, null, last.at(1), last.at(0));
            }
            case Node.NODE_TYPE_3: {
              final int newSfxInfo = packAfxInfo(lastMeasure, AFX_TYPE_3);
              return new DeepNodes<>(pfxInfo, pfx0, pfx1, pfx2, pfx3, subMeasure - lastMeasure, parentSub::initNodes, null, newSfxInfo, null, last.at(2), last.at(1), last.at(0));
            }
            default:
              assert false;
          }
        }
        switch (pfxType()) {
          case AFX_TYPE_1: return new SimpleNodes<>(pfx0);
          case AFX_TYPE_2:return new DeepNodes<>(pfx0, pfx1);
          case AFX_TYPE_3:return new DeepNodes<>(pfx0, pfx1, pfx2);
          case AFX_TYPE_4:return new DeepNodes<>(pfx0, pfx1, pfx1, pfx2);
          default: assert false;
        }
        return null;
      }

      @Override
      boolean isEmpty() {
        return false;
      }

      @Override
      @Nonnull <B> B foldRight(final BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
        B result = initial;
        switch (sfxType()) {
          case AFX_TYPE_1: {
            result = sfx0.foldRight(function, result);
            break;
          }
          case AFX_TYPE_2: {
            result = sfx0.foldRight(function, result);
            result = sfx1.foldRight(function, result);
            break;
          }
          case AFX_TYPE_3: {
            result = sfx0.foldRight(function, result);
            result = sfx1.foldRight(function, result);
            result = sfx2.foldRight(function, result);
            break;
          }
          case AFX_TYPE_4: {
            result = sfx0.foldRight(function, result);
            result = sfx1.foldRight(function, result);
            result = sfx2.foldRight(function, result);
            result = sfx3.foldRight(function, result);
            break;
          }
          default: assert false;
        }
        result = sub().foldRight((node, b) -> node.foldRight(function, b), result);
        switch (pfxType()) {
          case AFX_TYPE_4: {
            result = pfx3.foldRight(function, result);
            result = pfx2.foldRight(function, result);
            result = pfx1.foldRight(function, result);
            result = pfx0.foldRight(function, result);
            break;
          }
          case AFX_TYPE_3: {
            result = pfx2.foldRight(function, result);
            result = pfx1.foldRight(function, result);
            result = pfx0.foldRight(function, result);
            break;
          }
          case AFX_TYPE_2: {
            result = pfx1.foldRight(function, result);
            result = pfx0.foldRight(function, result);
            break;
          }
          case AFX_TYPE_1: {
            result = pfx0.foldRight(function, result);
            break;
          }
          default: assert false;
        }
        return result;
      }

      @Override
      @Nonnull <B> B foldLeft(final BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
        B result = initial;
        switch (pfxType()) {
          case AFX_TYPE_1: {
            result = pfx0.foldLeft(function, result);
            break;
          }
          case AFX_TYPE_2: {
            result = pfx0.foldLeft(function, result);
            result = pfx1.foldLeft(function, result);
            break;
          }
          case AFX_TYPE_3: {
            result = pfx0.foldLeft(function, result);
            result = pfx1.foldLeft(function, result);
            result = pfx2.foldLeft(function, result);
            break;
          }
          case AFX_TYPE_4: {
            result = pfx0.foldLeft(function, result);
            result = pfx1.foldLeft(function, result);
            result = pfx2.foldLeft(function, result);
            result = pfx3.foldLeft(function, result);
            break;
          }
          default: assert false;
        }
        result = sub().foldLeft((b, node) -> node.foldLeft(function, b), result);
        switch (sfxType()) {
          case AFX_TYPE_4: {
            result = sfx3.foldLeft(function, result);
            result = sfx2.foldLeft(function, result);
            result = sfx1.foldLeft(function, result);
            result = sfx0.foldLeft(function, result);
            break;
          }
          case AFX_TYPE_3: {
            result = sfx2.foldLeft(function, result);
            result = sfx1.foldLeft(function, result);
            result = sfx0.foldLeft(function, result);
            break;
          }
          case AFX_TYPE_2: {
            result = sfx1.foldLeft(function, result);
            result = sfx0.foldLeft(function, result);
            break;
          }
          case AFX_TYPE_1: {
            result = sfx0.foldLeft(function, result);
            break;
          }
          default: assert false;
        }
        return result;
      }

      @Override
      @Nonnull Nodes<A> append0(final Node<A>[] mid, final Nodes<A> right) {
        return right.prepend0(this, mid);
      }

      @Override
      @Nonnull Nodes<A> prepend0(final SimpleNodes<A> left, final Node<A>[] mid) {
        return concat(left, mid, this);
      }

      @Override
      @Nonnull Nodes<A> prepend0(final DeepNodes<A> left, final Node<A>[] mid) {
        return concat(left, mid, this);
      }
    }

    private static final class SimpleNodes<A> extends Nodes<A> {
      private static final @Nonnull SimpleNodes<?> EMPTY = new SimpleNodes<>();

      private final Node<A> value;

      SimpleNodes(Node<A> value) {
        this.value = value;
      }

      SimpleNodes() {
        this.value = null;
      }

      @Override
      @Nonnull A get(final int idx) {
        assert idx >= 0;
        assert value != null;
        assert idx < value.measure();
        return value.get(idx);
      }

      @Override
      @Nonnull A get(final int idx, final IntHolder offsetHolder) {
        assert idx >= 0;
        assert value != null;
        assert idx < value.measure();
        return value.get(idx, offsetHolder);
      }

      @Override
      @Nonnull Nodes<A> prepend(final Node<A> node) {
        if (value == null) return new SimpleNodes<>(node);
        return new DeepNodes<>(DeepNodes.packAfxInfo(node.measure(), AFX_TYPE_1), node, null, null, null, 0, null, SimpleNodes.empty(), DeepNodes.packAfxInfo(value.measure(), AFX_TYPE_1), null, null, null, value);
      }

      @Override
      @Nonnull Nodes<A> append(final Node<A> node) {
        if (value == null) return new SimpleNodes<>(node);
        return new DeepNodes<>(DeepNodes.packAfxInfo(value.measure(), AFX_TYPE_1), value, null, null, null, 0, null, SimpleNodes.empty(), DeepNodes.packAfxInfo(node.measure(), AFX_TYPE_1), null, null, null, node);
      }

      @Override
      @Nonnull Node<A> headNode() {
        assert value != null;
        return value;
      }

      @Override
      @Nonnull Nodes<A> tailNodes() {
        assert value != null;
        return empty();
      }

      @Override
      @Nonnull Node<A> lastNode() {
        assert value != null;
        return value;
      }

      @Override
      @Nonnull Nodes<A> initNodes() {
        assert value != null;
        return empty();
      }

      @Override
      boolean isEmpty() {
        return value == null;
      }

      @Override
      @Nonnull <B> B foldRight(final BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
        return value == null ? initial : value.foldRight(function, initial);
      }

      @Override
      @Nonnull <B> B foldLeft(final BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
        return value == null ? initial : value.foldLeft(function, initial);
      }

      @Override
      @Nonnull Nodes<A> append0(final Node<A>[] mid, final Nodes<A> right) {
        return right.prepend0(this, mid);
      }

      @Override
      @Nonnull Nodes<A> prepend0(final SimpleNodes<A> left, final Node<A>[] mid) {
        return concat(left, mid, this);
      }

      @Override
      @Nonnull Nodes<A> prepend0(final DeepNodes<A> left, final Node<A>[] mid) {
        return concat(left, mid, this);
      }

      @SuppressWarnings("unchecked")
      static @Nonnull <A> SimpleNodes<A> empty() {
        return (SimpleNodes<A>) EMPTY;
      }
    }
  }

  /**
   * <p>Kleisli arrow is a pure function from an argument of type <b>A</b> to <b>Sequence&lt;B&gt;</b>. </p>
   * <p>It can be combined with other arrows of the same type (but parameterized differently) in ways similar to how
   * {@link Function}s can be combined with other functions.</p>
   * @param <A> argument type.
   * @param <B> return type parameter.
   */
  @FunctionalInterface
  interface Kleisli<A, B> {
    /**
     * Run the computation, producing a monad.
     * @param arg computation argument. Can't be null.
     * @return new monad. Can't be null.
     */
    @Nonnull Sequence<B> run(A arg);

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>Z -&gt; A -&gt; B</b>.
     * @param kleisli <b>Z -&gt; A</b> arrow. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> Kleisli<Z, B> precomposeKleisli(final Kleisli<? super Z, A> kleisli) {
      nonNull(kleisli);
      return z -> kleisli.run(z).bind(this);
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>Z -&gt; A -&gt; B</b>.
     * @param function <b>Z -&gt; A</b> function. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> Kleisli<Z, B> precomposeFunction(final Function<? super Z, ? extends A> function) {
      nonNull(function);
      return z -> run(function.$(z));
    }

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>A -&gt; B -&gt; C</b>.
     * @param kleisli <b>B -&gt; C</b> arrow. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> Kleisli<A, C> postcomposeKleisli(final Kleisli<? super B, C> kleisli) {
      nonNull(kleisli);
      return a -> run(a).bind(kleisli);
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>A -&gt; B -&gt; C</b>.
     * @param function <b>B -&gt; C</b> function. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> Kleisli<A, C> postcomposeFunction(final Function<? super B, ? extends C> function) {
      nonNull(function);
      return a -> run(a).map(function);
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Left} and passes it
     * unchanged otherwise.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, Either<B, C>> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> sequence(Either.right(c)));
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Right} and passes it
     * unchanged otherwise.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> sequence(Either.left(c)), a -> run(a).map(Either::right));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(sequence(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> sequence(ca.first).zip(run(ca.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps its input using this arrow if it is {@link Either.Left} and using the given arrow if
     * it is {@link Either.Right}.
     * @param kleisli right <b>C -&gt; D</b> mapping. Can't be null.
     * @param <C> right argument type.
     * @param <D> right return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C, D> Kleisli<Either<A, C>, Either<B, D>> sum(final Kleisli<? super C, D> kleisli) {
      nonNull(kleisli);
      return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
    }

    /**
     * Returns an arrow which maps the first part of its input using this arrow and the second part using the given arrow.
     * @param kleisli second <b>C -&gt; D</b> mapping. Can't be null.
     * @param <C> second argument type.
     * @param <D> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C, D> Kleisli<Pair<A, C>, Pair<B, D>> product(final Kleisli<? super C, D> kleisli) {
      nonNull(kleisli);
      return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps input using this arrow if it is {@link Either.Left} or the given arrow if it is {@link Either.Right}.
     * @param kleisli left <b>C -&gt; B</b> mapping. Can't be null.
     * @param <C> right argument type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, B> fanIn(final Kleisli<? super C, B> kleisli) {
      nonNull(kleisli);
      return ac -> ac.either(this::run, kleisli::run);
    }

    /**
     * Returns an arrow which maps its input using this arrow and the given arrow and returns two resulting values as a pair.
     * @param kleisli second <b>A -&gt; C</b> mapping. Can't be null.
     * @param <C> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> Kleisli<A, Pair<B, C>> fanOut(final Kleisli<? super A, C> kleisli) {
      nonNull(kleisli);
      return a -> run(a).zip(kleisli.run(a), Pair::pair);
    }

    /**
     * Returns an arrow wrapping the given function.
     * @param function <b>A -&gt; B</b> function to wrap. Can't be null.
     * @param <A> argument type.
     * @param <B> return type parameter.
     * @return an arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    static @Nonnull <A, B> Kleisli<A, B> lift(final Function<? super A, ? extends B> function) {
      nonNull(function);
      return a -> sequence(function.$(a));
    }

    /**
     * Returns an arrow <b>(A -&gt; B, B) -&gt; B</b> which applies its input arrow (<b>A -&gt; B</b>) to its input
     * value (<b>A</b>) and returns the result (<b>B</b>).
     * @param <A> argument type.
     * @param <B> return type.
     * @return an arrow. Can't be null.
     */
    static @Nonnull <A, B> Kleisli<Pair<Kleisli<A, B>, A>, B> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}