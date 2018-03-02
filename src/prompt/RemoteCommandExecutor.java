package prompt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
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

public class RemoteCommandExecutor {

	static final String EXIT = "exit";

	static final String EXIT_CLIENT = "exitc";

	public static final String RESULT = "RESULT";

	public static final String CURRENT_FOLDER = "CURRENT_FOLDER";

	public static final String COMMAND = "COMMAND";

	public static final String TCP_BROKER_URL = "tcp://localhost:61616";

	private static BrokerService broker;

	private static Connection connection;

	private static Map<String, MessageProducer> producers = new HashMap<>();

	public static void start() {

		// configure the broker
		try {
			broker = new BrokerService();
			broker.addConnector(TCP_BROKER_URL);
			broker.start();

			RemoteCommandExecutor.waitFor(broker, p -> !p.isStarted());

			connection = createConnection();

			Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

			remote(session);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static Connection createConnection() throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(TCP_BROKER_URL);

		// Create a Connection
		connection = connectionFactory.createConnection();

		connection.setExceptionListener(new ExceptionListener() {

			@Override
			public void onException(JMSException exception) {
				System.out.println("JMS Exception occured.  Shutting down client.");
			}
		});

		connection.start();

		return connection;
	}

	private static void remote(Session session) throws Exception {

		final LocalCommand command = new LocalCommand();

		command.setResultConsumer(p -> sendMessage(session, p, RemoteCommandExecutor.RESULT));

		consumerListener(session, RemoteCommandExecutor.COMMAND, (cmd, commiter) -> {
			try {
				command.execute(cmd);
				sendMessage(session, command.getCurrentFolder(), RemoteCommandExecutor.CURRENT_FOLDER);
				commiter.commit(session);
				checkForExit(session, cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	static class SessionCommiter {
		
		boolean commited = false;
		
		void commit(Session session) {
			if (commited) return;
			try {
				session.commit();
				commited = true;
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void consumerListener(Session session, String queue, Consumer<String> consumer) throws JMSException {
		consumerListener(session, queue, (p, c) -> consumer.accept(p));
	}
	

	public static void consumerListener(Session session, String queue, BiConsumer<String, SessionCommiter> consumer) throws JMSException {

		MessageConsumer mc = RemoteCommandExecutor.createMessageConsumer(session, queue);
		
		SessionCommiter commiter = new SessionCommiter();

		mc.setMessageListener(message -> {
			TextMessage textMessage = (TextMessage) message;
			try {
				consumer.accept(textMessage.getText(), commiter);
				commiter.commit(session);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		});

	}

	public static MessageConsumer createMessageConsumer(Session session, String queue) throws JMSException {
		Queue target = session.createQueue(queue);

		return session.createConsumer(target);
	}

	private static void checkForExit(Session session, String cmd) throws JMSException, InterruptedException, Exception {
		if (cmd.equals(RemoteCommandExecutor.EXIT)) {
			waitFor(broker, p -> p.getCurrentConnections() > 1);
			session.close();
			connection.close();
			waitFor(broker, p -> p.getCurrentConnections() > 0);
			broker.stop();
		}
	}

	public static <T> void waitFor(T t, Predicate<T> p) throws InterruptedException {
		while (p.test(t)) {
			Thread.sleep(100);
		}
	}

	public static void sendMessage(Session session, String message, String queue) {
		try {
			MessageProducer producer;
			if (producers.containsKey(queue)) {
				producer = producers.get(queue);
			} else {
				Queue destination = session.createQueue(queue);
				producer = session.createProducer(destination);
				producer.setDeliveryMode(DeliveryMode.PERSISTENT);
				producers.put(queue, producer);
			}

			TextMessage textMessage = session.createTextMessage(message);
			producer.send(textMessage);

			session.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
