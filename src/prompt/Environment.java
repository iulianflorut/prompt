package prompt;

import java.util.stream.Stream;

public enum Environment {

	local(LocalCommand.class), remote(RemoteCommand.class);

	private Commandable command;

	private Class<? extends Commandable> clazz;

	Environment(Class<? extends Commandable> clazz) {
		this.clazz = clazz;
	}

	public Commandable getCommand() {
		if (!isInstantiated()) {
			try {
				this.command = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return command;
	}

	public boolean isInstantiated() {
		return command != null;
	}

	public String shortName() {
		return name().substring(0, 1).toUpperCase();
	}
	
	public void writePrompt() {
		System.out.print(shortName() + " " + getCommand().getDefaultFolder().getPath() + ">");
	}

	public static boolean exit(String cmd) {
		boolean exit = Stream.of(ServiceBrokerHelper.KILL, ServiceBrokerHelper.EXIT).filter(p -> p.equals(cmd))
				.findFirst().isPresent();

		if (exit && Environment.remote.isInstantiated() && Environment.remote.getCommand().isAlive()) {
			Environment.remote.getCommand().execute(cmd);
		}

		return exit;
	}

}
