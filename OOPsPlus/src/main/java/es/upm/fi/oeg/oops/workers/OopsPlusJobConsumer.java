package es.upm.fi.oeg.oops.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import es.upm.fi.oeg.oops.Checker;
import es.upm.fi.oeg.oops.CheckersCatalogue;
import es.upm.fi.oeg.oops.Linter;
import es.upm.fi.oeg.oops.ModelLoader;
import es.upm.fi.oeg.oops.Report;
import es.upm.fi.oeg.oops.SrcModel;
import es.upm.fi.oeg.oops.SrcSpec;
import es.upm.fi.oeg.oops.SrcType;
import es.upm.fi.oeg.oops.rabbitmq.OopsPlusJob;
import es.upm.fi.oeg.oops.rabbitmq.RabbitMqConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OopsPlusJobConsumer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final int PREFETCH_ONE_JOB = 1;
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private final OopsPlusReportWriter reportWriter = new OopsPlusReportWriter();

    public static void main(final String[] args) throws InterruptedException {
        new OopsPlusJobConsumer().start();
    }

    public void start() throws InterruptedException {
        while (true) {
            try {
                consumeJobs();
            } catch (final IOException | TimeoutException exc) {
                System.err.println("[OOPS+ worker] RabbitMQ connection failed: " + exc.getMessage());
                System.err.println("[OOPS+ worker] Retrying in " + RECONNECT_DELAY_SECONDS + " seconds...");
                TimeUnit.SECONDS.sleep(RECONNECT_DELAY_SECONDS);
            }
        }
    }

    private void consumeJobs() throws IOException, TimeoutException, InterruptedException {
        final ConnectionFactory connectionFactory = RabbitMqConfig.createConnectionFactory();
        final String queueName = RabbitMqConfig.getOopsPlusJobsQueue();

        final Connection connection = connectionFactory.newConnection("oops-plus-job-consumer");
        final Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, true, false, false, null);
        channel.basicQos(PREFETCH_ONE_JOB);

        System.out.println("[OOPS+ worker] Waiting for jobs in queue: " + queueName);

        final DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            final long deliveryTag = delivery.getEnvelope().getDeliveryTag();
            try {
                final OopsPlusJob job = parseJob(delivery.getBody());
                handleJob(job);
                channel.basicAck(deliveryTag, false);
            } catch (final Exception exc) {
                System.err.println("[OOPS+ worker] Failed to receive job: " + exc.getMessage());
                channel.basicNack(deliveryTag, false, true);
            }
        };

        final CancelCallback cancelCallback = consumerTag -> System.err
                .println("[OOPS+ worker] Consumer cancelled: " + consumerTag);

        channel.basicConsume(queueName, false, deliverCallback, cancelCallback);

        new CountDownLatch(1).await();
    }

    private OopsPlusJob parseJob(final byte[] body) throws IOException {
        final String json = new String(body, StandardCharsets.UTF_8);
        System.out.println("[OOPS+ worker] Raw job received: " + json);
        return OBJECT_MAPPER.readValue(json, OopsPlusJob.class);
    }

    private void handleJob(final OopsPlusJob job) throws IOException {
        final Path ontologyPath = Path.of(job.getOntologyPath());
        final Path analysisDir = ontologyPath.getParent();
        final Path statusPath = analysisDir.resolve("status.json");

        System.out.println("[OOPS+ worker] Job received at " + Instant.now());
        System.out.println("               analysisId=" + job.getAnalysisId());
        System.out.println("               ontologyPath=" + job.getOntologyPath());
        System.out.println("               removeOntology=" + job.isRemoveOntology());
        System.out.println("               requestedAt=" + job.getRequestedAt() + "\n\n");

        changeStatus(statusPath, OopsPlusJobStatus.PROCESSING);

        try {

            final String rdf = Files.readString(ontologyPath, StandardCharsets.UTF_8);

            final SrcSpec srcSpec = new SrcSpec(SrcType.RDF_CODE, null, rdf, null);
            final SrcModel srcModel = ModelLoader.load(srcSpec);

            final Linter linter = new Linter();
            final List<Checker> allCheckers = CheckersCatalogue.getAllCheckers();

            final Report report = linter.partialExecution(srcModel, null, allCheckers);

            final Path reportHtmlPath = analysisDir.resolve("report.html");

            reportWriter.writeHtml(reportHtmlPath, report);

            changeStatus(statusPath, OopsPlusJobStatus.COMPLETED, Map.of("reportHtmlPath", reportHtmlPath.toString()));
        } catch (final Exception exc) {
            changeStatus(statusPath, OopsPlusJobStatus.FAILED,
                    Map.of("error", exc.getMessage() == null ? exc.getClass().getName() : exc.getMessage()));
            System.err.println("[OOPS+ worker] Job failed for analysisId=" + job.getAnalysisId());
            exc.printStackTrace(System.err);
        } finally {
            removeOntologyIfRequested(job, ontologyPath, statusPath);
        }
    }

    public void changeStatus(final Path statusPath, final OopsPlusJobStatus status) throws IOException {
        changeStatus(statusPath, status, Map.of());
    }

    public void changeStatus(final Path statusPath, final OopsPlusJobStatus status,
            final Map<String, Object> extraFields) throws IOException {
        final Map<String, Object> statusContent = readStatusFile(statusPath);
        statusContent.put("status", status.name());
        statusContent.putAll(extraFields);
        OBJECT_MAPPER.writeValue(statusPath.toFile(), statusContent);
    }

    private Map<String, Object> readStatusFile(final Path statusPath) throws IOException {
        if (!Files.exists(statusPath)) {
            return new LinkedHashMap<>();
        }
        return OBJECT_MAPPER.readValue(statusPath.toFile(), LinkedHashMap.class);
    }

    private void removeOntologyIfRequested(final OopsPlusJob job, final Path ontologyPath, final Path statusPath) {
        if (!job.isRemoveOntology()) {
            return;
        }

        try {
            final boolean removed = Files.deleteIfExists(ontologyPath);
            updateStatusFields(statusPath, Map.of("ontologyRemoved", removed));
            System.out.println("[OOPS+ worker] Ontology cleanup requested. Removed=" + removed);
        } catch (final IOException exc) {
            System.err.println("[OOPS+ worker] Could not remove ontology: " + exc.getMessage());
            try {
                updateStatusFields(statusPath, Map.of("ontologyRemoved", false, "ontologyRemovalError",
                        exc.getMessage() == null ? exc.getClass().getName() : exc.getMessage()));
            } catch (final IOException statusExc) {
                System.err.println("[OOPS+ worker] Could not write ontology cleanup status: " + statusExc.getMessage());
            }
        }
    }

    private void updateStatusFields(final Path statusPath, final Map<String, Object> fields) throws IOException {
        final Map<String, Object> statusContent = readStatusFile(statusPath);
        statusContent.putAll(fields);
        OBJECT_MAPPER.writeValue(statusPath.toFile(), statusContent);
    }

    public enum OopsPlusJobStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED
    }
}
