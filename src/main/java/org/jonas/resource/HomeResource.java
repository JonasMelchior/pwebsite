package org.jonas.resource;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jonas.service.ProjectService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Path("/")
public class HomeResource {

    private static final Logger LOG = Logger.getLogger(HomeResource.class);

    @Inject
    @Location("index.html")
    Template indexTemplate;

    @Inject
    ProjectService projectService;

    @ConfigProperty(name = "widget.url", defaultValue = "http://localhost:8080")
    String widgetUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return indexTemplate
                .data("projects", projectService.getAllProjects())
                .data("widgetUrl", widgetUrl);
    }

    @GET
    @Path("/api/activity-heatmap")
    @Produces(MediaType.TEXT_HTML)
    public Response proxyActivityHeatmap(@QueryParam("range") @DefaultValue("3m") String range) {
        String targetUrl = widgetUrl.replaceAll("/+$", "") + "/api/activity-heatmap?range=" + range;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Accept", "text/html")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Response.status(response.statusCode())
                    .entity(response.body())
                    .type(MediaType.TEXT_HTML)
                    .build();
        } catch (Exception e) {
            LOG.errorf("Error forwarding request to widget backend at %s: %s", targetUrl, e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("<div class='text-xs text-neutral-500 py-6 text-center border border-neutral-200 rounded-xl bg-neutral-50'>"
                            + "Unable to connect to PR view widget service at <code>" + widgetUrl + "</code>. "
                            + "Ensure the <code>prview</code> backend is running."
                            + "</div>")
                    .type(MediaType.TEXT_HTML)
                    .build();
        }
    }
}
