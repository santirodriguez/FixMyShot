package org.cajapersonal.fixmyshot.review

/** Latest completed import only. Positions follow received order; no names, URIs or image bytes. */
internal data class ImportProblem(val position: Int, val reason: ReviewSources.Failure)

internal data class ImportReport(
    val total: Int,
    val added: Int,
    val ignored: Int,
    val problems: List<ImportProblem>,
) {
    init {
        require(total in 1..100 && added >= 0 && ignored >= 0 && added + ignored + problems.size == total)
        require(problems.map { it.position }.distinct().size == problems.size && problems.all { it.position in 1..total })
    }
    companion object {
        fun from(results: List<ReviewSources.ImportResult>, acceptedIds: Set<String>): ImportReport {
            val added = results.filterIsInstance<ReviewSources.ImportResult.Added>().count { it.image.id in acceptedIds }
            val problems = results.mapIndexedNotNull { index, result ->
                (result as? ReviewSources.ImportResult.Rejected)?.let { ImportProblem(index + 1, it.reason) }
            }
            return ImportReport(results.size, added, results.size - added - problems.size, problems)
        }
    }
}
