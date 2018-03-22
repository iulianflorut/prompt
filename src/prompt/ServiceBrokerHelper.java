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
import javax.jms.MessageProducer;
import javax.jms.Queue;
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
		Properties p = new Properties();
		try {
			Optional.of((InputStream) new FileInputStream("cfg.properties")).ifPresent(t -> {
				try {
					p.load(t);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}
		TCP_BROKER_URL = p.getProperty("broker.url", "tcp://localhost:61616");
	}

	BrokerService createBroker() throws IOException, Exception {
		BrokerService broker = new BrokerService();
		broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
		broker.addConnector(ServiceBrokerHelper.TCP_BROKER_URL);
		broker.start();
		return broker;
	}

	Connection createConnection(ExceptionListener listener) throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ServiceBrokerHelper.TCP_BROKER_URL);

		Connection connection = connectionFactory.createConnection();

		connection.setExceptionListener(listener);

		connection.start();

		return connection;
	}

	void consumerListener(Connection connection, String queue, Consumer<String> consumer) throws JMSException {
		consumerListener(connection, queue, consumer, Optional.empty());
	}

	void consumerListener(Connection connection, String queue, Consumer<String> consumer,
			Optional<PropertyConsumer<String>> propertyConsumer) throws JMSException {

		Session session = createSession(connection);

		MessageConsumer mc = createMessageConsumer(session, queue);

		mc.setMessageListener(message -> {
			TextMessage textMessage = (TextMessage) message;
			try {

				if (propertyConsumer.isPresent() && Optional
						.ofNullable(textMessage.getBooleanProperty(propertyConsumer.get().property)).orElse(false)) {
					propertyConsumer.get().consumer.accept(textMessage.getText());
				} else {
					consumer.accept(textMessage.getText());
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		});
	}

	MessageConsumer createMessageConsumer(Session session, String queue) throws JMSException {

		Queue target = session.createQueue(queue);

		return session.createConsumer(target);
	}

	<T> void waitFor(T t, Predicate<T> p) throws InterruptedException {
		while (p.test(t)) {
			Thread.sleep(100);
		}
	}

	void sendMessage(Connection connection, String message, String queue, Optional<String> property) {
		try {
			Session session = createSession(connection);
			MessageProducer producer;
			Queue destination = session.createQueue(queue);
			producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			TextMessage textMessage = session.createTextMessage(message);

			property.ifPresent(p -> {
				try {
					textMessage.setBooleanProperty(p, true);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			});

			producer.send(textMessage);

			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void sendMessage(Connection connection, String message, String queue) {
		sendMessage(connection, message, queue, Optional.empty());
	}

	Session createSession(Connection connection) throws JMSException {
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}
}
