package org.jonas.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProjectDocument {
    public String filename;
    public String title;
    public String type; // "report", "presentation", "document"
    public String url;
    public long sizeBytes;

    public ProjectDocument() {}

    public ProjectDocument(String filename, String title, String type, String url, long sizeBytes) {
        this.filename = filename;
        this.title = title;
        this.type = type;
        this.url = url;
        this.sizeBytes = sizeBytes;
    }

    public String getFormattedSize() {
        if (sizeBytes <= 0) return "";
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
