package com.git_crawler.backend.services.indexing;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class CodeFileFilter {

    private final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java",
            ".py",
            ".go",
            ".ts",
            ".tsx",
            ".js",
            ".jsx",
            ".css",
            ".html",
            ".xml",
            ".yml",
            ".yaml",
            ".json"
    );

    public boolean isAllowed(String filePath) {

        if (filePath == null || !filePath.contains(".")) {
            return false;
        }

        String extension = filePath
                .substring(filePath.lastIndexOf('.'))
                .toLowerCase();

        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            "node_modules",
            "target",
            "build",
            "dist",
            ".next",
            "vendor",
            "__pycache__",
            ".idea",
            "out",
            "bin",
            "obj"
    );

    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".png",
            ".jpg",
            ".jpeg",
            ".gif",
            ".ico",
            ".zip",
            ".jar",
            ".class",
            ".exe",
            ".dll",
            ".so"
    );

    private static final Set<String> SKIP_FILENAMES = Set.of(
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml",
            "Cargo.lock",
            "composer.lock",
            "Gemfile.lock"
    );

    private static final Set<String> INCLUDED_FILENAMES = Set.of(
            "Dockerfile",
            "Makefile",
            "Jenkinsfile"
    );

    public boolean isEligible(
            String path,
            long sizeBytes,
            long maxFileBytes
    ) {

        if (path == null || path.isBlank()) {
            return false;
        }

        // Ignore files which are too large
        if (sizeBytes > maxFileBytes) {
            return false;
        }

        String normalizedPath = path.replace("\\", "/");

        String fileName = normalizedPath.substring(
                normalizedPath.lastIndexOf("/") + 1
        );

        // Ignore specific files
        if (SKIP_FILENAMES.contains(fileName)) {
            return false;
        }

        // Ignore unwanted directories
        String[] parts = normalizedPath.split("/");

        for (String part : parts) {
            if (IGNORED_DIRECTORIES.contains(part)) {
                return false;
            }
        }

        // Allow useful extensionless files
        if (INCLUDED_FILENAMES.contains(fileName)) {
            return true;
        }

        String lowerFileName =
                fileName.toLowerCase(Locale.ROOT);

        // Ignore known binary/unwanted extensions
        for (String extension : IGNORED_EXTENSIONS) {
            if (lowerFileName.endsWith(extension)) {
                return false;
            }
        }

        // Allow supported source/config/documentation files
        for (String extension : ALLOWED_EXTENSIONS) {
            if (lowerFileName.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }
}