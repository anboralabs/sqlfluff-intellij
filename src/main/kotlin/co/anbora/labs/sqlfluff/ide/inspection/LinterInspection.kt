package co.anbora.labs.sqlfluff.ide.inspection

import co.anbora.labs.sqlfluff.ide.settings.Settings
import co.anbora.labs.sqlfluff.ide.settings.Settings.SELECTED_LINTER
import co.anbora.labs.sqlfluff.lint.LintRunner
import co.anbora.labs.sqlfluff.lint.LinterConfig
import co.anbora.labs.sqlfluff.lint.isSqlFileType
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile

class LinterInspection: LocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        if (!file.isSqlFileType()) {
            return arrayOf()
        }

        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)
            ?: return arrayOf()

        val linterType = LinterConfig.valueOf(Settings[SELECTED_LINTER])

        return linterType.lint(file, manager, document).toTypedArray()
    }



}