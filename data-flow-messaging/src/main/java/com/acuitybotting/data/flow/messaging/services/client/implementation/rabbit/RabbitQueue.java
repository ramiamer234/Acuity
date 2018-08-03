package com.acuitybotting.data.flow.messaging.services.client.implementation.rabbit;

import com.acuitybotting.data.flow.messaging.services.Message;
import com.acuitybotting.data.flow.messaging.services.client.MessagingChannel;
import com.acuitybotting.data.flow.messaging.services.client.MessagingClient;
import com.acuitybotting.data.flow.messaging.services.client.MessagingQueue;
import com.acuitybotting.data.flow.messaging.services.client.exceptions.MessagingException;
import com.acuitybotting.data.flow.messaging.services.client.listeners.MessagingQueueListener;
import com.acuitybotting.data.flow.messaging.services.events.MessageEvent;
import com.acuitybotting.data.flow.messaging.services.futures.MessageFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Zachary Herridge on 7/26/2018.
 */
public class RabbitQueue implements MessagingQueue {

    private RabbitClient client;
    private RabbitChannel channel;

    private String queueName;
    private boolean createQueue;
    private boolean autoAcknowledge;

    private Set<RabbitQueueBinding> bindings = new HashSet<>();
    private AtomicReference<String> consumeId = new AtomicReference<>();

    private List<MessagingQueueListener> listeners = new CopyOnWriteArrayList<>();

    public RabbitQueue(RabbitChannel channel, String queueName) {
        this.channel = channel;
        this.client = channel.getClient();
        this.queueName = queueName;
    }

    public RabbitQueue setAutoAcknowledge(boolean autoAcknowledge) {
        this.autoAcknowledge = autoAcknowledge;
        return this;
    }

    public RabbitQueue setCreateQueue(boolean createQueue) {
        this.createQueue = createQueue;
        return this;
    }

    @Override
    public RabbitQueue open(boolean autoAcknowledge) {
        this.autoAcknowledge = autoAcknowledge;
        channel.getQueues().add(this);
        confirmState();
        return this;
    }

    public void confirmState() {
        synchronized (channel.getClient().CONFIRM_STATE_LOCK) {
            if (!channel.getQueues().contains(this)) return;

            try {
                if (channel.getChannel() == null || !channel.getChannel().isOpen()) return;

                if (consumeId.get() == null) {
                    if (createQueue) {
                        queueName = channel.getChannel().queueDeclare(queueName, false, true, true, null).getQueue();
                        client.getLog().accept("Queue declared named '" + queueName + "'.");
                    }

                    DefaultConsumer consumer = new DefaultConsumer(channel.getChannel()) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            onDelivery(consumerTag, envelope, properties, body);
                        }

                        @Override
                        public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                            consumeId.set(null);
                        }
                    };

                    consumeId.set(channel.getChannel().basicConsume(queueName, autoAcknowledge, consumer));
                    client.getLog().accept("Consuming queue named '" + queueName + "' with consume id '" + consumeId.get() + "'.");
                }

                String consumeId = this.consumeId.get();
                if (consumeId != null) {
                    for (RabbitQueueBinding binding : bindings) {
                        if (consumeId.equals(binding.getConsumeId())) continue;

                        channel.getChannel().queueBind(queueName, binding.getExchange(), binding.getRouting());
                        binding.setConsumeId(consumeId);
                        client.getLog().accept("Bound queue '" + queueName + "' to exchange '" + binding.getExchange() + "' with routing key '" + binding.getRouting() + "' on consume id '" + binding.getConsumeId() + "'.");
                    }
                }
            } catch (Throwable e) {
                client.getExceptionHandler().accept(e);
            }
        }
    }

    @Override
    public MessagingQueue bind(String exchange, String routing) throws MessagingException {
        RabbitQueueBinding binding = new RabbitQueueBinding();
        binding.setRabbitQueue(this);
        binding.setExchange(exchange);
        binding.setRouting(routing);
        bindings.add(binding);
        return this;
    }

    @Override
    public MessagingChannel getChannel() {
        return channel;
    }

    @Override
    public MessagingQueue withListener(MessagingQueueListener listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public String getName() {
        return queueName;
    }

    private void onDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        try {
            Message message = null;

            if (body != null && body.length > 0)
                message = client.getGson().fromJson(new String(body), Message.class);
            if (message == null) message = new Message();
            if (message.getAttributes() == null) message.setAttributes(new HashMap<>());

            message.getAttributes().put("envelope.exchange", envelope.getExchange());
            message.getAttributes().put("envelope.routing", envelope.getRoutingKey());

            message.setRabbitTag(envelope.getDeliveryTag());

            if (properties.getHeaders() != null) {
                for (Map.Entry<String, Object> header : properties.getHeaders().entrySet()) {
                    message.getAttributes().put("header." + header.getKey(), String.valueOf(header.getValue()));
                }
            }

            if (properties.getReplyTo() != null)
                message.getAttributes().put("properties.reply-to", properties.getReplyTo());
            if (properties.getCorrelationId() != null)
                message.getAttributes().put("properties.correlation-id", properties.getCorrelationId());


            MessageEvent messageEvent = new MessageEvent();
            messageEvent.setMessage(message);
            messageEvent.setQueue(this);

            String futureId = message.getAttributes().get(MessagingClient.FUTURE_ID);
            if (futureId != null) {
                MessageFuture messageFuture = client.getMessageFutures().get(futureId);
                if (messageFuture != null) {
                    messageFuture.complete(messageEvent);
                }
            }

            for (MessagingQueueListener listener : listeners) {
                try {
                    listener.onMessage(messageEvent);
                } catch (Throwable e) {
                    client.getExceptionHandler().accept(e);
                }
            }

        } catch (Throwable e) {
            client.getExceptionHandler().accept(e);
        }
    }
}
