package org.cajapersonal.fixmyshot.core

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GeometryTest(private val name: String, private val case: () -> Unit) {
    @Test fun geometry() = case()

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> = geometryCases().map { arrayOf(it.first, it.second) }
    }
}
