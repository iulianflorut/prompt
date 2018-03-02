package prompt;

import javax.jms.Connection;
import javax.jms.JMSException;

public class RemoteCommand extends BaseCommand {

	private String currentFolder;

	private Connection connection;
	
	ServiceBrokerHelper servivceBrokerHelper = new ServiceBrokerHelper();

	private Object sync = new Object();

	public RemoteCommand() {
		try {
			connection = servivceBrokerHelper.createConnection();
			connection.setExceptionListener(e -> {
				synchronized (sync) {
					sync.notifyAll();
				}
				System.out.println("JMS Exception occured.  Shutting down client.");
			});

			receiveResult();
			execute("");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getCurrentFolder() {
		return currentFolder;
	}

	@Override
	public void execute(String cmd) {

		try {
			if (cmd.equals(ServiceBrokerHelper.EXIT_CLIENT)) {
				exit();
				return;
			}

			synchronized (sync) {
				servivceBrokerHelper.sendMessage(connection, cmd, ServiceBrokerHelper.COMMAND);
				sync.wait();
			}

			if (cmd.equals(ServiceBrokerHelper.EXIT)) {
				exit();
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void exit() throws JMSException {
		connection.close();
	}

	private void receiveResult() throws JMSException {
		servivceBrokerHelper.consumerListener(connection, ServiceBrokerHelper.RESULT, ServiceBrokerHelper.CURRENT_FOLDER, (p, hasProperty) -> {
			if (hasProperty) {
				this.currentFolder = p;
				synchronized (sync) {
					sync.notifyAll();
				}
			} else {
				resultConsumer.accept(p);
			}
		});
	}
}
