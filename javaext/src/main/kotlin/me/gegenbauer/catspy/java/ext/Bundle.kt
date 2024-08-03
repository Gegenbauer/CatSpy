package me.gegenbauer.catspy.java.ext

class Bundle {

    private val map = mutableMapOf<String, Any>()

    @Synchronized
    fun <T> put(key: String, value: T) {
        map[key] = value as Any
    }

    @Synchronized
    fun <T> get(key: String): T? {
        return map[key] as? T
    }

    @Synchronized
    fun remove(key: String) {
        map.remove(key)
    }

    @Synchronized
    fun clear() {
        map.clear()
    }

    @Synchronized
    fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    @Synchronized
    fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    @Synchronized
    fun isNotEmpty(): Boolean {
        return map.isNotEmpty()
    }

    @Synchronized
    fun size(): Int {
        return map.size
    }

    @Synchronized
    fun keys(): Set<String> {
        return map.keys
    }

    @Synchronized
    fun values(): Collection<Any> {
        return map.values
    }

    @Synchronized
    fun entries(): Set<Map.Entry<String, Any>> {
        return map.entries
    }

    @Synchronized
    fun putAll(bundle: Bundle) {
        map.putAll(bundle.map)
    }

    @Synchronized
    fun toMap(): Map<String, Any> {
        return map.toMap()
    }

    override fun toString(): String {
        return map.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Bundle

        return map == other.map
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }
}