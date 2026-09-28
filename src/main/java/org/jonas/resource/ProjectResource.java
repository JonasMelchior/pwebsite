package org.jonas.resource;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jonas.model.Project;
import org.jonas.model.ProjectDocument;
import org.jonas.service.ProjectService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;

@jakarta.ws.rs.Path("/")
public class ProjectResource {

    @Inject
    ProjectService projectService;

    @Inject
    @Location("project-detail.html")
    Template projectDetailTemplate;

    @GET
    @jakarta.ws.rs.Path("/projects/{slug}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getProjectDetail(@PathParam("slug") String slug) {
        Project project = projectService.getProjectBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Project summary not found: " + slug));

        return projectDetailTemplate.data("project", project);
    }

    @GET
    @jakarta.ws.rs.Path("/projects/{slug}/documents/{filename}")
    @Produces("application/pdf")
    public Response getProjectDocument(
            @PathParam("slug") String slug,
            @PathParam("filename") String filename) throws IOException {

        java.nio.file.Path docPath = projectService.getDocumentPath(slug, filename)
                .orElseThrow(() -> new NotFoundException("Document not found: " + filename));

        byte[] pdfBytes = Files.readAllBytes(docPath);

        return Response.ok(pdfBytes)
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .header("Content-Type", "application/pdf")
                .build();
    }

    @GET
    @jakarta.ws.rs.Path("/projects/{slug}/assets/{filename}")
    public Response getProjectAsset(
            @PathParam("slug") String slug,
            @PathParam("filename") String filename) throws IOException {

        java.nio.file.Path assetPath = projectService.getDocumentPath(slug, filename)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + filename));

        String probeType = Files.probeContentType(assetPath);
        String contentType = probeType != null ? probeType : "application/octet-stream";

        byte[] bytes = Files.readAllBytes(assetPath);

        return Response.ok(bytes)
                .header("Content-Type", contentType)
                .header("Cache-Control", "public, max-age=86400")
                .build();
    }
}
