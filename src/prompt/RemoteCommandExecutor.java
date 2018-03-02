package prompt;

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

public class RemoteCommandExecutor {

	static final String EXIT = "exit";

	static final String EXIT_CLIENT = "exitc";

	static final String RESULT = "RESULT";

	static final String COMMAND = "COMMAND";

	static final String CURRENT_FOLDER = "cf=";

	//static final String TCP_BROKER_URL = "tcp://172.27.34.42:61616";
	static final String TCP_BROKER_URL = "tcp://localhost:61616";

	private static BrokerService broker;

	private static Connection connection;

	public static void start() {

		// configure the broker
		try {
			broker = new BrokerService();
			broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
			broker.addConnector(TCP_BROKER_URL);
			broker.start();

			RemoteCommandExecutor.waitFor(broker, p -> !p.isStarted());

			connection = createConnection();
			
			connection.setExceptionListener(e -> {
				System.out.println("JMS Exception occured.  Shutting down client.");
			});

			remote();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static Connection createConnection() throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(TCP_BROKER_URL);

		// Create a Connection
		connection = connectionFactory.createConnection();

		connection.start();

		return connection;
	}

	private static void remote() throws Exception {

		final LocalCommand command = new LocalCommand();

		command.setResultConsumer(p -> sendMessage(p, RemoteCommandExecutor.RESULT));

		consumerListener(RemoteCommandExecutor.COMMAND, cmd -> {
			command.execute(cmd);
			sendMessage(RemoteCommandExecutor.CURRENT_FOLDER+command.getCurrentFolder(), RemoteCommandExecutor.RESULT);
			checkForExit(cmd);
		});
	}

	public static void consumerListener(String queue, Consumer<String> consumer) throws JMSException {

		Session session = createSession();

		MessageConsumer mc = RemoteCommandExecutor.createMessageConsumer(session, queue);

		mc.setMessageListener(message -> {
			TextMessage textMessage = (TextMessage) message;
			try {
				consumer.accept(textMessage.getText());
			} catch (JMSException e) {
				e.printStackTrace();
			}
		});

	}

	public static MessageConsumer createMessageConsumer(Session session, String queue) throws JMSException {

		Queue target = session.createQueue(queue);

		return session.createConsumer(target);
	}

	private static void checkForExit(String cmd) {
		if (cmd.equals(RemoteCommandExecutor.EXIT)) {
			try {
				waitFor(broker, p -> p.getCurrentConnections() > 1);
				connection.close();
				waitFor(broker, p -> p.getCurrentConnections() > 0);
				broker.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static <T> void waitFor(T t, Predicate<T> p) throws InterruptedException {
		while (p.test(t)) {
			Thread.sleep(100);
		}
	}

	public static void sendMessage(String message, String queue) {
		try {
			Session session = createSession();
			MessageProducer producer;
			Queue destination = session.createQueue(queue);
			producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			TextMessage textMessage = session.createTextMessage(message);
			producer.send(textMessage);

			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Session createSession() throws JMSException {
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

}
