package app.model;

import app.entities.Server;
import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.sql.SQLException;

public class ServerConfigurer implements Runnable {
	private final Server server;

	public ServerConfigurer(Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		try {
			String ipsecpsk = RandomStringUtils.randomAlphanumeric(20);
			Model.lockServer(server.getId());
			SSHConnector connector = new SSHConnector(server.getIp_addr(), server.getConn());
			connector.connect();
			connector.sendCommand("wget https://raw.githubusercontent.com/zdllucky/setup-ipsec-vpn/master/vpnsetup.sh -O vpnsetup.sh && " +
					"sudo VPN_IPSEC_PSK='" + ipsecpsk + "' VPN_USER='conf' VPN_PASSWORD='testtest228' sh vpnsetup.sh");
			connector.close();
			Model.unlockServer(server.getId());
			DbHandler.getInstance().editServer(server.getId(),
					server.getIp_addr(),
					server.getConn(),
					server.getCountry(),
					ipsecpsk,
					server.getUsersLimit(),
					Server.State.RUNNING);
			Logger.getInstance().add("Server VPN software deployment", Logger.INFO, "Server ID#" + server.getId() + ", country: " + server.getCountry());
		} catch (JSchException | IOException | SQLException | InterruptedException e) {

			Logger.getInstance().add("Server VPN software deployment", Logger.ERROR, "Server ID#" + server.getId() + ", country: " + server.getCountry() + ", " + Logger.parseException(e));
			Model.unlockServer(server.getId());
			try {
				DbHandler.getInstance().editServer(server.getId(),
						server.getIp_addr(),
						server.getConn(),
						server.getCountry(),
						server.getIpSecPSK(),
						server.getUsersLimit(),
						Server.State.NOT_SET_UP);
			} catch (SQLException ex) {
				Logger.getInstance().add("!!!Server VPN software deployment MEGA exception", Logger.ERROR, "Server ID#" + server.getId() + ", country: " + server.getCountry() + ", " + Logger.parseException(ex));
			}
		}
	}
}
