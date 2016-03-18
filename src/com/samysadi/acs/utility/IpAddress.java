/*
===============================================================================
Copyright (c) 2014-2015, Samy Sadi. All rights reserved.
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

This file is part of ACS - Advanced Cloud Simulator.

ACS is part of a research project undertaken by
Samy Sadi (samy.sadi.contact@gmail.com) and supervised by
Belabbas Yagoubi (byagoubi@gmail.com) in the
University of Oran1 Ahmed Benbella, Algeria.

ACS is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3
as published by the Free Software Foundation.

ACS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ACS. If not, see <http://www.gnu.org/licenses/>.
===============================================================================
*/

package com.samysadi.acs.utility;

import com.samysadi.acs.utility.collections.Bitmap;

/**
 * An object to save Ip Address information.
 *
 * <p>Ip addresses are saved in a long (8 bytes).
 *
 * @since 1.0
 */
public class IpAddress {
	private static final Bitmap usedIpAddresses = new Bitmap();
	private static long ipAddressCounter = 1l;
	public static final IpAddress ipMin = new IpAddress(0l);
	public static final IpAddress ipMax = new IpAddress(Bitmap.MAX_BITMAP_SIZE-1);
	private Long ip;

	public IpAddress(Long ip) {
		super();
		this.ip = ip;
		usedIpAddresses.mark(this.ip, 1l);
	}

	public IpAddress(IpAddress o) {
		this(o.ip);
	}
	public IpAddress(long ip) {
		this(Long.valueOf(ip));
	}

	public IpAddress(int ip0, int ip1) {
		this(Long.valueOf(((ip0 & 0xffffffffl) << 32) + (ip1 & 0xffffffffl)));
	}

	private static Long arrayOfByteToLong(byte[] ip) {
		if (ip.length == 0)
			return 0l;
		else {
			if (ip.length < 8) {
				byte[] ipp = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
				int k = ip.length - ipp.length;
				for (int i = 0; i < ipp.length; i++) {
					if (k >= 0)
						ipp[i] = ip[k];
					else
						ipp[i] = 0;
					k++;
				}
				ip = ipp;
			}

			return Long.valueOf(
					((ip[0] & 0xffl) << 56) +
					((ip[1] & 0xffl) << 48) +
					((ip[2] & 0xffl) << 40) +
					((ip[3] & 0xffl) << 32) +
					((ip[4] & 0xffl) << 24) +
					((ip[5] & 0xffl) << 16) +
					((ip[6] & 0xffl) << 8) +
					((ip[7] & 0xffl))
				);
		}
	}

	public IpAddress(byte... ip) {
		this(arrayOfByteToLong(ip));
	}

	public Long getValue() {
		return ip;
	}

	@Override
	public int hashCode() {
		return ip.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		else if (!(obj instanceof IpAddress))
			return false;
		else
			return this.ip.equals(((IpAddress) obj).ip);
	}

	public static boolean isSameSubnet(IpAddress ip1, IpAddress ip2,
			IpAddress mask) {
		long m1 = ip1.getValue().longValue() & mask.getValue().longValue();
		long m2 = ip2.getValue().longValue() & mask.getValue().longValue();
		return m1 == m2;
	}

	private static int[] getIPv6Parts(String addr) {
		if (addr.isEmpty())
			return new int[0];
		String[] sa = addr.split(":");

		int[] parts = new int[sa.length];
		for (int i = 0; i<parts.length; i++)
			parts[i] = Integer.parseInt(sa[i], 16);

		return parts;
	}

	public static IpAddress fromString(String addr) {
		addr = addr.trim();
		try {
			if (addr.indexOf('.') >= 0) {
				String[] sa = addr.split("\\.");
				byte[] b = new byte[Math.min(8, sa.length)];

				int i = 0;
				for (String ss : sa) {
					b[i] = (byte) Integer.parseInt(ss);
					i++;
					if (i >= b.length)
						break;
				}

				return new IpAddress(b);
			} else if (addr.indexOf(':') >= 0) {
				int[] parts;

				String[] addrs = addr.split("::");
				if (addr.endsWith("::") && (addrs.length == 1))
					addrs = new String[] {addrs[0], ""};
				if (addrs.length == 1)
					parts = getIPv6Parts(addrs[0]);
				else if (addrs.length > 1) {
					int[] parts0 = getIPv6Parts(addrs[0]);
					int[] parts1 = getIPv6Parts(addrs[1]);
					if (parts0.length == 0)
						parts = parts1;
					else {
						parts = new int[]{0,0,0,0,0,0,0,0};

						{
							int i = 0;
							for (int j=0;j<parts0.length;j++) {
								parts[i] = parts0[j];
								i++;
								if (i>parts.length)
									break;
							}
						}

						{
							int i = parts.length - 1;
							for (int j=parts1.length-1;j>=0;j--) {
								parts[i] = parts1[j];
								i--;
								if (i<0)
									break;
							}
						}
					}
				} else
					parts = new int[0];

				if (parts.length < 8) {
					int[] p = new int[]{0,0,0,0,0,0,0,0};
					int j = p.length;
					for (int i=parts.length - 1; i>=0; i--)
						p[--j] = parts[i];
					parts = p;
				}

				long long0 = parts[0];
				for (int i = 1; i < 4; i++) {
					long0 = (long0 << 16) + parts[i];
				}

				if (long0 != 0)
					throw new IllegalArgumentException("Only IPv6 addresses with a maximum of 64bit length are supported");

				long long1 = parts[4];
				for (int i = 5; i < 8; i++) {
					long1 = (long1 << 16) + parts[i];
				}
				return new IpAddress(long1);
			}
			return new IpAddress(Long.valueOf(addr));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public String toString() {
	    long l = ip;

	    long[] longs = new long[4];
	    for (int i=longs.length-1; i>=0; i--) {
	    	longs[i] = l & 0xFFFF;
	    	l = l >> 16;
	    }

	    StringBuilder ipString = new StringBuilder();
	    int k = 0;
	    while ((k<longs.length-1) && (longs[k] == 0))
	    	k++;
	    ipString.append("::");
	    for (int i = k; i<longs.length; i++) {
	    	ipString.append(Long.toHexString(longs[i]));
	    	ipString.append(":");
	    }
	    ipString.deleteCharAt(ipString.length()-1);

	    return ipString.toString();
	}

	/**
	 * Generates and returns an IP address.
	 *
	 * <p>Calling this ensures you that you will get a new unused  {@link IpAddress}.
	 *
	 * @return a {@link IpAddress}
	 */
	public static IpAddress newIpAddress() {
		while (usedIpAddresses.isMarked(ipAddressCounter))
			ipAddressCounter++;

		return new IpAddress(ipAddressCounter);
	}
}
