package com.devpilot.ai.artifact.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;

@Component
public class LocalArtifactStorage implements ArtifactStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalArtifactStorage.class);

    private final String baseDir;

    public LocalArtifactStorage(@Value("${devpilot.storage.artifacts-dir:${user.home}/.devpilot/artifacts}") String baseDir) {
        this.baseDir = baseDir;
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public String store(InputStream inputStream, String storageKey) {
        try {
            File targetFile = new File(baseDir, storageKey);
            targetFile.getParentFile().mkdirs();

            try (FileOutputStream out = new FileOutputStream(targetFile)) {
                inputStream.transferTo(out);
            }
            log.info("Stored artifact at: {}", targetFile.getAbsolutePath());
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store artifact: " + storageKey, e);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        try {
            File targetFile = new File(baseDir, storageKey);
            if (!targetFile.exists()) {
                throw new IllegalArgumentException("Artifact file does not exist: " + storageKey);
            }
            return new FileInputStream(targetFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load artifact: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        File targetFile = new File(baseDir, storageKey);
        if (targetFile.exists()) {
            targetFile.delete();
        }
    }

    @Override
    public boolean exists(String storageKey) {
        File targetFile = new File(baseDir, storageKey);
        return targetFile.exists();
    }

    @Override
    public Path getAbsolutePath(String storageKey) {
        return new File(baseDir, storageKey).toPath();
    }
}
