package co.anbora.labs.sqlfluff.ide.quickFix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix

object QuickFixesManager {
    operator fun get(errorType: String): IntentionAction? = fixes[errorType]

    //TODO implement quick fixes
    private val fixes: Map<String, IntentionAction?> = mapOf(
        "L001" to null,
        "L002" to null,
        "L003" to null,
        "L004" to null,
        "L005" to null,
        "L006" to null,
        "L008" to null,
        "L009" to null,
        "L010" to null,
        "L011" to null,
        "L012" to null,
        "L013" to null,
        "L014" to null,
        "L015" to null,
        "L016" to null,
        "L017" to null,
        "L018" to null,
        "L019" to null,
        "L020" to null,
        "L021" to null,
        "L022" to null,
        "L023" to null,
        "L024" to null,
        "L025" to null,
        "L026" to null,
        "L027" to null,
        "L028" to null,
        "L029" to null,
        "L030" to null,
        "L031" to null,
        "L032" to null,
        "L033" to null,
        "L034" to null,
        "L035" to null,
        "L036" to null,
        "L037" to null,
        "L038" to null,
        "L039" to null,
        "L040" to null,
        "L041" to null,
        "L042" to null,
        "L043" to null,
        "L044" to null,
        "L045" to null,
        "L046" to null,
        "L047" to null,
        "L048" to null,
        "L049" to null,
        "L050" to null,
        "L051" to null,
        "L052" to null,
        "L053" to null,
        "L054" to null,
        "L055" to null,
        "L056" to null,
        "L057" to null,
        "L058" to null,
        "L059" to null,
        "L060" to null,
        "L061" to null,
        "L062" to null,
        "L063" to null,
        "L064" to null,
        "L065" to null,
        "L066" to null
    )
}