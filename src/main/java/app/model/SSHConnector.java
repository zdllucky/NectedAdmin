package app.model;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;

public class SSHConnector {
	private String user;
	private String host;
	private String password;
	private int port;
	private JSch jsch;
	private Session session;

	public SSHConnector(String host, String password) {
		this.user = "root";
		this.host = host;
		this.password = password;
		this.port = 22;
		this.jsch = new JSch();
	}

	//Checking server connectivity
	public void checkConnection() throws JSchException {
		jsch.setKnownHosts(host);
		session = jsch.getSession(user, host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect(5000);
		if (session.isConnected())
			session.disconnect();
	}

	//Connect to a server
	void connect() throws JSchException {
		jsch.setKnownHosts(host);
		session = jsch.getSession(user,
				host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect(10000);
		session.isConnected();
	}

	//Send command to a connected server
	void sendCommand(String command) throws JSchException, IOException {
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);
		InputStream commandOutput = channel.getInputStream();
		channel.connect();
		while (channel.getExitStatus() == -1 && session.isConnected()) {
			commandOutput.readAllBytes();
		}
		int status = channel.getExitStatus();
		channel.disconnect();
		if (status != 0) throw new JSchException();
	}

	//Close connection
	public void close() {
		session.disconnect();
	}
}
