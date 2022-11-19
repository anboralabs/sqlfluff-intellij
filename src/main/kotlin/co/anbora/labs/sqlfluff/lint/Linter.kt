package co.anbora.labs.sqlfluff.lint

import co.anbora.labs.sqlfluff.ide.settings.Settings
import co.anbora.labs.sqlfluff.ide.settings.Settings.DEFAULT_ARGUMENTS
import co.anbora.labs.sqlfluff.ide.settings.Settings.OPTION_KEY_PYTHON
import co.anbora.labs.sqlfluff.ide.settings.Settings.OPTION_KEY_SQLLINT
import co.anbora.labs.sqlfluff.ide.settings.Settings.OPTION_KEY_SQLLINT_ARGUMENTS
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
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
        manager: InspectionManager,
        document: Document
    ): List<ProblemDescriptor> {

        val baseDir = file.project.baseDir
        if (null == baseDir) {
            LOGGER.error("No valid base directory found!")
            return emptyList()
        }
        val canonicalPath = baseDir.canonicalPath
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

        return runLinter(file, manager, document, canonicalPath, args)
    }

    fun runLinter(
        file: PsiFile,
        manager: InspectionManager,
        document: Document,
        canonicalPath: String,
        args: List<String>
    ): List<ProblemDescriptor> {
        val lintWorkingDirectory = file.virtualFile.toNioPath().parent.toFile()
        val pb = ProcessBuilder(args)
        pb.directory(lintWorkingDirectory)
        val proc: Process = try {
            pb.start()
        } catch (e: IOException) {
            LOGGER.error(
                "Failed to run lint against file: " + file.virtualFile.canonicalPath,
                e
            )
            return emptyList()
        }
        val problemDescriptors: MutableList<ProblemDescriptor> = ArrayList()
        try {
            BufferedReader(InputStreamReader(proc.inputStream)).use { stdError ->
                var line: String? = null
                while (stdError.readLine().also { line = it } != null) {
                    val problemDescriptor =
                        parseLintResult(
                            file,
                            manager,
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

    private val PATTERN = Pattern.compile("L:\\s+\\d+\\s+\\|\\s+P:\\s+\\d+\\s+\\|\\s+L\\d+\\s+\\|\\s+\\D+\\.")

    fun parseLintResult(
        file: PsiFile,
        manager: InspectionManager,
        document: Document,
        line: String?
    ): ProblemDescriptor? {

        val matcher = PATTERN.matcher(line)
        if (!matcher.matches()) {
            return null
        }
        val fix: LocalQuickFix? = null
        return manager.createProblemDescriptor(
            file, "|", fix, ProblemHighlightType.ERROR, true
        )
    }

    abstract fun buildCommandLineArgs(
        python: String,
        lint: String,
        lintOptions: String,
        file: PsiFile
    ): List<String>
}