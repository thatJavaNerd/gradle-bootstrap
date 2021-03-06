package net.dean.gbs.api.test

import net.dean.gbs.api.io.ProjectRenderer
import net.dean.gbs.api.io.RenderReport
import net.dean.gbs.api.io.ZipHelper
import net.dean.gbs.api.models.*
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.Assert.assertEquals
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.collections.setOf
import kotlin.properties.Delegates
import org.junit.Test as test

public class CreationTest {
    private val processLogger = ProcessOutputAdapter()
    private var renderer: ProjectRenderer by Delegates.notNull()
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val upstreamUrl = "https://github.com/example/example"

    public @test fun testSignificantPermutations() {
        // Testing framework: NONE vs TESTNG vs <any other>
        // TestNG tests require a useTestNG() call in the buildscript, while the others don't
        val testingOptions = arrayOf(TestingFramework.NONE, TestingFramework.TESTNG, TestingFramework.JUNIT)
        // Logging framework: NONE vs <SLF4J or LOG4J> vs <any other>
        // SLF4J and Log4J both require multiple dependencies, while the others don't
        val loggingOptions = arrayOf(LoggingFramework.NONE, LoggingFramework.SLF4J, LoggingFramework.LOGBACK_CLASSIC)
        // Languages: JAVA vs <any other> vs KOTLIN
        // Kotlin requires a custom meta-dependency (plugin) and a compile-time dependency, while the others require
        // a built-in plugin and a compile-time dependency, except for Java, which only requires its built-in plugin
        val languageOptions = arrayOf(Language.JAVA, Language.GROOVY, Language.KOTLIN)
        val projectAmount = testingOptions.size * loggingOptions.size * languageOptions.size
        log.info("Testing $projectAmount unique projects")

        var counter = 0
        for (testing in testingOptions)
            for (logging in loggingOptions)
                for (lang in languageOptions) {
                    val id = "$testing-$logging-$lang"
                    log.info("Evaluating project #${++counter}: (testing=$testing,logging=$logging," +
                            "lang=$lang)")
                    val (proj, path) = newProject("permutation-$counter-$id", lang = lang)
                    proj.license = License.NONE
                    proj.build.testing = testing
                    proj.build.logging = logging
                    validateProject(proj, path)
                }
    }

    /**
     * Runs a full suite of validation tests, including validating the report from ProjectRenderer.render(), creating a
     * zip archive of the directory structure, and making sure a 'gradle build' succeeds for the given project
     */
    private fun validateProject(proj: Project, root: File, zip: Boolean = false) {
        validateProjetExportReport(ProjectRenderer(root).render(proj))
        // Testing zip functionality is only required for one project. Testing multiple will only serve to make the test longer
        if (zip)
            testZip(proj, root)
        validateGradleBuild(root)

        if (proj.gitInit)
            validateGit(root)
    }

    private fun validateGit(rootPath: File) {
        val repo = FileRepositoryBuilder.create(File(rootPath, ".git"))
        val config = repo.config
        assertEquals(upstreamUrl, config.getString("remote", "origin", "url"))
    }

    /**
     * Executes "gradle build" in a given directory and asserts that the exit code of that process is equal to 0.
     */
    private fun validateGradleBuild(rootPath: File) {
        val command = arrayOf("gradle", "build", "--no-daemon")
        val process = ProcessBuilder()
                .directory(rootPath)
                .command(*command)
                .start()
        processLogger.attach(process, rootPath.name.toString())

        val exitCode = process.waitFor()
        // An exit code of 0 means the process completed without error
        assertEquals(0, exitCode)
    }

    /**
     * Makes sure every directory and file in the report exist and are either a directory or a file respectively
     */
    fun validateProjetExportReport(report: RenderReport) {
        for (dir in report.directories) {
            assert(dir.isDirectory) { "Path $dir claimed to be a directory but was not" }
        }
        for (file in report.files) {
            assert(file.isFile) { "Path $file claimed to be a file but was not" }
        }
    }

    /**
     * Returns a Pair of a Project to its root Pat based on the calling method. The group will be
     * "com.example.$name" and its base path will be "build/projects/$name
     */
    private fun newProject(name: String, lang: Language): Pair<Project, File> {
        val path = File("build/projects/normal/$name")
        val proj = Project(name, "com.example.$name", "0.1", gitRepo = upstreamUrl, languages = setOf(lang))
        // Delete the files before generating it so that if we want to examine the crated files after creation, we can.
        path.delete()
        return proj to path
    }

    /**
     * Makes sure that a zip file is created for the given project in the given path
     */
    private fun testZip(proj: Project, basePath: File) {
        val zipFile = File(basePath, "../../zipped/${proj.name}.zip")
        // Clean up from last time
        zipFile.delete()

        // Make sure the directories exist first
        FileUtils.forceMkdir(zipFile.parentFile)
        ZipHelper.createZip(basePath, zipFile)
        assert(zipFile.isFile) { "Output file does not exist" }
    }
}
