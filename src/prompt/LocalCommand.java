package prompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LocalCommand extends BaseCommand {

	private Map<String, String> paths = new HashMap<>();

	private String currentDrive;

	public LocalCommand() {
		setCurrentFolder(System.getProperty("user.dir"));
	}

	@Override
	public String getCurrentFolder() {
		return paths.get(currentDrive);
	}

	private void setCurrentFolder(String currentFolder) {
		currentDrive = currentFolder.substring(0, 1).toLowerCase();
		paths.put(currentDrive, currentFolder);
	}

	@Override
	public void execute(String cmd) {

		try {
			if (Optional.ofNullable(cmd).isPresent() && cmd.matches("[a-z]\\:")) {

				String drive = cmd.substring(0, 1).toLowerCase();
				if (paths.containsKey(drive)) {
					currentDrive = drive;
				} else if (new File(cmd).exists()) {
					setCurrentFolder(new File(cmd).getAbsolutePath());
				}
				return;
			}

			if (Optional.ofNullable(cmd).isPresent() && (cmd.startsWith("cd.") || cmd.startsWith("cd "))) {

				if (cmd.replaceAll(" ", "").startsWith("cd.")) {
					if (cmd.replaceAll(" ", "").equals("cd..") && getCurrentFolder().indexOf(File.separator) != -1) {
						setCurrentFolder(
								getCurrentFolder().substring(0, getCurrentFolder().lastIndexOf(File.separator)));
					}
				} else {
					cmd = cmd.split(" ")[1];
					if (cmd.matches("[a-z]\\:.*")) {
						if (new File(cmd).exists()) {
							setCurrentFolder(cmd);
						} else {
							System.out.println("The system cannot find the path specified.");
						}
					} else {
						if (new File(getCurrentFolder() + File.separator + cmd).exists()) {
							setCurrentFolder(getCurrentFolder() + File.separator + cmd);
						} else {
							System.out.println("The system cannot find the path specified.");
						}
					}
				}

				return;
			}

			Process p = Runtime.getRuntime().exec("cmd /c " + cmd, null, new File(getCurrentFolder() + File.separator));

			// Get input streams
			try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
					BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {

				// Read command standard output
				Optional<String> s;
				while ((s = Optional.ofNullable(stdInput.readLine())).isPresent()) {
					resultConsumer.accept(s.get());
				}

				// Read command errors
				while ((s = Optional.ofNullable(stdError.readLine())).isPresent()) {
					resultConsumer.accept(s.get());
				}
			}

			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
