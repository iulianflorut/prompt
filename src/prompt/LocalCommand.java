package prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalCommand extends BaseCommand {

	private Map<String, File> paths = new HashMap<>();

	private String root;

	private final Pattern cdPattern = Pattern.compile("^((?i)cd|chdir)(\\.\\.| .*)$");

	private final Pattern rootPattern = Pattern.compile("^([A-Za-z]\\:)$");

	public LocalCommand() {
		changeRoot(System.getProperty("user.dir"));
	}

	@Override
	public File getDefaultFolder() {
		return paths.get(root);
	}

	private void changeRoot(final String root) {
		var file = new File(root);
		if (file.exists()) {
			this.root = getFileRoot(file);
			if (!paths.containsKey(this.root)) {
				setDefaultFolder(file);
			}
		} else {
			System.err.println("The system cannot find the drive specified.");
		}
	}

	private String getFileRoot(final File file) {
		return Paths.get(file.toURI()).getRoot().toString().toLowerCase();
	}

	private void setDefaultFolder(final String path) {
		Optional.ofNullable(new File(path)).filter(File::exists).ifPresentOrElse(this::setDefaultFolder, () -> {
			System.err.println("The system cannot find the path specified.");
		});
	}

	private void setDefaultFolder(final File file) {
		paths.put(getFileRoot(file), file);
	}

	@Override
	public void execute(final String cmd) {

		if (!Optional.ofNullable(cmd).isPresent() || cmd.isEmpty())
			return;

		final var root = Optional.of(rootPattern.matcher(cmd)).filter(Matcher::find).map(m -> m.group(1));

		if (root.isPresent()) {
			changeRoot(root.get());
		} else {
			Optional.of(cdPattern.matcher(cmd)).filter(Matcher::find).map(m -> m.group(2).trim())
					.map(m -> Paths.get(getDefaultFolder().toURI()).resolve(m).normalize().toString())
					.ifPresentOrElse(this::setDefaultFolder, () -> {
						try {
							final var p = new ProcessBuilder("cmd", "/c", cmd).directory(getDefaultFolder())
									.redirectErrorStream(true).start();

							try (final BufferedReader buffer = new BufferedReader(
									new InputStreamReader(p.getInputStream()))) {
								buffer.lines().forEach(resultConsumer);
							}

							p.waitFor();
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
		}
	}
}
