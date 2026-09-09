package icu.nullptr.hidemyapplist.common

object CollectionUtils {
    inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (K, V) -> Boolean) {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (predicate(entry.key, entry.value)) iterator.remove()
        }
    }

    inline fun <K, V> MutableMap<K, V>.removeIfWithCount(predicate: (K, V) -> Boolean): Int {
        var count = 0
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (predicate(entry.key, entry.value)) {
                iterator.remove()
                count++
            }
        }
        return count
    }

    inline fun <K> MutableSet<K>.removeIfWithCount(predicate: (K) -> Boolean): Int {
        var count = 0
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next())) {
                iterator.remove()
                count++
            }
        }
        return count
    }

    inline fun <reified T> Array<*>.firstWithType(): T {
        return this.first { it is T } as T
    }

    inline fun <reified T> Array<*>.firstOrNullWithType(): T? {
        return this.firstOrNull { it is T } as? T
    }

    inline fun <reified T> Array<*>.lastWithType(): T {
        return this.last { it is T } as T
    }

    inline fun <reified T> Array<*>.lastOrNullWithType(): T? {
        return this.lastOrNull { it is T } as? T
    }

    inline fun <reified T> MutableList<T>.sync(elements: Array<T>) {
        clear()
        addAll(elements)
    }

    inline fun <reified T> MutableList<T>.sync(elements: Iterable<T>) {
        clear()
        addAll(elements)
    }

    inline fun <reified T> MutableSet<T>.sync(elements: Iterable<T>) {
        clear()
        addAll(elements)
    }

    inline fun <reified K, reified V> MutableMap<K, V>.sync(from: Map<K, V>) {
        clear()
        putAll(from)
    }
}
