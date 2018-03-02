package prompt;

public enum Environment {

	local(LocalCommand.class), remote(RemoteCommand.class);

	private Commandable command;

	private Class<? extends Commandable> clazz;

	Environment(Class<? extends Commandable> clazz) {
		this.clazz = clazz;
	}

	public Commandable getCommand() {
		if (command == null) {
			try {
				this.command = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return command;
	}

	public String shortName() {
		return name().substring(0, 1).toUpperCase();
	}

}
