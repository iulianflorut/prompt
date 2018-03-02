package prompt;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

public class RemoteCommand extends BaseCommand {

	private String currentFolder = System.getProperty("user.dir").toLowerCase();

	private Session session;

	private Connection connection;

	private Object sync = new Object();

	public RemoteCommand() {
		try {
			connection = RemoteCommandExecutor.createConnection();
			session = connection.createSession(true, Session.SESSION_TRANSACTED);
			receiveCurrentFolder();
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

			RemoteCommandExecutor.sendMessage(session, cmd, RemoteCommandExecutor.COMMAND);

			synchronized (sync) {
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
		session.close();
		connection.close();
	}

	private void receiveResult() throws JMSException {
		RemoteCommandExecutor.consumerListener(session, RemoteCommandExecutor.RESULT, resultConsumer);
	}

	private void receiveCurrentFolder() throws JMSException {
		RemoteCommandExecutor.consumerListener(session, RemoteCommandExecutor.CURRENT_FOLDER, (p, commiter) -> {
			currentFolder = p;
			commiter.commit(session);
			synchronized (sync) {
				sync.notify();
			}
		});

	}
}
