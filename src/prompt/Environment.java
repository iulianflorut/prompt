package prompt;

import java.util.Optional;
import java.util.stream.Stream;

public enum Environment {

	local(LocalCommand.class), remote(RemoteCommand.class);

	private Commandable command;

	private Class<? extends Commandable> clazz;

	Environment(Class<? extends Commandable> clazz) {
		this.clazz = clazz;
		try {
			this.command = clazz.newInstance();
		} catch (Exception e) {
		}
	}

	public void start() {
		if (!isAlive()) {
			try {
				this.command = clazz.newInstance();
			} catch (Exception e) {
				System.err.println("Cannot connect to the " + this.name() + " environment!");
			}
		}
	}

	private Commandable getCommand() {
		return command;
	}

	public void execute(String cmd) {
		Optional.ofNullable(this.getCommand()).filter(Commandable::isAlive).ifPresent(c -> c.execute(cmd));
	}

	public boolean isAlive() {
		return command != null && command.isAlive();
	}

	public String shortName() {
		return name().substring(0, 1).toUpperCase();
	}

	public void writePrompt() {
		Optional.ofNullable(this.getCommand()).filter(Commandable::isAlive)
				.ifPresent(c -> System.out.print(shortName() + " " + c.getDefaultFolder().getPath() + ">"));
	}

	public static boolean exit(String cmd) {

		switch (cmd) {
		case ServiceBrokerHelper.EXIT:
			Stream.of(Environment.values()).filter(e -> e.isAlive()).forEach(e -> e.getCommand().exit());
			return true;
		case ServiceBrokerHelper.KILL:
			Stream.of(Environment.values()).filter(e -> e.isAlive()).forEach(e -> e.getCommand().kill());
			return true;
		default:
			return false;
		}
	}

	public static Environment get(Environment env) {
		env.start();
		return env.isAlive() ? env : local;

	}

}
