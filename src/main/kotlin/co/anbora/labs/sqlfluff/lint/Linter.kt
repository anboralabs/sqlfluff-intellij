package co.anbora.labs.sqlfluff.lint

import co.anbora.labs.sqlfluff.ide.annotator.LinterExternalAnnotator
import co.anbora.labs.sqlfluff.ide.quickFix.QuickFixesManager
import co.anbora.labs.sqlfluff.ide.settings.Settings
import co.anbora.labs.sqlfluff.ide.settings.Settings.DEFAULT_ARGUMENTS
import co.anbora.labs.sqlfluff.ide.settings.Settings.OPTION_KEY_PYTHON
import co.anbora.labs.sqlfluff.ide.settings.Settings.OPTION_KEY_SQLLINT
import co.anbora.labs.sqlfluff.ide.settings.Settings.OPTION_KEY_SQLLINT_ARGUMENTS
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Pattern

sealed class Linter {

    protected val LOGGER: Logger = Logger.getInstance(
        Linter::class.java
    )

    open fun lint(
        file: PsiFile,
        document: Document
    ): List<LinterExternalAnnotator.Error> {

        val vFile = file.virtualFile
        if (null == vFile) {
            LOGGER.error("No valid file found!")
            return emptyList()
        }
        val canonicalPath = vFile.canonicalPath
        if (canonicalPath.isNullOrBlank()) {
            LOGGER.error("Failed to get canonical path!")
            return emptyList()
        }

        // First time users will not have this Option set if they do not open the Settings
        // UI yet.
        var arguments = Settings[OPTION_KEY_SQLLINT_ARGUMENTS]
        if (arguments.isBlank()) {
            arguments = DEFAULT_ARGUMENTS
        }

        val args: List<String> = buildCommandLineArgs(
            Settings[OPTION_KEY_PYTHON],
            Settings[OPTION_KEY_SQLLINT],
            arguments,
            file
        )

        return runLinter(file, document, args)
    }

    fun runLinter(
        file: PsiFile,
        document: Document,
        args: List<String>
    ): List<LinterExternalAnnotator.Error> {
        val pb = ProcessBuilder(args)
        val proc: Process = try {
            pb.start()
        } catch (e: IOException) {
            LOGGER.error(
                "Failed to run lint against file: " + file.virtualFile.canonicalPath,
                e
            )
            return emptyList()
        }
        val problemDescriptors: MutableList<LinterExternalAnnotator.Error> = ArrayList()
        try {
            BufferedReader(InputStreamReader(proc.inputStream)).use { stdError ->
                var line: String? = null
                while (stdError.readLine().also { line = it } != null) {
                    val problemDescriptor =
                        parseLintResult(
                            file,
                            document,
                            line
                        ) ?: continue
                    problemDescriptors.add(problemDescriptor)
                }
            }
        } catch (e: IOException) {
            LOGGER.error(
                "Failed to run lint against file: " + file.virtualFile.canonicalPath,
                e
            )
            return emptyList()
        }

        return problemDescriptors
    }

    private val PATTERN = Pattern.compile("L:\\s+(\\d+)\\s+\\|\\s+P:\\s+(\\d+)\\s+\\|\\s+(L\\d+)\\s+\\|\\s+(\\D+)")

    fun parseLintResult(
        file: PsiFile,
        document: Document,
        line: String?
    ): LinterExternalAnnotator.Error? {

        val matcher = PATTERN.matcher(line)
        if (!matcher.matches()) {
            return null
        }
        var lineNumber = matcher.group(1).toInt(10)
        val lineCount = document.lineCount
        if (0 == lineCount) {
            return null
        }
        lineNumber = if (lineNumber >= lineCount) lineCount - 1 else lineNumber
        lineNumber = if (lineNumber > 0) lineNumber - 1 else 0

        val position = matcher.group(2).toInt(10)
        val errorType = matcher.group(3)
        val errorDescription = matcher.group(4)

        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)

        val errorMessage = "sqlfluff [$errorType]: $errorDescription"

        val initialPosition = if (position > 0) position -1 else 0

        val lit = PsiUtilCore.getElementAtOffset(file, position)

        val fix = QuickFixesManager[errorType]
        return LinterExternalAnnotator.Error(
            errorMessage,
            lit.textRange,
            errorType
        )
    }

    abstract fun buildCommandLineArgs(
        python: String,
        lint: String,
        lintOptions: String,
        file: PsiFile
    ): List<String>
}