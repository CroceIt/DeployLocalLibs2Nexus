package grucee.deploy.locallibs.nexus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

public class ConfigProperties {
	private static volatile ConfigProperties INSTANCE = null;
	private static final Object LOCKER = new Object();
	private String scanDir = null;
	private String nexusUrl = null;
	private String nexusRepositoryId = null;
	private String maven = null;
	private String version = null;
	private String groupId = null;
	
	public String getGroupId() {
		return groupId;
	}

	public String getVersion() {
		return version;
	}

	public String getMaven() {
		return maven;
	}

	public String getNexusUrl() {
		return nexusUrl;
	}


	public String getNexusRepositoryId() {
		return nexusRepositoryId;
	}


	public String getScanDir() {
		return scanDir;
	}


	private ConfigProperties(){}
	
  	
	public static ConfigProperties getInstance() {
		if (INSTANCE == null) {
			synchronized(LOCKER) {
				if (INSTANCE == null) {
					ConfigProperties t = new ConfigProperties();
					t.init();
					INSTANCE = t;
				}
			}
		}
		
		return INSTANCE;
	}
	
	public void init() {
		Properties p = new Properties();
		InputStream in = ConfigProperties.class.getClassLoader().getResourceAsStream("config.properties");
		try {
			p.load(in);
		} catch (IOException e) {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
				}
			}
		}
		
		this.scanDir = StringUtils.trim(p.getProperty("scan.dir"));
		this.nexusUrl = StringUtils.trim(p.getProperty("nexus.url"));
		this.nexusRepositoryId = StringUtils.trim(p.getProperty("nexus.repositoryId"));
		this.maven = StringUtils.trim(p.getProperty("maven"));
		this.version = StringUtils.trim(p.getProperty("version"));
		this.groupId = StringUtils.trim(p.getProperty("groupId"));
	}
}
