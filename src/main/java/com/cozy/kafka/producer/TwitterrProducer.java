package com.cozy.kafka.producer;

import com.cozy.kafka.infrastructure.twitter.TwitterClient;
import com.cozy.kafka.infrastructure.twitter.TwitterQueue;
import com.twitter.hbc.core.Client;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;

public class TwitterrProducer implements Producer {

    private Logger logger = LoggerFactory.getLogger(TwitterrProducer.class.getName());
    private final String KAFKA_TOPIC = "twitter_tweets";

    private Properties properties;

    public TwitterrProducer(Properties properties) {
        this.properties = properties;
    }

    public KafkaProducer<String, String> producer() {
        return new KafkaProducer<>(properties);
    }

    @Override
    public void perform() {
        BlockingQueue<String> queue = new TwitterQueue().queue();

        Client client = new TwitterClient().client(queue);
        KafkaProducer<String, String> producer = producer();

        logger.info("Starting streaming data");

        client.connect();

        while (!client.isDone()) {
            String message = null;
            try {
                message = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }

            if (message != null) {
                logger.info(message);
                producer.send(new ProducerRecord<>(KAFKA_TOPIC, null, message), (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Something bad happened", exception);
                    }
                });
            }
        }

        logger.info("Ending streaming data");
    }

    private void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping application...");
        }));
    }
}
