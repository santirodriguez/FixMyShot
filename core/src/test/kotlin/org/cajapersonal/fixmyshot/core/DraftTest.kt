package org.cajapersonal.fixmyshot.core

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DraftTest(private val name: String, private val case: () -> Unit) {
    @Test fun contract() = case()

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> = draftCases().map { arrayOf(it.first, it.second) }
    }
}
