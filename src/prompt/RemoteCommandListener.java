package prompt;

import javax.jms.Connection;

import org.apache.activemq.broker.BrokerService;

public class RemoteCommandListener {

	private final ServiceBrokerHelper servivceBrokerHelper = new ServiceBrokerHelper();

	public void start() {

		try {
			// configure the broker
			final BrokerService broker = servivceBrokerHelper.createBroker();

			servivceBrokerHelper.waitFor(broker, p -> !p.isStarted());

			final Connection connection = servivceBrokerHelper.createConnection();

			connection.setExceptionListener(e -> System.err.println("JMS Exception occured. Shutting down client."));

			final LocalCommand command = new LocalCommand();

			command.setResultConsumer(p -> servivceBrokerHelper.sendMessage(connection, p, ServiceBrokerHelper.RESULT));

			servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.COMMAND, cmd -> {
				command.execute(cmd);
				servivceBrokerHelper.sendMessage(connection, command.getDefaultFolder().getAbsolutePath(),
						ServiceBrokerHelper.RESULT, ServiceBrokerHelper.DEFAULT_FOLDER);
			});

			servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.KILL, cmd -> {
				kill(broker, connection);
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void kill(final BrokerService broker, final Connection connection) {
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
