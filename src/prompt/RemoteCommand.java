package prompt;

import java.io.File;

import javax.jms.Connection;
import javax.jms.JMSException;

public class RemoteCommand extends BaseCommand {

	private File defaultFolder = new File(System.getProperty("user.dir"));

	private Connection connection;

	private final ServiceBrokerHelper servivceBrokerHelper = new ServiceBrokerHelper();

	private final Object sync = new Object();

	public RemoteCommand() {
		try {
			connection = servivceBrokerHelper.createConnection(e -> {
				synchronized (sync) {
					sync.notify();
				}
				System.err.println("JMS Exception occured. Shutting down client.");
			});

			receiveResult();
			execute("");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getDefaultFolder() {
		return defaultFolder;
	}

	@Override
	public void execute(final String cmd) {
		try {
			synchronized (sync) {
				servivceBrokerHelper.sendMessage(connection, cmd, ServiceBrokerHelper.COMMAND);
				sync.wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void exit() {
		try {
			super.exit();
			connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void kill() {
		servivceBrokerHelper.sendMessage(connection, ServiceBrokerHelper.KILL, ServiceBrokerHelper.KILL);
		super.kill();
	}

	private void receiveResult() throws JMSException {
		servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.RESULT, resultConsumer,
				PropertyConsumer.optional(ServiceBrokerHelper.DEFAULT_FOLDER, this::setDefaultFolder));
	}

	private void setDefaultFolder(String dir) {
		this.defaultFolder = new File(dir);
		synchronized (sync) {
			sync.notify();
		}
	}
}
