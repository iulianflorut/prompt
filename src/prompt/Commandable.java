package prompt;

import java.util.function.Consumer;

public interface Commandable {

	String getCurrentFolder();

	void execute(String cmd);

	void setResultConsumer(Consumer<String> c);
}
