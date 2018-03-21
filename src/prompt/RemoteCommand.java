package prompt;

import java.io.File;

import javax.jms.Connection;
import javax.jms.JMSException;

public class RemoteCommand extends BaseCommand {

	private File defaultFolder;

	private Connection connection;
	
	private final ServiceBrokerHelper servivceBrokerHelper = new ServiceBrokerHelper();

	private final Object sync = new Object();

	public RemoteCommand() {
		try {
			connection = servivceBrokerHelper.createConnection();
			connection.setExceptionListener(e -> {
				synchronized (sync) {
					sync.notifyAll();
				}
				System.err.println("JMS Exception occured. Shutting down client.");
			});

			receiveResult();
			execute("");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public File getDefaultFolder() {
		return defaultFolder;
	}

	@Override
	public void execute(final String cmd) {

		try {
			if (cmd.equals(ServiceBrokerHelper.EXIT)) {
				exit();
				return;
			}

			synchronized (sync) {
				servivceBrokerHelper.sendMessage(connection, cmd, ServiceBrokerHelper.COMMAND);
				sync.wait();
			}

			if (cmd.equals(ServiceBrokerHelper.KILL)) {
				exit();
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void exit() throws JMSException {
		alive = false;
		connection.close();
	}

	private void receiveResult() throws JMSException {
		servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.RESULT, ServiceBrokerHelper.DEFAULT_FOLDER, (p, hasProperty) -> {
			if (hasProperty) {
				this.defaultFolder = new File(p);
				synchronized (sync) {
					sync.notifyAll();
				}
			} else {
				resultConsumer.accept(p);
			}
		});
	}
}
