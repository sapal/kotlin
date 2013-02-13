// Auto-generated by org.jetbrains.jet.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
import java.util.ArrayList
import java.lang as j

fun box(): String {
    val list1 = ArrayList<Int>()
    for (i in (5 downTo 3).reversed()) {
        list1.add(i)
    }
    if (list1 != listOf<Int>(3, 4, 5)) {
        return "Wrong elements for (5 downTo 3).reversed(): $list1"
    }

    val list2 = ArrayList<Byte>()
    for (i in (5.toByte() downTo 3.toByte()).reversed()) {
        list2.add(i)
    }
    if (list2 != listOf<Byte>(3, 4, 5)) {
        return "Wrong elements for (5.toByte() downTo 3.toByte()).reversed(): $list2"
    }

    val list3 = ArrayList<Short>()
    for (i in (5.toShort() downTo 3.toShort()).reversed()) {
        list3.add(i)
    }
    if (list3 != listOf<Short>(3, 4, 5)) {
        return "Wrong elements for (5.toShort() downTo 3.toShort()).reversed(): $list3"
    }

    val list4 = ArrayList<Long>()
    for (i in (5.toLong() downTo 3.toLong()).reversed()) {
        list4.add(i)
    }
    if (list4 != listOf<Long>(3, 4, 5)) {
        return "Wrong elements for (5.toLong() downTo 3.toLong()).reversed(): $list4"
    }

    val list5 = ArrayList<Char>()
    for (i in ('c' downTo 'a').reversed()) {
        list5.add(i)
    }
    if (list5 != listOf<Char>('a', 'b', 'c')) {
        return "Wrong elements for ('c' downTo 'a').reversed(): $list5"
    }

    val list6 = ArrayList<Double>()
    for (i in (5.0 downTo 3.0).reversed()) {
        list6.add(i)
    }
    if (list6 != listOf<Double>(3.0, 4.0, 5.0)) {
        return "Wrong elements for (5.0 downTo 3.0).reversed(): $list6"
    }

    val list7 = ArrayList<Float>()
    for (i in (5.0.toFloat() downTo 3.0.toFloat()).reversed()) {
        list7.add(i)
    }
    if (list7 != listOf<Float>(3.0, 4.0, 5.0)) {
        return "Wrong elements for (5.0.toFloat() downTo 3.0.toFloat()).reversed(): $list7"
    }

    return "OK"
}
