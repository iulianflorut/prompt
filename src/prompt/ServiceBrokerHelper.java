package prompt;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
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

	static final String EXIT = "exit";
	static final String EXIT_CLIENT = "exitc";
	static final String RESULT = "RESULT";
	static final String COMMAND = "COMMAND";
	static final String CURRENT_FOLDER = "cf=";
	// static final String TCP_BROKER_URL = "tcp://172.27.34.42:61616";
	static final String TCP_BROKER_URL = "tcp://localhost:61616";

	Connection createConnection() throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ServiceBrokerHelper.TCP_BROKER_URL);

		// Create a Connection
		Connection connection = connectionFactory.createConnection();

		connection.start();

		return connection;
	}

	void consumerListener(Connection connection, String queue, String property, BiConsumer<String, Boolean> consumer)
			throws JMSException {

		Session session = createSession(connection);

		MessageConsumer mc = createMessageConsumer(session, queue);

		mc.setMessageListener(message -> {
			TextMessage textMessage = (TextMessage) message;
			try {
				Boolean hasProperty = Optional.ofNullable(property).isPresent()
						&& Optional.ofNullable(textMessage.getBooleanProperty(property)).isPresent();
				consumer.accept(textMessage.getText(), hasProperty);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		});
	}

	void consumerListener(Connection connection, String queue, Consumer<String> consumer) throws JMSException {

		consumerListener(connection, queue, null, (m, b) -> consumer.accept(m));
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

	void sendMessage(Connection connection, String message, String queue, String property) {
		try {
			Session session = createSession(connection);
			MessageProducer producer;
			Queue destination = session.createQueue(queue);
			producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			TextMessage textMessage = session.createTextMessage(message);

			Optional.ofNullable(property).ifPresent(p -> {
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
		sendMessage(connection, message, queue, null);
	}

	Session createSession(Connection connection) throws JMSException {
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	BrokerService createBroker() throws IOException, Exception {
		BrokerService broker = new BrokerService();
		broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
		broker.addConnector(ServiceBrokerHelper.TCP_BROKER_URL);
		broker.start();
		return broker;
	}
}
