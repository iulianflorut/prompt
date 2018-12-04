package prompt;

import java.util.Optional;

import javax.jms.Connection;

import org.apache.activemq.broker.BrokerService;

public class RemoteCommandListener {

	private final ServiceBrokerHelper servivceBrokerHelper = new ServiceBrokerHelper();

	public void start() {

		try {
			// configure the broker
			final var broker = servivceBrokerHelper.createBroker();

			servivceBrokerHelper.waitFor(broker, p -> !p.isStarted());

			final var connection = servivceBrokerHelper
					.createConnection(e -> System.err.println("JMS Exception occured. Shutting down client."));

			final var command = new LocalCommand();

			command.setResultConsumer(p -> servivceBrokerHelper.sendMessage(connection, p, ServiceBrokerHelper.RESULT));

			servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.COMMAND, cmd -> {
				command.execute(cmd);
				servivceBrokerHelper.sendMessage(connection, command.getDefaultFolder().getAbsolutePath(),
						ServiceBrokerHelper.RESULT, Optional.of(ServiceBrokerHelper.DEFAULT_FOLDER));
			});

			servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.KILL,
					c -> kill(broker, connection));

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
