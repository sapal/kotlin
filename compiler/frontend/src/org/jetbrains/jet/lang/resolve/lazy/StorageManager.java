/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.lazy;

import com.intellij.openapi.util.Computable;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingTrace;

import java.util.Collection;

public interface StorageManager {
    /**
     * Given a function compute: K -> V create a memoized version of it that computes a value only once for each key
     * @param compute the function to be memoized
     * @param modeForValues how to store teh memoized values
     */
    @NotNull
    <K, V> Function<K, V> createMemoizedFunction(@NotNull Function<K, V> compute, @NotNull MemoizationMode modeForValues);
    @NotNull
    <K, V> Function<K, V> createMemoizedFunctionWithNullableValues(@NotNull Function<K, V> compute, @NotNull MemoizationMode modeForValues);

    <E> Collection<E> createConcurrentCollection();

    @NotNull
    <T> LazyValue<T> createLazyValue(@NotNull Computable<T> computable);

    @NotNull
    <T> LazyValue<T> createNullableLazyValue(@NotNull Computable<T> computable);

    @NotNull
    BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace);

    enum MemoizationMode {
        STRONG,
        WEAK
    }
}
