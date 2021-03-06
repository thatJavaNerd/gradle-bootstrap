package net.dean.gbs.api.io

import net.dean.gbs.api.models.License
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * Retrieves a normalized path relative to the given path.
 *
 *    val path = Paths.get("/home/user")
 *    relativePath(path, "subpathA", "subpathB") // => /home/user/subpathA/subpathB
 *    relativePath(path, "../other_user") // => /home/other_user
 */
public fun relativePath(base: Path, other: String, vararg others: String): Path {
    return Paths.get(base.normalize().toString(), other, *others).normalize()
}

/**
 * Deletes the given path. Works on files and directories. Does nothing if the path doesn't exist.
 */
public fun delete(path: Path) {
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
                if (exc != null)
                    throw exc

                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    } else {
        // Delete file
        Files.delete(path)
    }
}

public fun mkdirs(dir: File) {
    FileUtils.forceMkdir(dir)
}

/**
 * Gets the path to the raw data for the license file.
 */
public fun licensePath(lic: License): File {
    return resource("/licenses/${lic.name}.txt")
}

public fun resource(path: String): File {
    val url = ProjectRenderer::class.java.getResource(path)
    try {
        return File(url.toURI())
    } catch (e: NullPointerException) {
        throw FileNotFoundException(path)
    }
}

