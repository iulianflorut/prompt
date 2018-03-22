package prompt;

import java.util.Optional;
import java.util.stream.Stream;

public enum Environment {

	local(LocalCommand.class), remote(RemoteCommand.class);

	private Commandable command;

	private Class<? extends Commandable> clazz;
	
	private  static Environment env = local;

	Environment(Class<? extends Commandable> clazz) {
		this.clazz = clazz;
		try {
			this.command = clazz.newInstance();
		} catch (Exception e) {
		}
	}

	public Environment start() {
		if (!isStarted()) {
			try {
				this.command = clazz.newInstance();
			} catch (Exception e) {
				System.err.println("Cannot connect to the " + this.name() + " environment!");
			}
		}
		return this;
	}

	private Commandable getCommand() {
		return command;
	}

	public static void execute(String cmd) {
		
		Optional<Environment> openv = Stream.of(Environment.values()).filter(p -> p.name().equals(cmd))
				.findFirst();
		if (openv.isPresent()) {
			env = Environment.get(openv.get());
		} else { 
			Optional.ofNullable(env.getCommand()).filter(Commandable::isAlive).ifPresent(c -> c.execute(cmd));
		}
	}

	private boolean isStarted() {
		return command != null && command.isAlive();
	}

	private String shortName() {
		return name().substring(0, 1).toUpperCase();
	}

	public static void writePrompt() {
		Optional.ofNullable(env.getCommand()).filter(Commandable::isAlive).map(Environment::getPrompt)
				.ifPresent(System.out::print);
	}
	
	private static String getPrompt(Commandable c) {
		return String.format("%s %s>", env.shortName(), c.getDefaultFolder().getPath());
	}

	public static boolean exit(String cmd) {

		switch (cmd) {
		case ServiceBrokerHelper.EXIT:
			Stream.of(Environment.values()).filter(e -> e.isStarted()).forEach(e -> e.getCommand().exit());
			return true;
		case ServiceBrokerHelper.KILL:
			Stream.of(Environment.values()).filter(e -> e.isStarted()).forEach(e -> e.getCommand().kill());
			return true;
		default:
			return false;
		}
	}

	private static Environment get(Environment env) {
		return env.start().isStarted() ? env : local;
	}

}
