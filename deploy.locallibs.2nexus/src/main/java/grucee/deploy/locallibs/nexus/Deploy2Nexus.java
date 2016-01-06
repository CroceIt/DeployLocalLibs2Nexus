package grucee.deploy.locallibs.nexus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;

/**
 * 将原来的普通java项目切换为maven项目 但是由于引用的开源jar包有做定制，因此nexus禁止从maven中央仓库下载jar包
 * 原来项目的所有lib都归属同一个组织：asiainfo
 * 
 * @author Grucee
 *
 */
public class Deploy2Nexus {

	public static void main(String[] args) throws IOException, InterruptedException {
		boolean upload = true;
		
		List<String> cmds = start();
		if (cmds == null || cmds.isEmpty()) {
			return;
		}


		if (upload) {
			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
			for (final String cmd : cmds) {
				executor.execute(new Runnable() {

					public void run() {
						Deploy2Nexus.exec(cmd);
					}

				});
			}
			executor.shutdown();
		}
	}

	private static void exec(String cmd) {
		PrintUtils.print(cmd);

		BufferedReader in = null;
		BufferedReader error = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);

			String inTemp = null;
			in = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
			PrintUtils.print("----------inputstream-------------");

			while ((inTemp = in.readLine()) != null) {
				PrintUtils.print(inTemp);
			}
			PrintUtils.print("----------inputstream-------------");

			error = new BufferedReader(new InputStreamReader(process.getErrorStream(), "utf-8"));
			PrintUtils.print("----------errorstream-------------");
			while ((inTemp = error.readLine()) != null) {
				PrintUtils.print(inTemp);
			}
			PrintUtils.print("----------errorstream-------------");
			
			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (error != null)
				try {
					error.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			if (process != null) {
				process.destroy();
			}
		}
	}

	public static List<String> start() throws IOException {
		String dir = ConfigProperties.getInstance().getScanDir();
		if (StringUtils.isEmpty(dir)) {
			PrintUtils.print("no operation.");
			return null;
		}

		StringBuilder depends = new StringBuilder();
		List<String> cmdList = new ArrayList<String>();
		
		File f = new File(dir);
		if (f.isDirectory()) {
			handleDir(f, depends, cmdList);
		} else {
			handleFile(f, depends, cmdList);
		}

		PrintUtils.print("----------cmd-------------");
		for (String l : cmdList) {
			PrintUtils.print(l);
		}
		PrintUtils.print("-----------------------");

		PrintUtils.print("----------depends-------------");
		PrintUtils.print(depends.toString());
		PrintUtils.print("-----------------------");
		return cmdList;
	}

	private static void handleDir(File dir, StringBuilder depends, List<String> cmdList) throws IOException {

		File[] files = dir.listFiles();
		if (files == null || files.length < 1) {
			return;
		}

		for (File f : files) {
			if (f.isDirectory()) {
				handleDir(f, depends, cmdList);
			}

			if (!f.getName().endsWith(".jar")) {
				continue;
			}

			handleFile(f, depends, cmdList);
		}
	}

	// 同时生成pom.xml里面的依赖
	private static void handleFile(File file, StringBuilder depends, List<String> cmdList) throws IOException {
		// mvn deploy:deploy-file -DgroupId=asiainfo
		// -DartifactId=terracotta-toolkit-api-internal -Dversion=1.12
		// -Dpackaging=jar -Dfile=D:\terracotta-toolkit-api-internal-1.12.jar
		// -Durl=http://ip:port/nexus/content/repositories/thirdparty/
		// -DrepositoryId=thirdparty

		// sb.append()
		String fileName = file.getName();
		fileName = StringUtils.substring(fileName, 0, StringUtils.indexOf(fileName, ".jar"));

		int index = StringUtils.lastIndexOf(fileName, "-");

		String cmdGroupId = "asiainfo";
		String cmdArtifactId = null;
		String cmdVersion = "1.0";
		String cmdFileName = file.getCanonicalPath();
		String cmdUrl = ConfigProperties.getInstance().getNexusUrl();
		String cmdRepositoryId = ConfigProperties.getInstance().getNexusRepositoryId();

		// jar包名称中不包含-
		if (index == -1) {
			cmdArtifactId = fileName;
		} else {
			// 取出版本号
			String front = StringUtils.substring(fileName, 0, index);
			String end = StringUtils.substring(fileName, index + 1);
			// end包含字母，说明不是版本号；不包含字母，说明是版本号
			if (isVersionStr(end)) { // 是版本号
				cmdArtifactId = front;
				cmdVersion = end;
			} else {
				cmdArtifactId = fileName;
			}
		}

		// 拼接字符串
		StringBuilder cmd = new StringBuilder();
		cmd.append(ConfigProperties.getInstance().getMaven()).append(" deploy:deploy-file -DgroupId=")
				.append(cmdGroupId).append(" -DartifactId=").append(cmdArtifactId).append(" -Dversion=")
				.append(cmdVersion).append(" -Dpackaging=jar -Dfile=").append(cmdFileName).append(" -Durl=")
				.append(cmdUrl).append(" -DrepositoryId=").append(cmdRepositoryId);
		cmdList.add(cmd.toString());

		// depency
		// <dependency>
		// <groupId>commons-lang</groupId>
		// <artifactId>commons-lang</artifactId>
		// <version>2.6</version>
		// </dependency>
		depends.append("<dependency>").append("<groupId>").append(cmdGroupId).append("</groupId>")
				.append("<artifactId>").append(cmdArtifactId).append("</artifactId>").append("<version>")
				.append(cmdVersion).append("</version>").append("</dependency>").append("\n");

	}

	private static boolean isVersionStr(String s) {
		char[] chars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' };
		return StringUtils.containsOnly(s, chars);
	}
}
