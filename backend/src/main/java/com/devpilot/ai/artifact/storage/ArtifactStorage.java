package com.devpilot.ai.artifact.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface ArtifactStorage {
    String store(InputStream input, String key);
    InputStream retrieve(String key);
    void delete(String key);
    boolean exists(String key);
    Path getAbsolutePath(String key);
}
