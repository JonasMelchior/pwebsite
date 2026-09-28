package org.jonas.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jboss.logging.Logger;
import org.jonas.model.Project;
import org.jonas.model.ProjectDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class ProjectService {

    private static final Logger LOG = Logger.getLogger(ProjectService.class);
    private final Path projectsDir = Path.of("content/projects");
    private final Parser parser;
    private final HtmlRenderer renderer;

    public ProjectService() {
        List<Extension> extensions = List.of(YamlFrontMatterExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(projectsDir)) {
                Files.createDirectories(projectsDir);
            }
        } catch (IOException e) {
            LOG.error("Failed to create content/projects directory", e);
        }
    }

    public List<Project> getAllProjects() {
        ensureDirectoryExists();
        try (Stream<Path> stream = Files.list(projectsDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(this::parseProjectDirectory)
                    .filter(Objects::nonNull)
                    .sorted((p1, p2) -> {
                        if (p1.date != null && p2.date != null) {
                            return p2.date.compareTo(p1.date);
                        }
                        return p1.title != null ? p1.title.compareToIgnoreCase(p2.title != null ? p2.title : "") : 0;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error("Failed to list project folders", e);
            return Collections.emptyList();
        }
    }

    public Optional<Project> getProjectBySlug(String slug) {
        ensureDirectoryExists();
        Path projectFolder = projectsDir.resolve(slug);
        if (!Files.exists(projectFolder) || !Files.isDirectory(projectFolder)) {
            return Optional.empty();
        }
        return Optional.ofNullable(parseProjectDirectory(projectFolder));
    }

    public Optional<Path> getDocumentPath(String slug, String filename) {
        Path projectDocDir = projectsDir.resolve(slug);
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "");
        Path file = projectDocDir.resolve(safeFilename);
        if (Files.exists(file) && Files.isRegularFile(file)) {
            return Optional.of(file);
        }
        return Optional.empty();
    }

    private Project parseProjectDirectory(Path projectDir) {
        try {
            String slug = projectDir.getFileName().toString();
            // Find main markdown file: project.md, index.md, or <slug>.md, or any .md in folder
            Path mdFile = findMainMarkdownFile(projectDir, slug);
            if (mdFile == null || !Files.exists(mdFile)) {
                return null;
            }

            String rawContent = Files.readString(mdFile);
            String preprocessedContent = preprocessMarkdownContent(rawContent, slug);
            Node document = parser.parse(preprocessedContent);

            YamlFrontMatterVisitor visitor = new YamlFrontMatterVisitor();
            document.accept(visitor);
            Map<String, List<String>> metadata = visitor.getData();

            String title = getFirst(metadata, "title", slug);
            String description = getFirst(metadata, "description", "");
            String tag = getFirst(metadata, "tag", "Project");
            String date = getFirst(metadata, "date", "");
            String github = getFirst(metadata, "github", "");
            String liveUrl = getFirst(metadata, "live", "");
            String youtube = getFirst(metadata, "youtube", "");
            if (youtube.isBlank()) {
                youtube = getFirst(metadata, "video", "");
            }
            String youtubeEmbed = formatYoutubeEmbedUrl(youtube);

            String html = renderer.render(document);

            Project project = new Project(slug, title, description, tag, date, github, liveUrl, youtube, youtubeEmbed, html);
            project.documents = findDocumentsForProject(projectDir, slug);

            return project;
        } catch (Exception e) {
            LOG.errorf("Error parsing project directory %s: %s", projectDir, e.getMessage());
            return null;
        }
    }

    private Path findMainMarkdownFile(Path projectDir, String slug) {
        Path direct = projectDir.resolve("project.md");
        if (Files.exists(direct)) return direct;
        
        Path index = projectDir.resolve("index.md");
        if (Files.exists(index)) return index;

        Path slugNamed = projectDir.resolve(slug + ".md");
        if (Files.exists(slugNamed)) return slugNamed;

        try (Stream<Path> stream = Files.list(projectDir)) {
            return stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".md"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private List<ProjectDocument> findDocumentsForProject(Path projectDir, String slug) {
        try (Stream<Path> stream = Files.list(projectDir)) {
            return stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .map(p -> {
                        String filename = p.getFileName().toString();
                        long size = 0;
                        try {
                            size = Files.size(p);
                        } catch (IOException ignored) {}
                        String title = determineDocTitle(filename);
                        String type = determineDocType(filename);
                        String url = "/projects/" + slug + "/documents/" + filename;
                        return new ProjectDocument(filename, title, type, url, size);
                    })
                    .sorted(Comparator.comparing(d -> d.title))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.errorf("Error listing documents for project %s: %s", slug, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String determineDocTitle(String filename) {
        // Strip resource_ or resource- prefix and .pdf extension
        String clean = filename.replaceFirst("(?i)^resource[_-]", "")
                               .replaceFirst("(?i)\\.pdf$", "")
                               .replace("-", " ")
                               .replace("_", " ");
        String[] words = clean.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String determineDocType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.contains("pres") || lower.contains("slide") || lower.contains("deck") || lower.contains("pitch")) {
            return "Presentation";
        }
        if (lower.contains("report") || lower.contains("paper") || lower.contains("spec") || lower.contains("doc")) {
            return "Report";
        }
        return "PDF Document";
    }

    private String formatYoutubeEmbedUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        // Extract video ID from youtube.com/watch?v=ID, youtu.be/ID, or youtube.com/embed/ID
        try {
            if (trimmed.contains("youtube.com/embed/")) {
                return trimmed;
            }
            if (trimmed.contains("youtu.be/")) {
                String id = trimmed.substring(trimmed.lastIndexOf("youtu.be/") + 9);
                int qIdx = id.indexOf("?");
                if (qIdx != -1) id = id.substring(0, qIdx);
                return "https://www.youtube.com/embed/" + id;
            }
            if (trimmed.contains("watch?v=")) {
                String id = trimmed.substring(trimmed.indexOf("watch?v=") + 8);
                int ampIdx = id.indexOf("&");
                if (ampIdx != -1) id = id.substring(0, ampIdx);
                return "https://www.youtube.com/embed/" + id;
            }
        } catch (Exception ignored) {}
        return trimmed;
    }

    private String preprocessMarkdownContent(String markdown, String slug) {
        if (markdown == null) return "";

        // 1. Transform Obsidian Wiki-links ![[image.png|width]] or ![[image.png]]
        // Example: ![[sys_arch.png|423]] -> <img src="/projects/{slug}/assets/sys_arch.png" style="max-width: 423px; width: 100%;" class="rounded-xl border border-neutral-200 my-4" alt="sys_arch.png" />
        // Example: ![[sys_arch.png]] -> <img src="/projects/{slug}/assets/sys_arch.png" class="rounded-xl border border-neutral-200 my-4" alt="sys_arch.png" />
        java.util.regex.Pattern wikiPattern = java.util.regex.Pattern.compile("!\\[\\[([^|\\]]+)(?:\\|([^\\]]+))?\\]\\]");
        java.util.regex.Matcher wikiMatcher = wikiPattern.matcher(markdown);
        StringBuilder sb = new StringBuilder();
        while (wikiMatcher.find()) {
            String filename = wikiMatcher.group(1).trim();
            String width = wikiMatcher.group(2) != null ? wikiMatcher.group(2).trim() : null;
            String assetUrl = "/projects/" + slug + "/assets/" + filename;
            
            String replacement;
            if (width != null && !width.isBlank()) {
                String cleanWidth = width.replaceAll("[^0-9]", "");
                replacement = String.format("<img src=\"%s\" alt=\"%s\" style=\"max-width: %spx; width: 100%%;\" class=\"rounded-xl border border-neutral-200 my-4\" />", assetUrl, filename, cleanWidth);
            } else {
                replacement = String.format("<img src=\"%s\" alt=\"%s\" class=\"rounded-xl border border-neutral-200 my-4 max-w-full\" />", assetUrl, filename);
            }
            wikiMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        wikiMatcher.appendTail(sb);
        String processed = sb.toString();

        // 2. Transform relative markdown image links ![alt](image.png) or ![alt](./image.png) -> ![alt](/projects/{slug}/assets/image.png)
        processed = processed.replaceAll("!\\[([^\\]]*)\\]\\((?!https?://|/)(?:\\./)?([^)]+)\\)", "![$1](/projects/" + slug + "/assets/$2)");

        return processed;
    }

    private String getFirst(Map<String, List<String>> map, String key, String fallback) {
        List<String> list = map.get(key);
        if (list != null && !list.isEmpty() && list.get(0) != null) {
            return list.get(0).trim();
        }
        return fallback;
    }
}
