/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmxtrans.embedded.util.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-platform MAC-address lookup utilities.
 *
 * <p>The class supports three flavours of MAC lookup:</p>
 * <ol>
 *   <li><strong>Local machine</strong> &mdash; implemented by dispatching to
 *       either {@link #getWindowXPMacAddress(String)},
 *       {@link #getWindow7MacAddress()}, {@link #getLinuxMacAddress()} or
 *       {@link #getUnixMacAddress()} depending on the value of
 *       {@code os.name}.</li>
 *   <li><strong>By IP</strong> &mdash; implemented by resolving the interface
 *       bound to a given hostname / IP and reading the hardware address from
 *       the resulting {@link NetworkInterface}.</li>
 *   <li><strong>All interfaces</strong> &mdash; via
 *       {@link #getAllMacAddresses()}.</li>
 * </ol>
 *
 * <p>The legacy {@link #getRemoteMacAddr(String)} implementation speaks a
 * &quot;UDP-NetBIOS-NS&quot; query packet and is preserved for compatibility
 * with environments where the local lookup is impossible. Note that this
 * approach only works against a co-operative remote host that exposes the
 * NetBIOS name service on UDP port 137.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */
public class MacAddressUtils {

    /** SLF4J logger, shared by all helper methods. */
	protected static Logger LOG = LoggerFactory.getLogger(MacAddressUtils.class);

    /** Placeholder returned when a lookup yields no usable answer. */
	protected static String UNKNOWN_MAC_ADDRESS = "Unknown Mac Address";

    /** NetBIOS-NS name-service port used by {@link #getRemoteMacAddr(String)}. */
	private static int remotePort = 137;

    /** Static receive buffer shared by all incoming NetBIOS-NS packets. */
	private static byte[] buffer = new byte[1024];

    /** Singleton UDP socket used to talk NetBIOS-NS. */
	private static DatagramSocket ds = null;

	static{
		try {
			ds = new DatagramSocket();
		} catch (SocketException e) {

		}
	}

    /**
     * Sends a datagram packet to the configured remote address.
     *
     * @param remoteAddr the NetBIOS-NS peer address (hostname or IP).
     * @param bytes      the datagram payload (typically a query packet from
     *                   {@link #getQueryCmd()}).
     * @return the {@link DatagramPacket} that was sent.
     * @throws IOException if the underlying socket fails to send.
     */
	// 发送数据包
	protected static final DatagramPacket send(String remoteAddr,byte[] bytes) throws IOException {
		DatagramPacket dp = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(remoteAddr), remotePort);
		ds.send(dp);
		return dp;
	}

    /**
     * Receives a single datagram using a three-second read timeout. Any
     * timeout, socket or I/O failure is logged at {@code ERROR} and an
     * empty packet is returned so callers can attempt to extract whatever
     * data was already buffered.
     *
     * @return the received packet; never {@code null}.
     */
	// 接收数据包
	protected static final DatagramPacket receive() {
		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
		try {
			ds.setSoTimeout(3000);
			ds.receive(dp);
		} catch (SocketTimeoutException ex) {
			LOG.error("接收数据超时...,不能获取客户端MAC地址",ex);
		} catch (SocketException e1) {
			LOG.error("发生Sorcket异常...",e1);
		} catch (IOException e2) {
			LOG.error("发生IO异常...", e2);
		}
		return dp;
	}

    /**
     * Builds the canonical 50-byte NetBIOS-NS query packet. The packet
     * encodes a Name Service query for a name consisting of 30
     * {@code 0x41} (&quot;A&quot;) bytes padded with the
     * {@code 0x20 0x43 0x4B} length prefix.
     *
     * @return the freshly-built 50-byte query command.
     * @throws Exception never thrown but kept for cross-version safety.
     */
	protected static final byte[] getQueryCmd() throws Exception {
		byte[] t_ns = new byte[50];
		t_ns[0] = 0x00;
		t_ns[1] = 0x00;
		t_ns[2] = 0x00;
		t_ns[3] = 0x10;
		t_ns[4] = 0x00;
		t_ns[5] = 0x01;
		t_ns[6] = 0x00;
		t_ns[7] = 0x00;
		t_ns[8] = 0x00;
		t_ns[9] = 0x00;
		t_ns[10] = 0x00;
		t_ns[11] = 0x00;
		t_ns[12] = 0x20;
		t_ns[13] = 0x43;
		t_ns[14] = 0x4B;

		for (int i = 15; i < 45; i++) {
			t_ns[i] = 0x41;
		}

		t_ns[45] = 0x00;
		t_ns[46] = 0x00;
		t_ns[47] = 0x21;
		t_ns[48] = 0x00;
		t_ns[49] = 0x01;
		return t_ns;
	}

    /**
     * Extracts the 6-byte MAC address from a NetBIOS-NS response packet.
     *
     * @param brevdata the raw response bytes.
     * @return the MAC address formatted as six upper-case hex digits joined
     *         with {@code -} separators.
     * @throws Exception if the buffer is shorter than expected for the
     *                   declared number of NetBIOS names.
     */
	protected static final String getMacAddr(byte[] brevdata) throws Exception {
		int i = brevdata[56] * 18 + 56;
		String sAddr = "";
		StringBuffer sb = new StringBuffer(17);
		for (int j = 1; j < 7; j++) {
			sAddr = Integer.toHexString(0xFF & brevdata[i + j]);
			if (sAddr.length() < 2) {
				sb.append(0);
			}
			sb.append(sAddr.toUpperCase());
			if (j < 6){
				sb.append('-');
			}
		}
		return sb.toString();
	}

    /**
     * Closes the static NetBIOS-NS socket. Errors during close are logged
     * but otherwise ignored so that this method can be safely used as a
     * JVM shutdown helper.
     */
	public static final void close() {
		try {
			ds.close();
		} catch (Exception ex) {
			LOG.error(ex.getMessage());
		}
	}

    /**
     * Legacy NetBIOS-NS based remote MAC lookup. Sends the query command
     * from {@link #getQueryCmd()}, awaits the response and parses the
     * MAC from the reply. If anything fails, returns
     * {@link #UNKNOWN_MAC_ADDRESS} after logging the cause.
     *
     * @param remoteIPAddr the target host address.
     * @return the resolved MAC address, or
     *         {@link #UNKNOWN_MAC_ADDRESS} if no answer was received in
     *         three seconds.
     */
	public static final String getRemoteMacAddr(String remoteIPAddr) {
		try {
			byte[] bqcmd = getQueryCmd();
			send(remoteIPAddr,bqcmd);
			DatagramPacket dp = receive();
			String smac = "";
			smac = getMacAddr(dp.getData());
			close();
			return smac;
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		return UNKNOWN_MAC_ADDRESS;
	}

    /**
     * Returns the lower-case name of the operating system the JVM is
     * currently running on.
     *
     * @return the value of {@code os.name} converted to lower case, never
     *         {@code null}.
     */
	public static String getOSName() {
		return System.getProperty("os.name").toLowerCase();
	}

    /**
     * Resolves the MAC address of a Windows XP machine by parsing the
     * output of the {@code ipconfig /all} command. Lines containing
     * &quot;本地连接&quot; (the Chinese label for local area connection) are
     * skipped to avoid virtual adapters.
     *
     * @param execStr the {@code ipconfig /all} invocation string.
     * @return the first matching MAC address, or {@code null} if none was
     *         found.
     */
	public static String getWindowXPMacAddress(String execStr) {
		String mac = null;
		BufferedReader reader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(execStr);
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			int index = -1;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf("本地连接") != -1){
					continue;
				}
				index = line.toLowerCase().indexOf("physical address");
				if (index != -1) {
					index = line.indexOf(":");
					if (index != -1) {
						mac = line.substring(index + 1).trim();
					}
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				process.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			reader = null;
			process = null;
		}
		return mac;
	}

    /**
     * Resolves the MAC address of a Windows Vista / 7 / 2008 machine by
     * querying the {@link NetworkInterface} registered against the local
     * host.
     *
     * @return the MAC address formatted with {@code -} separators, in upper
     *         case.
     */
	public static String getWindow7MacAddress() {
		byte[] mac = null;
		try {
			mac = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
		} catch (SocketException e) {
			e.printStackTrace();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mac.length; i++) {
			if (i != 0) {
				sb.append("-");
			}
			String s = Integer.toHexString(mac[i] & 0xFF);
			sb.append(s.length() == 1 ? 0 + s : s);
		}
		return sb.toString().toUpperCase();
	}

    /**
     * Resolves the MAC address associated with the given hostname or IP by
     * querying {@link NetworkInterface}. Returns {@code null} if the host is
     * unknown or has no associated hardware address.
     *
     * @param host the hostname or IP address.
     * @return the MAC address formatted with {@code -} separators, or
     *         {@code null} when the address cannot be resolved.
     */
	public static String getHostMacAddress(String host) {

		byte[] mac = null;
		try {
			mac = NetworkInterface.getByInetAddress(InetAddress.getByName(host)).getHardwareAddress();
		} catch (SocketException e) {
			e.printStackTrace();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (mac == null || mac.length == 0) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mac.length; i++) {
			if (i != 0) {
				sb.append("-");
			}
			String s = Integer.toHexString(mac[i] & 0xFF);
			sb.append(s.length() == 1 ? 0 + s : s);
		}
		return sb.toString().toUpperCase();
	}

    /**
     * Returns the MAC address of every non-empty {@link NetworkInterface}
     * on the local host. Interfaces without a hardware address (for
     * example loopback) are skipped.
     *
     * @return a possibly empty list of MAC addresses formatted with
     *         {@code -} separators, in upper case.
     */
	public static List<String> getAllMacAddresses() {
		List<String> addresses = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = networkInterfaces.nextElement();
				byte[] mac = netInterface.getHardwareAddress();
				if (mac != null && mac.length != 0) {
					sb.delete(0, sb.length());
					for (int i = 0; i < mac.length; i++) {
						if (i != 0) {
							sb.append("-");
						}
						String s = Integer.toHexString(mac[i] & 0xFF);
						sb.append(s.length() == 1 ? 0 + s : s);
					}
					addresses.add(sb.toString().toUpperCase());
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return addresses;
	}

    /**
     * Linux-only MAC lookup that shells out to {@code ifconfig eth0} and
     * parses the line containing &quot;硬件地址&quot; (the Chinese label for
     * "hardware address").
     *
     * @return the MAC address or {@code null} if it cannot be parsed.
     */
	public static String getLinuxMacAddress() {
		String mac = null;
		BufferedReader reader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("ifconfig eth0");
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			int index = -1;
			while ((line = reader.readLine()) != null) {
				index = line.toLowerCase().indexOf("硬件地址");
				if (index != -1) {
					mac = line.substring(index + 4).trim();
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				process.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			reader = null;
			process = null;
		}
		return mac;
	}

    /**
     * Unix-only MAC lookup that shells out to {@code ifconfig eth0} and
     * parses the line containing {@code hwaddr}.
     *
     * @return the MAC address or {@code null} if it cannot be parsed.
     */
	public static String getUnixMacAddress() {
		String mac = null;
		BufferedReader reader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("ifconfig eth0");
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			int index = -1;
			while ((line = reader.readLine()) != null) {
				index = line.toLowerCase().indexOf("hwaddr");
				if (index != -1) {
					mac = line.substring(index + "hwaddr".length() + 1).trim();
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				process.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			reader = null;
			process = null;
		}

		return mac;
	}

    /**
     * Auto-detecting entry-point used by callers that don't know which
     * operating system they are running on. Dispatches to one of
     * {@link #getWindowXPMacAddress(String)},
     * {@link #getWindow7MacAddress()}, {@link #getLinuxMacAddress()} or
     * {@link #getUnixMacAddress()} based on the value of
     * {@link #getOSName()}.
     *
     * @return the MAC address or {@code null} if it could not be resolved.
     */
	public static String getMacAddress() {
		String os = getOSName();
		String mac = null;
		if (os.startsWith("windows")) {
			String execStr = getSystemRoot() + "/system32/ipconfig /all";
			if (os.equals("windows xp")) {
				mac = getWindowXPMacAddress(execStr);
			} else if (os.equals("windows 2003")) {
				mac = getWindowXPMacAddress(execStr);
			} else {
				mac = getWindow7MacAddress();
			}
		} else if (os.startsWith("linux")) {
			mac = getLinuxMacAddress();
		} else {
			mac = getUnixMacAddress();
		}
		return mac;
	}

    /**
     * Resolves the value of the {@code windir} (a.k.a. {@code SystemRoot})
     * environment variable by shelling out to {@code cmd /c SET} on Windows
     * or {@code env} on other platforms.
     *
     * @return the value of the {@code windir} environment variable or
     *         {@code null} if it cannot be read.
     */
	public static String getSystemRoot() {
		String cmd = null;
		String os = null;
		String result = null;
		String envName = "windir";
		os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith("windows")) {
			cmd = "cmd /c SET";
		} else {
			cmd = "env";
		}
		BufferedReader reader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.toLowerCase();
				if (line.indexOf(envName) > -1) {
					result = line.substring(line.indexOf(envName) + envName.length() + 1);
					return result;
				}
			}
		} catch (Exception e) {
			LOG.error("获取系统命令路径 error: " + cmd + ":", e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				process.destroy();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			reader = null;
			process = null;
		}
		return null;
	}

}
