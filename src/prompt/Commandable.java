package prompt;

import java.io.File;
import java.util.function.Consumer;

public interface Commandable {

	File getDefaultFolder();

	void execute(final String cmd);

	void setResultConsumer(final Consumer<String> c);

	boolean isAlive();

	void exit();

	void kill();
}
