package prompt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

public class ServiceBrokerHelper {

	static final String KILL = "kill";
	static final String EXIT = "exit";
	static final String RESULT = "RESULT";
	static final String COMMAND = "COMMAND";
	static final String DEFAULT_FOLDER = "dir";
	static String TCP_BROKER_URL;

	ServiceBrokerHelper() {
		var p = new Properties();
		try {
			Optional.of((InputStream) new FileInputStream("cfg.properties")).ifPresent(consumer(p::load));
		} catch (IOException e) {
			e.printStackTrace();
		}
		TCP_BROKER_URL = p.getProperty("broker.url", "tcp://localhost:61616");
	}

	BrokerService createBroker() throws IOException, Exception {
		var broker = new BrokerService();
		broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
		broker.addConnector(ServiceBrokerHelper.TCP_BROKER_URL);
		broker.start();
		return broker;
	}

	Connection createConnection(final ExceptionListener listener) throws JMSException {
		var connectionFactory = new ActiveMQConnectionFactory(ServiceBrokerHelper.TCP_BROKER_URL);

		final var connection = connectionFactory.createConnection();

		connection.setExceptionListener(listener);

		connection.start();

		return connection;
	}

	void consumerListener(final Connection connection, final String queue, final Consumer<String> consumer) throws JMSException {
		consumerListener(connection, queue, consumer, Optional.empty());
	}

	void consumerListener(final Connection connection, final String queue,final Consumer<String> consumer,
			final Optional<PropertyConsumer<String>> propertyConsumer) throws JMSException {

		final var session = createSession(connection);

		final var mc = createMessageConsumer(session, queue);

		mc.setMessageListener(message -> {
			TextMessage textMessage = (TextMessage) message;
			try {

				if (propertyConsumer.map(p -> p.property).filter(predicate(textMessage::propertyExists))
						.filter(predicate(textMessage::getBooleanProperty)).isPresent()) {
					propertyConsumer.get().consumer.accept(textMessage.getText());
				} else {
					consumer.accept(textMessage.getText());
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		});
	}

	<T> Predicate<T> predicate(final ThrowablePredicate<T> p) {
		return i -> p.test(i);
	}

	<T> Consumer<T> consumer(final ThrowableConsumer<T> p) {
		return i -> p.accept(i);
	}

	MessageConsumer createMessageConsumer(final Session session, final String queue) throws JMSException {

		final var target = session.createQueue(queue);

		return session.createConsumer(target);
	}

	<T> void waitFor(T t, Predicate<T> p) throws InterruptedException {
		while (p.test(t)) {
			Thread.sleep(100);
		}
	}

	void sendMessage(final Connection connection, final String message, final String queue, final Optional<String> property) {
		try {
			final var session = createSession(connection);
			final var destination = session.createQueue(queue);
			final var producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			final var textMessage = session.createTextMessage(message);

			property.ifPresent(consumer(p -> textMessage.setBooleanProperty(p, true)));

			producer.send(textMessage);

			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void sendMessage(final Connection connection, final String message, final String queue) {
		sendMessage(connection, message, queue, Optional.empty());
	}

	Session createSession(final Connection connection) throws JMSException {
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}
}
