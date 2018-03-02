package prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Prompt {

	public static void main(String[] args) throws Exception {

		if (args.length > 0 && args[0].equals("remote")) {
			RemoteCommandExecutor.start();
		} else {
			Environment env = Environment.local;

			if (args.length > 0 && args[0].startsWith("file=")) {
				String fileName = args[0].substring(5, args[0].length());
				if (!new File(fileName).exists()) {
					throw new FileNotFoundException(fileName);
				}

				try (Stream<String> stream = Files.lines(Paths.get(fileName))) {

					for (String cmd : stream.collect(Collectors.toList())) {

						writePrompt(env);

						if (exit(cmd)) {
							break;
						}

						env = executeCommand(env, cmd);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {

				try (PrintWriter writer = new PrintWriter("cmd_" + System.currentTimeMillis() + ".cmd", "UTF-8")) {

					do {
						writePrompt(env);

						BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

						final String cmd = br.readLine().toLowerCase().trim();

						if (Optional.ofNullable(cmd).orElse("").isEmpty())
							continue;

						writer.println(cmd);
						writer.println();

						if (exit(cmd)) {
							break;
						}

						env = executeCommand(env, cmd);

					} while (true);
				}
			}
		}
	}

	private static boolean exit(final String cmd) {
		return Stream.of(RemoteCommandExecutor.EXIT, RemoteCommandExecutor.EXIT_CLIENT).filter(p -> p.equals(cmd))
				.peek(p -> Environment.remote.getCommand().execute(p)).findFirst().isPresent();
	}

	private static void writePrompt(Environment env) {
		String currentFolder = env.getCommand().getCurrentFolder();

		File dir = new File(currentFolder);

		System.out.print(env.shortName() + " " + dir.getPath() + ">");
	}

	private static Environment executeCommand(Environment env, String cmd) throws Exception {

		if (Optional.ofNullable(cmd).isPresent()
				&& Stream.of(Environment.values()).anyMatch(p -> p.name().equals(cmd))) {
			env = Environment.valueOf(cmd);
			return env;
		}

		env.getCommand().execute(cmd);

		return env;
	}

}
