package org.jonas.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public class Project {
    public String slug;
    public String title;
    public String description;
    public String tag;
    public String date;
    public String githubUrl;
    public String liveUrl;
    public String youtubeUrl;
    public String youtubeEmbedUrl;
    public String contentHtml;
    public List<ProjectDocument> documents = new ArrayList<>();

    public Project() {}

    public Project(String slug, String title, String description, String tag, String date, String githubUrl, String liveUrl, String youtubeUrl, String youtubeEmbedUrl, String contentHtml) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.tag = tag;
        this.date = date;
        this.githubUrl = githubUrl;
        this.liveUrl = liveUrl;
        this.youtubeUrl = youtubeUrl;
        this.youtubeEmbedUrl = youtubeEmbedUrl;
        this.contentHtml = contentHtml;
    }
}
