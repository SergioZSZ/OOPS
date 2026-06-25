package es.upm.fi.oeg.oops.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitMqConfig {

    /* Queue for complete OOPS+ report jobs */
    private static final String OOPS_PLUS_JOBS_QUEUE = env("RABBITMQ_OOPS_PLUS_JOBS_QUEUE", "oops.plus.reports");

    /* broker host */
    private static final String HOST = env("RABBITMQ_HOST", "localhost");

    /* AMQP port */
    private static final int PORT = intEnv("RABBITMQ_PORT", 5672);

    /* RabbitMQ username */
    private static final String USERNAME = env("RABBITMQ_USER", "oops");

    /* RabbitMQ password */
    private static final String PASSWORD = env("RABBITMQ_PASSWORD", "oops");

    /* RabbitMQ virtual host */
    private static final String VIRTUAL_HOST = "/";

    /* connection timeout in milliseconds */
    private static final int CONNECTION_TIMEOUT_MS = 30000;
    /* connection heartbeat */
    private static final int REQUESTED_HEARTBEAT_SECONDS = 30;

    private RabbitMqConfig() {
    }

    public static ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        // queues logic space
        factory.setVirtualHost(VIRTUAL_HOST);

        // auto reconect
        factory.setAutomaticRecoveryEnabled(true);
        factory.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        factory.setRequestedHeartbeat(REQUESTED_HEARTBEAT_SECONDS);

        return factory;
    }

    public static String getOopsPlusJobsQueue() {
        return OOPS_PLUS_JOBS_QUEUE;
    }

    private static String env(final String name, final String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static int intEnv(final String name, final int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
