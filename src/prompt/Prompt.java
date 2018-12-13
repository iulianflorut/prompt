package prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Prompt {

	private static final String FILE = "file=";
	
	private static List<String> arguments; 
	
	private static final Pattern argPattern = Pattern.compile("%\\d");


	public static void main(String[] args) throws Exception {

		var remote = Stream.of(args).filter(p -> p.equals(Environment.remote.name())).findFirst();

		if (remote.isPresent()) {
			new RemoteCommandListener().start();
		} else {
			var exit = false;

			var file = Stream.of(args).filter(p -> p.startsWith(Prompt.FILE))
					.map(p -> p.substring(Prompt.FILE.length())).map(File::new).findFirst();
			
			arguments = Stream.of(args).filter(p -> !p.startsWith(Prompt.FILE)).collect(Collectors.toList());

			if (file.isPresent()) {
				try (var is = new FileInputStream(file.get())) {
					exit = run(is, System.out::println);
				} catch (FileNotFoundException e) {
					System.err.println(e.getMessage());
				}
			}

			if (!exit) {
				try (var out = new PrintWriter("log\\cmd_" + System.currentTimeMillis() + ".log", "UTF-8")) {
					run(System.in, out::println);
				}
			}
		}
	}

	private static boolean run(InputStream is, Consumer<String> c)
			throws Exception {
		
		try (var br = new BufferedReader(new InputStreamReader(is))) {

			do {
				Environment.writePrompt();

				final var line = br.readLine();

				if (Optional.ofNullable(line).isPresent()) {

					var cmd = line.trim();
					
					var m = argPattern.matcher(cmd);
					
					while (m.find()) {
						var index = Integer.valueOf(m.group().substring(1));
						
						if (arguments.size() >= index) {
							cmd = cmd.replace(m.group(), arguments.get(index-1));
						}
					}

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
