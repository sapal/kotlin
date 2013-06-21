// Auto-generated by org.jetbrains.jet.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
import java.util.ArrayList
import java.lang as j

import java.lang.Integer.MAX_VALUE as MaxI
import java.lang.Integer.MIN_VALUE as MinI
import java.lang.Byte.MAX_VALUE as MaxB
import java.lang.Byte.MIN_VALUE as MinB
import java.lang.Short.MAX_VALUE as MaxS
import java.lang.Short.MIN_VALUE as MinS
import java.lang.Long.MAX_VALUE as MaxL
import java.lang.Long.MIN_VALUE as MinL
import java.lang.Character.MAX_VALUE as MaxC
import java.lang.Character.MIN_VALUE as MinC

fun box(): String {
    val list1 = ArrayList<Int>()
    val range1 = (MinI + 5) downTo MinI step 3
    for (i in range1) {
        list1.add(i)
        if (list1.size() > 23) break
    }
    if (list1 != listOf<Int>(MinI + 5, MinI + 2)) {
        return "Wrong elements for (MinI + 5) downTo MinI step 3: $list1"
    }

    val list2 = ArrayList<Byte>()
    val range2 = (MinB + 5).toByte() downTo MinB step 3
    for (i in range2) {
        list2.add(i)
        if (list2.size() > 23) break
    }
    if (list2 != listOf<Byte>((MinB + 5).toByte(), (MinB + 2).toByte())) {
        return "Wrong elements for (MinB + 5).toByte() downTo MinB step 3: $list2"
    }

    val list3 = ArrayList<Short>()
    val range3 = (MinS + 5).toShort() downTo MinS step 3
    for (i in range3) {
        list3.add(i)
        if (list3.size() > 23) break
    }
    if (list3 != listOf<Short>((MinS + 5).toShort(), (MinS + 2).toShort())) {
        return "Wrong elements for (MinS + 5).toShort() downTo MinS step 3: $list3"
    }

    val list4 = ArrayList<Long>()
    val range4 = (MinL + 5).toLong() downTo MinL step 3
    for (i in range4) {
        list4.add(i)
        if (list4.size() > 23) break
    }
    if (list4 != listOf<Long>((MinL + 5).toLong(), (MinL + 2).toLong())) {
        return "Wrong elements for (MinL + 5).toLong() downTo MinL step 3: $list4"
    }

    val list5 = ArrayList<Char>()
    val range5 = (MinC + 5).toChar() downTo MinC step 3
    for (i in range5) {
        list5.add(i)
        if (list5.size() > 23) break
    }
    if (list5 != listOf<Char>((MinC + 5).toChar(), (MinC + 2).toChar())) {
        return "Wrong elements for (MinC + 5).toChar() downTo MinC step 3: $list5"
    }

    return "OK"
}