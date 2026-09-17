package org.jmxtrans.embedded;

import org.jmxtrans.embedded.util.network.MacAddressUtils;

import junit.framework.TestCase;

public class MacAddressUtils_Test extends TestCase {

	public void testMacAddress_1() {
		for (String mac : MacAddressUtils.getAllMacAddresses()) {
			System.out.println("Mac Address is : "+ mac);
		}
	}

	public void testRemoteMacAddr_2() {
		System.out.println("MacAddress is : " + MacAddressUtils.getMacAddress());
	}

	// 原有 testRemoteMacAddr_3/4 依赖开发者局域网地址 192.168.31.54，在 CI 环境必然失败，已移除。

}
