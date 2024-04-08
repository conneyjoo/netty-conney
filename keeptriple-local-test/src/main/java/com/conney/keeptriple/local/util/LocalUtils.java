package com.conney.keeptriple.local.util;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class LocalUtils {

    public static String getIp() {
        return getIp("");
    }

    public static String getIp(String face) {
        return getLocalInetAddress(face).getHostAddress();
    }

    public static InetAddress getLocalInetAddress(String face) {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress candidateAddress = null;

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                if (StringUtils.isEmpty(face) || networkInterface.getDisplayName().startsWith(face)) {
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();

                        if (!inetAddress.isLoopbackAddress()) {
                            if (inetAddress.isSiteLocalAddress()) {
                                return inetAddress;
                            } else {
                                candidateAddress = inetAddress;
                            }
                        }
                    }
                }
            }

            if (candidateAddress != null) {
                return candidateAddress;
            }

            candidateAddress = InetAddress.getLocalHost();
            return candidateAddress;
        } catch (IOException e) {
            return null;
        }
    }
}
