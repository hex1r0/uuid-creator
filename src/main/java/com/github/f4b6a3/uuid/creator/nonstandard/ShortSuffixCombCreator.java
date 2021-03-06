/*
 * MIT License
 * 
 * Copyright (c) 2018-2019 Fabio Lima
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.f4b6a3.uuid.creator.nonstandard;

import java.security.SecureRandom;
import java.util.UUID;

import com.github.f4b6a3.uuid.util.ByteUtil;
import com.github.f4b6a3.uuid.util.RandomUtil;
import com.github.f4b6a3.uuid.creator.AbstractRandomBasedUuidCreator;
import com.github.f4b6a3.uuid.enums.UuidVersion;

/**
 * Factory that creates Suffix COMB GUIDs.
 * 
 * A Suffix COMB GUID is a UUID that combines a creation time with random bits.
 * 
 * The creation minute is a 2 bytes SUFFIX at the LEAST significant bits.
 * 
 * The suffix wraps around every ~45 days (2^16/60/24 = ~45).
 * 
 * Read: Sequential UUID Generators
 * https://www.2ndquadrant.com/en/blog/sequential-uuid-generators/
 * 
 */
public class ShortSuffixCombCreator extends AbstractRandomBasedUuidCreator {

	protected static final int ONE_MINUTE = 60_000;

	public ShortSuffixCombCreator() {
		super(UuidVersion.VERSION_RANDOM_BASED);
	}

	/**
	 * Return a Suffix COMB GUID.
	 * 
	 * It combines a creation time with random bits.
	 * 
	 * The creation minute is a 2 bytes SUFFIX at the LEAST significant bits.
	 * 
	 * The suffix wraps around every ~45 days (2^16/60/24 = ~45).
	 */
	@Override
	public synchronized UUID create() {

		final short timestamp = (short) (System.currentTimeMillis() / ONE_MINUTE);

		long msb;
		long lsb;

		// Get random values for MSB and LSB
		if (random == null) {
			final byte[] bytes = new byte[14];
			RandomUtil.get().nextBytes(bytes);
			msb = ByteUtil.toNumber(bytes, 0, 8);
			lsb = ByteUtil.toNumber(bytes, 8, 14);
		} else {
			if (this.random instanceof SecureRandom) {
				final byte[] bytes = new byte[14];
				this.random.nextBytes(bytes);
				msb = ByteUtil.toNumber(bytes, 0, 8);
				lsb = ByteUtil.toNumber(bytes, 8, 14);
			} else {
				msb = this.random.nextLong();
				lsb = this.random.nextLong();
			}
		}

		// Insert the short suffix in the LSB
		lsb = ((lsb & 0x0000ffff00000000L) << 16) | (lsb & 0x00000000ffffffffL)
				| ((timestamp & 0x000000000000ffffL) << 32);

		// Set the version and variant bits
		return new UUID(applyVersionBits(msb), applyVariantBits(lsb));
	}
}
