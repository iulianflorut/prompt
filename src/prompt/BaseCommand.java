package prompt;

import java.util.function.Consumer;

public abstract class BaseCommand implements Commandable {

	protected Consumer<String> resultConsumer;
	
	protected boolean alive;
	
	protected BaseCommand() {
		alive = true;
		setResultConsumer(System.out::println);
	}

	public void setResultConsumer(final Consumer<String> resultConsumer) {
		this.resultConsumer = resultConsumer;
	}

	public boolean isAlive() {
		return alive;
	}
	
}
