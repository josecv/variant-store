package org.phenotips.variantstore.shared;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.phenotips.variantstore.db.DatabaseException;

/**
 * Manage the resources for the application. Set them up, clean them up, the whole 9 yards.
 */
public class ResourceManager {
    private static Logger logger = Logger.getLogger(ResourceManager.class);

    /**
     * Copy resources bundled with the application to a specified folder.
     * @param source the path of the resources relative to the resource folder
     * @param dest the destination
     * @throws DatabaseException
     */
    public static void copyResourcesToPath(String source, Path dest) throws DatabaseException {
        copyResourcesToPath(Paths.get(source), dest);
    }

    /**
     * Copy resources bundled with the application to a specified folder.
     * @param source the path of the resources relative to the resource folder
     * @param dest the destination
     * @throws DatabaseException
     */
    public static void copyResourcesToPath(final Path source, Path dest) throws DatabaseException {
        // Check if storage dirs exists
        if (Files.isDirectory(dest)) {
            return;
        }

        // Make sure that we aren't double-nesting directories
        if (dest.endsWith(source)) {
            dest = dest.getParent();
        }

        // Get path to where the resources are stored
        Class clazz = ResourceManager.class;

        if (clazz.getProtectionDomain().getCodeSource() == null) {
            throw new DatabaseException("This is running in a jar loaded from the system class loader. Don't know how to handle this.");
        }

        Path resourcesPath = Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());

        try {

            // make destination folder
            Files.createDirectories(dest);

            // if we are running in a jar, get the resources from the jar
            if ("jar".equals(FilenameUtils.getExtension(resourcesPath.toString()))) {

                copyResourcesFromJar(resourcesPath, source, dest);

            // if running from an IDE or the filesystem, get the resources from the folder
            } else {

                copyResourcesFromFilesystem(resourcesPath.resolve(source), dest.resolve(source));

            }
        } catch (IOException e) {
            throw new DatabaseException("Error setting up variant store, unable to install resources.", e);
        }

    }

    /**
     * Copy resources recursively from a path on the filesystem to the destination folder
     * @param source
     * @param dest
     * @throws IOException
     */
    private static void copyResourcesFromFilesystem(final Path source, final Path dest) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Files.copy(file, dest.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Files.createDirectory(dest.resolve(relative));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Copy resources recursively from a folder specified by source in a jar file specified by jarPath to a destination folder on
     * the filesystem dest
     * @param jarPath the jar file
     * @param source the folder on the filesystem
     * @param dest the destination
     * @throws IOException
     */
    private static void copyResourcesFromJar(Path jarPath, Path source, Path dest) throws IOException {
        JarFile jar = new JarFile(jarPath.toFile());

        for (JarEntry entry : Collections.list(jar.entries())) {

            if (entry.getName().startsWith(source.toString())) {
                if (entry.isDirectory()) {
                    Files.createDirectory(dest.resolve(entry.getName()));
                } else {
                    Files.copy(jar.getInputStream(entry), dest.resolve(entry.getName()));
                }
            }

        }
    }

    /**
     * Delete the directory and any files in the path provided
     * @param path
     * @throws DatabaseException
     */
    public static void clearResources(Path path) throws DatabaseException {
        try {
            FileUtils.deleteDirectory(path.toFile());
        } catch (IOException e) {
            throw new DatabaseException("Error clearing resources", e);
        }
    }
}
