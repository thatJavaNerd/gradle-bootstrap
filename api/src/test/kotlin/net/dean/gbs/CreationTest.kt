package net.dean.gbs

import org.testng.annotations.Test as test
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.io.IOException
import org.testng.Assert

public class CreationTest {
    public test fun basicCreate() {
        val (proj, path) = newProject()
        proj.license = License.APACHE
        proj.add(Language.JAVA)
        proj.build.logging = LoggingFramework.SLF4J
        proj.build.testing = TestingFramework.TESTNG
        proj.build.projectContext.add(Repository.MAVEN_CENTRAL)
        // Random plugin
        proj.build.addGradlePlugin(Dependency("net.swisstech", "gradle-dropwizard", scope = Scope.CLASSPATH))
        proj.build.plugins.add("application")

        // Render and export the project
        Exporter().export(proj, path, ProjectRenderer().render(proj))
        validateGradleBuild(path)
    }

    /**
     * Executes "gradle build" in a given directory and asserts that the exit code of that process is equal to 0.
     */
    private fun validateGradleBuild(rootPath: Path) {
        val command = array("gradle", "build")
        val dir = rootPath.toFile()
        println("Executing command '${command.join(" ")}' in directory '${dir.getAbsolutePath()}'")
        val process = ProcessBuilder()
                .directory(dir)
                .command(*command)
                .inheritIO()
                .start()
        // Should have an exit value of 0
        val exitCode = process.waitFor()
        println("Finished")
        Assert.assertEquals(exitCode, 0)
    }

    /**
     * Returns a Pair of a Project to its root Pat based on the calling method. The group will be
     * "com.example.$callingMethodName" and its base path will be "build/projects/$callingMethodName".
     *
     * stackOffset is the amount of indicies to increase the base count by. A stackOffset of 0 would result in the
     * method that directly called this method, 1 would be the method that called the other method, etc.
     */
    private fun newProject(stackOffset: Int = 0): Pair<Project, Path> {
        val name = Thread.currentThread().getStackTrace()[2 + stackOffset].getMethodName()
        val path = Paths.get("build/projects/$name")
        val proj = Project(name, "com.example.$name")
        // Delete the files before generating it so that if we want to examine the crated files after creation, we can.
        delete(path)
        return proj to path
    }

    private fun delete(path: Path) {
        if (!Files.exists(path))
            return
        if (Files.isDirectory(path)) {
            // Recursively delete file tree
            Files.walkFileTree(path, object: SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            })
        } else {
            // Delete file
            Files.delete(path)
        }
    }
}
