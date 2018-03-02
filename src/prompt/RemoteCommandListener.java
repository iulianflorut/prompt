package prompt;

import javax.jms.Connection;

import org.apache.activemq.broker.BrokerService;

public class RemoteCommandListener {

	ServiceBrokerHelper servivceBrokerHelper = new ServiceBrokerHelper();

	public void start() {

		try {
			// configure the broker
			BrokerService broker = servivceBrokerHelper.createBroker();

			servivceBrokerHelper.waitFor(broker, p -> !p.isStarted());

			Connection connection = servivceBrokerHelper.createConnection();

			connection.setExceptionListener(e -> {
				System.out.println("JMS Exception occured.  Shutting down client.");
			});

			final LocalCommand command = new LocalCommand();

			command.setResultConsumer(p -> servivceBrokerHelper.sendMessage(connection, p, ServiceBrokerHelper.RESULT));

			servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.COMMAND, cmd -> {
				command.execute(cmd);
				servivceBrokerHelper.sendMessage(connection,
						command.getCurrentFolder(), ServiceBrokerHelper.RESULT, ServiceBrokerHelper.CURRENT_FOLDER);
				checkForExit(broker, connection, cmd);
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkForExit(BrokerService broker, Connection connection, String cmd) {
		if (cmd.equals(ServiceBrokerHelper.EXIT)) {
			try {
				servivceBrokerHelper.waitFor(broker, p -> p.getCurrentConnections() > 1);
				connection.close();
				servivceBrokerHelper.waitFor(broker, p -> p.getCurrentConnections() > 0);
				broker.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
