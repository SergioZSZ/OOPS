package es.upm.fi.oeg.oops.service.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import es.upm.fi.oeg.oops.rabbitmq.OopsPlusJob;
import es.upm.fi.oeg.oops.rabbitmq.OopsPlusJobPublisher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class OopsPlusSubmissionService {

    private static final Path DEFAULT_ANALYSES_DIR = Path.of("/data/oops/analyses");
    private static final String ONTOLOGY_FILE_NAME = "ontology.rdf";
    private static final String STATUS_FILE_NAME = "status.json";
    private static final DateTimeFormatter ANALYSIS_ID_TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Path analysesDir;

    public OopsPlusSubmissionService() {
        this(DEFAULT_ANALYSES_DIR);
    }

    public OopsPlusSubmissionService(final Path analysesDir) {
        this.analysesDir = analysesDir;
    }

    public OopsPlusSubmission submit(final String ontologyContent) throws IOException {
        return submit(ontologyContent, false);
    }

    public OopsPlusSubmission submit(final String ontologyContent, final boolean removeOntology) throws IOException {
        if (ontologyContent == null || ontologyContent.isBlank()) {
            throw new IllegalArgumentException("Ontology content cannot be empty");
        }

        final Instant requestedAt = Instant.now();
        final String analysisId = ANALYSIS_ID_TIMESTAMP_FORMAT.format(requestedAt) + "-" + UUID.randomUUID().toString();
        final Path analysisDir = analysesDir.resolve(analysisId);
        final Path ontologyPath = analysisDir.resolve(ONTOLOGY_FILE_NAME);
        final Path statusPath = analysisDir.resolve(STATUS_FILE_NAME);

        Files.createDirectories(analysisDir);
        Files.writeString(ontologyPath, ontologyContent, StandardCharsets.UTF_8);
        writeQueuedStatus(statusPath, analysisId, ontologyPath, requestedAt, removeOntology);

        final OopsPlusJob job = new OopsPlusJob(analysisId, ontologyPath.toString(), requestedAt.toString(),
                removeOntology);
        publishWithRabbitMq(job);

        return new OopsPlusSubmission(analysisId, ontologyPath, statusPath);
    }

    private static void publishWithRabbitMq(final OopsPlusJob job) throws IOException {
        try (OopsPlusJobPublisher publisher = new OopsPlusJobPublisher()) {
            publisher.publish(job);
        } catch (final TimeoutException exc) {
            throw new IOException("Could not publish OOPS+ job", exc);
        }
    }

    private static void writeQueuedStatus(final Path statusPath, final String analysisId, final Path ontologyPath,
            final Instant requestedAt, final boolean removeOntology) throws IOException {
        final Map<String, Object> status = new LinkedHashMap<>();
        status.put("analysisId", analysisId);
        status.put("status", "QUEUED");
        status.put("ontologyPath", ontologyPath.toString());
        status.put("requestedAt", requestedAt.toString());
        status.put("removeOntology", removeOntology);

        OBJECT_MAPPER.writeValue(statusPath.toFile(), status);
    }

}
