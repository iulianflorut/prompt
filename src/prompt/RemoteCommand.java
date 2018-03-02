package prompt;

import javax.jms.Connection;
import javax.jms.JMSException;

public class RemoteCommand extends BaseCommand {

	private String currentFolder;

	private Connection connection;

	private Object sync = new Object();

	public RemoteCommand() {
		try {
			connection = RemoteCommandExecutor.createConnection();
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
			if (cmd.equals(RemoteCommandExecutor.EXIT_CLIENT)) {
				exit();
				return;
			}

			synchronized (sync) {
				RemoteCommandExecutor.sendMessage(cmd, RemoteCommandExecutor.COMMAND);
				sync.wait();
			}

			if (cmd.equals(RemoteCommandExecutor.EXIT)) {
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
		RemoteCommandExecutor.consumerListener(RemoteCommandExecutor.RESULT, p -> {
			if (p.startsWith(RemoteCommandExecutor.CURRENT_FOLDER)) {
				this.currentFolder = p.substring(RemoteCommandExecutor.CURRENT_FOLDER.length(), p.length());
				synchronized (sync) {
					sync.notifyAll();
				}
			} else {
				resultConsumer.accept(p);
			}
		});
	}
}
