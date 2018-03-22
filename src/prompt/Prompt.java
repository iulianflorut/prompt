package prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Prompt {

	private static final String FILE = "file=";

	public static void main(String[] args) throws Exception {

		Optional<String> remote = Stream.of(args).filter(p -> p.equals(Environment.remote.name())).findFirst();

		if (remote.isPresent()) {
			new RemoteCommandListener().start();
		} else {
			boolean exit = false;

			Optional<File> file = Stream.of(args).filter(p -> p.startsWith(Prompt.FILE))
					.map(p -> p.substring(Prompt.FILE.length())).map(File::new).findFirst();

			if (file.isPresent()) {
				try (InputStream is = new FileInputStream(file.get())) {
					exit = run(is, System.out::println);
				} catch (FileNotFoundException e) {
					System.err.println(e.getMessage());
				}
			}

			if (!exit) {
				try (PrintWriter out = new PrintWriter("cmd_" + System.currentTimeMillis() + ".log", "UTF-8")) {
					run(System.in, out::println);
				}
			}
		}
	}

	private static boolean run(InputStream is, Consumer<String> c)
			throws IOException, Exception, FileNotFoundException, UnsupportedEncodingException {
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

			do {
				Environment.writePrompt();

				final String line = br.readLine();

				if (Optional.ofNullable(line).isPresent()) {

					final String cmd = line.trim();

					if (cmd.isEmpty()) {
						continue;
					}

					c.accept(cmd);

					if (Environment.exit(cmd)) {
						return true;
					}

					Environment.execute(cmd);
				} else
					return false;

			} while (true);
		}
	}
}
