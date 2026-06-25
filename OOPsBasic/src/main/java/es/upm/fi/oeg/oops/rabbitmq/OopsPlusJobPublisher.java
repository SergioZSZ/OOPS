package es.upm.fi.oeg.oops.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class OopsPlusJobPublisher implements AutoCloseable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final AMQP.BasicProperties JSON_PERSISTENT_PROPERTIES = new AMQP.BasicProperties.Builder()
            .contentType("application/json").deliveryMode(2).build();

    private final Connection connection;
    private final Channel channel;
    private final String queueName;

    public OopsPlusJobPublisher() throws IOException, TimeoutException {
        this(RabbitMqConfig.createConnectionFactory(), RabbitMqConfig.getOopsPlusJobsQueue());
    }

    public OopsPlusJobPublisher(final ConnectionFactory connectionFactory, final String queueName)
            throws IOException, TimeoutException {
        this.connection = connectionFactory.newConnection();
        this.channel = connection.createChannel();
        this.queueName = queueName;
        declareQueue();
    }

    public void publish(final OopsPlusJob job) throws IOException {
        channel.basicPublish("", queueName, JSON_PERSISTENT_PROPERTIES, toJson(job));
    }

    static byte[] toJson(final OopsPlusJob job) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(job);
    }

    private void declareQueue() throws IOException {
        final boolean durable = true;
        final boolean exclusive = false;
        final boolean autoDelete = false;
        channel.queueDeclare(queueName, durable, exclusive, autoDelete, null);
    }

    @Override
    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
