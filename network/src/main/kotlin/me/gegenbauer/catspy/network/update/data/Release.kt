package me.gegenbauer.catspy.network.update.data

data class Release(
    val id: Int,
    val name: String,
    val body: String,
    val assets: List<Asset>,
) {

    constructor(versionName: String) : this(0, versionName, "", emptyList())

    operator fun compareTo(other: Release): Int {
        return compareVersionNames(name, other.name)
    }

    private fun compareVersionNames(version1: String, version2: String): Int {
        val preReleaseOrder = listOf(VERSION_APPENDIX_ALPHA, VERSION_APPENDIX_BETA, VERSION_APPENDIX_RC)

        val validParts1 = substringFromFirstDigit(version1)
        val validParts2 = substringFromFirstDigit(version2)
        if (validParts1.isBlank() || validParts2.isBlank()) {
            return -1
        }
        val v1 = validParts1.split(VERSION_APPENDIX_SPLITTER)
        val v2 = validParts2.split(VERSION_APPENDIX_SPLITTER)

        val nums1 = v1[0].split(VERSION_SPLITTER)
        val nums2 = v2[0].split(VERSION_SPLITTER)
        val length = maxOf(nums1.size, nums2.size)
        for (i in 0 until length) {
            val num1 = if (i < nums1.size) nums1[i].toInt() else 0
            val num2 = if (i < nums2.size) nums2[i].toInt() else 0
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }

        if (v1.size == 1 && v2.size == 2) return 1
        if (v1.size == 2 && v2.size == 1) return -1
        if (v1.size == 2 && v2.size == 2) {
            val preRelease1 = v1[1]
            val preRelease2 = v2[1]
            val preRelease1Index = preReleaseOrder.indexOf(preRelease1)
            val preRelease2Index = preReleaseOrder.indexOf(preRelease2)
            return preRelease1Index.compareTo(preRelease2Index)
        }

        return 0
    }

    private fun substringFromFirstDigit(text: String): String {
        val matchResult = "\\d.*".toRegex().find(text)
        return matchResult?.value ?: ""
    }

    fun isEmpty(): Boolean {
        return this == emptyRelease
    }

    companion object {
        private const val VERSION_APPENDIX_ALPHA = "alpha"
        private const val VERSION_APPENDIX_BETA = "beta"
        private const val VERSION_APPENDIX_RC = "rc"
        private const val VERSION_APPENDIX_SPLITTER = "-"
        private const val VERSION_SPLITTER = "."

        val emptyRelease = Release(0, "", "", emptyList())
    }
}