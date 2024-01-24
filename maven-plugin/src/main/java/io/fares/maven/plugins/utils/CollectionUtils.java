/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fares.maven.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CollectionUtils {

  public static Comparator<?> LT = new NegativeComparator();
  public static Comparator<?> GT = new PositiveComparator();

  public static <T, V> List<V> apply(Collection<T> collection,
                                     Function<T, V> function) {
    final List<V> list = new ArrayList<V>(collection.size());
    for (T t : collection) {
      list.add(function.eval(t));
    }
    return list;
  }

  public static <T, V> V bestValue(Collection<T> collection,
                                   CollectionUtils.Function<T, V> function, Comparator<V> comparator) {

    if (collection == null || collection.isEmpty())
      return null;

    final Iterator<T> i = collection.iterator();
    V candidateValue = function.eval(i.next());

    while (i.hasNext()) {
      final V nextValue = function.eval(i.next());
      if (comparator.compare(candidateValue, nextValue) < 0) {
        candidateValue = nextValue;
      }
    }
    return candidateValue;
  }

  public static <V extends Object & Comparable<? super V>> Comparator<V> lt() {
    @SuppressWarnings("unchecked")
    final Comparator<V> comparator = (Comparator<V>) LT;
    return comparator;
  }

  public static <V extends Object & Comparable<? super V>> Comparator<V> gt() {
    @SuppressWarnings("unchecked")
    final Comparator<V> comparator = (Comparator<V>) GT;
    return comparator;
  }

  public static <T> T[] toArray(Collection<T> collection, Class<T> clazz) {

    @SuppressWarnings("unchecked")
    T[] tt = (T[]) java.lang.reflect.Array.newInstance(clazz,
      collection.size());

    return collection.toArray(tt);

  }

  public interface Function<T, V> {
    public V eval(T argument);
  }

  public static class PositiveComparator<V extends Object & Comparable<? super V>>
    implements Comparator<V> {
    public int compare(V o1, V o2) {
      if (o1 == null && o2 == null)
        return 0;
      else if (o1 == null)
        return 1;
      else if (o2 == null)
        return -1;
      else
        return o1.compareTo(o2);
    }
  }

  public static class NegativeComparator<V extends Object & Comparable<? super V>>
    implements Comparator<V> {
    public int compare(V o1, V o2) {
      if (o1 == null && o2 == null)
        return 0;
      else if (o1 == null)
        return -1;
      else if (o2 == null)
        return 1;
      else
        return -o1.compareTo(o2);
    }
  }

}
