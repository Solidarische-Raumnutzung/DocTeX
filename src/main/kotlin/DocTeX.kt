package de.mr_pine.doctex

import de.mr_pine.doctex.spoon.DocTeXLauncher
import de.mr_pine.doctex.spoon.LaTeXGenerator
import spoon.OutputType
import spoon.reflect.CtModel
import spoon.reflect.declaration.CtPackage
import spoon.support.compiler.FileSystemFolder
import java.io.File

class DocTeX(private val sourcedir: File, private val classpath: File?) {
    private val launcher = DocTeXLauncher().apply {
        environment.apply {
            setShouldCompile(false)
            disableConsistencyChecks()
            setCommentEnabled(true)
            complianceLevel = 19
            outputType = OutputType.NO_OUTPUT
            if (classpath == null) {
                noClasspath = true
            } else {
                sourceClasspath =
                    arrayOf(classpath.path)
            }
        }
        addInputResource(FileSystemFolder(sourcedir))
    }
    private val model: CtModel = launcher.buildModel()

    fun writeJavadoc(to: File, forPackage: String, minimumVisibility: Visibility, inheritDoc: Boolean) {
        val templateText = DocTeX::class.java.getResource("/template.tex")!!.readText()
        val rootPackage = model.allPackages.find { it.qualifiedName == forPackage }
            ?: throw Exception("Invalid root package provided. Available packages: ${model.allPackages}")
        val packagesDocumentation =
            rootPackage.resolveAllPackages().map { it.generateDocs(rootPackage, minimumVisibility, inheritDoc) }

        val documentation = templateText.replace("TEXT", packagesDocumentation.joinToString("\n".repeat(3)))
        to.writeText(documentation)
    }

    private fun CtPackage.resolveAllPackages(): List<CtPackage> =
        packages.flatMap { listOf(it) + it.resolveAllPackages() }
}

private fun CtPackage.generateDocs(rootPackage: CtPackage, minimumVisibility: Visibility, inheritDoc: Boolean): String {
    val generator = LaTeXGenerator(rootPackage, minimumVisibility, inheritDoc)
    accept(generator)
    return generator.generate()
}
