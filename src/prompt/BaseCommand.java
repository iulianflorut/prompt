package prompt;

import java.util.function.Consumer;

public abstract class BaseCommand implements Commandable {

	protected Consumer<String> resultConsumer;
	
	protected BaseCommand() {
		setResultConsumer(System.out::println);
	}

	@Override
	public void setResultConsumer(Consumer<String> resultConsumer) {
		this.resultConsumer = resultConsumer;
	}

	
}
