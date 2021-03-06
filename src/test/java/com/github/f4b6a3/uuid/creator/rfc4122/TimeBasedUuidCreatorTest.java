package com.github.f4b6a3.uuid.creator.rfc4122;

import org.junit.Test;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.creator.AbstractTimeBasedUuidCreator;
import com.github.f4b6a3.uuid.creator.AbstractUuidCreatorTest;
import com.github.f4b6a3.uuid.creator.rfc4122.TimeBasedUuidCreator;
import com.github.f4b6a3.uuid.strategy.ClockSequenceStrategy;
import com.github.f4b6a3.uuid.strategy.NodeIdentifierStrategy;
import com.github.f4b6a3.uuid.strategy.clockseq.DefaultClockSequenceStrategy;
import com.github.f4b6a3.uuid.strategy.clockseq.FixedClockSequenceStrategy;
import com.github.f4b6a3.uuid.strategy.nodeid.FixedNodeIdentifierStrategy;
import com.github.f4b6a3.uuid.strategy.timestamp.FixedTimestampStretegy;
import com.github.f4b6a3.uuid.util.UuidTimeUtil;
import com.github.f4b6a3.uuid.util.UuidUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

public class TimeBasedUuidCreatorTest extends AbstractUuidCreatorTest {

	@Test
	public void testCreateTimeBasedUuid() {
		boolean multicast = true;
		testCreateAbstractTimeBasedUuid(UuidCreator.getTimeBasedCreator(), multicast);
	}

	@Test
	public void testCreateTimeBasedUuidWithMac() {
		boolean multicast = false;
		testCreateAbstractTimeBasedUuid(UuidCreator.getTimeBasedCreator().withMacNodeIdentifier(), multicast);
	}

	@Test
	public void testGetTimeBasedUuidStringIsValid() {
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			UUID uuid = UuidCreator.getTimeBased();
			checkIfStringIsValid(uuid);
		}
	}

	@Test
	public void testGetTimeBasedWithMacStringIsValid() {
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			UUID uuid = UuidCreator.getTimeBasedWithMac();
			checkIfStringIsValid(uuid);
		}
	}

	@Test
	public void testGetTimeBasedWithCustomNodeIdentifierStrategy() {

		NodeIdentifierStrategy customStrategy = new FixedNodeIdentifierStrategy(System.nanoTime());
		long nodeIdentifier1 = customStrategy.getNodeIdentifier();

		AbstractTimeBasedUuidCreator creator = UuidCreator.getTimeBasedCreator()
				.withNodeIdentifierStrategy(customStrategy);

		UUID uuid = creator.create();
		long nodeIdentifier2 = UuidUtil.extractNodeIdentifier(uuid);

		assertEquals(nodeIdentifier1, nodeIdentifier2);
	}

	@Test
	public void testGetTimeBasedWithCustomClockSequenceStrategy() {

		ClockSequenceStrategy customStrategy = new FixedClockSequenceStrategy((int) System.nanoTime());
		long clockseq1 = customStrategy.getClockSequence(0);

		AbstractTimeBasedUuidCreator creator = UuidCreator.getTimeBasedCreator()
				.withClockSequenceStrategy(customStrategy);

		UUID uuid = creator.create();
		long clockseq2 = UuidUtil.extractClockSequence(uuid);

		assertEquals(clockseq1, clockseq2);
	}

	@Test
	public void testGetTimeBasedTimestampIsCorrect() {

		TimeBasedUuidCreator creator = UuidCreator.getTimeBasedCreator();

		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {

			Instant instant1 = Instant.now();

			UUID uuid = creator.withTimestampStrategy(new FixedTimestampStretegy(instant1)).create();
			Instant instant2 = UuidUtil.extractInstant(uuid);

			long timestamp1 = UuidTimeUtil.toTimestamp(instant1);
			long timestamp2 = UuidTimeUtil.toTimestamp(instant2);

			assertEquals(timestamp1, timestamp2);
		}
	}

	@Test
	public void testGetTimeBasedShouldCreateAlmostSixteenThousandUniqueUuidsWithTheTimeStopped() {

		int max = 0x3fff + 1; // 16,384
		Instant instant = Instant.now();
		HashSet<UUID> set = new HashSet<>();

		// Reset the static ClockSequenceController
		// It could affect this test case
		DefaultClockSequenceStrategy.CONTROLLER.clearPool();

		// Instantiate a factory with a fixed timestamp, to simulate a request
		// rate greater than 16,384 per 100-nanosecond interval.
		TimeBasedUuidCreator creator = UuidCreator.getTimeBasedCreator()
				.withTimestampStrategy(new FixedTimestampStretegy(instant));

		int firstClockSeq = 0;
		int lastClockSeq = 0;

		// Try to create 16,384 unique UUIDs
		for (int i = 0; i < max; i++) {
			UUID uuid = creator.create();
			if (i == 0) {
				firstClockSeq = UuidUtil.extractClockSequence(uuid);
			} else if (i == max - 1) {
				lastClockSeq = UuidUtil.extractClockSequence(uuid);
			}

			// Fail if the insertion into the hash set returns false, indicating
			// that there's a duplicate UUID.
			assertTrue(DUPLICATE_UUID_MSG, set.add(uuid));
		}

		assertTrue(DUPLICATE_UUID_MSG, set.size() == max);
		assertTrue("The last clock sequence should be equal to the first clock sequence minus 1",
				(lastClockSeq % max) == ((firstClockSeq % max) - 1));
	}

	@Test
	public void testCreateTimeBasedUuidTheGreatestDateAndTimeShouldBeAtYear5236() {

		// Check if the greatest 60 bit timestamp corresponds to the date and
		// time
		long timestamp0 = 0x0fffffffffffffffL;
		Instant instant0 = Instant.parse("5236-03-31T21:21:00.684697500Z");
		assertEquals(UuidTimeUtil.toInstant(timestamp0), instant0);

		// Test the extraction of the maximum 60 bit timestamp
		long timestamp1 = 0x0fffffffffffffffL;
		TimeBasedUuidCreator creator1 = UuidCreator.getTimeBasedCreator()
				.withTimestampStrategy(new FixedTimestampStretegy(timestamp1));
		UUID uuid1 = creator1.create();
		long timestamp2 = UuidUtil.extractTimestamp(uuid1);
		assertEquals(timestamp1, timestamp2);

		// Test the extraction of the maximum date and time
		TimeBasedUuidCreator creator2 = UuidCreator.getTimeBasedCreator()
				.withTimestampStrategy(new FixedTimestampStretegy(timestamp0));
		UUID uuid2 = creator2.create();
		Instant instant2 = UuidUtil.extractInstant(uuid2);
		assertEquals(instant0, instant2);
	}

	@Test
	public void testGetTimeBasedParallelGeneratorsShouldCreateUniqueUuids() throws InterruptedException {

		Thread[] threads = new Thread[THREAD_TOTAL];
		TestThread.clearHashSet();

		// Instantiate and start many threads
		for (int i = 0; i < THREAD_TOTAL; i++) {
			threads[i] = new TestThread(UuidCreator.getTimeBasedCreator(), DEFAULT_LOOP_MAX);
			threads[i].start();
		}

		// Wait all the threads to finish
		for (Thread thread : threads) {
			thread.join();
		}

		// Check if the quantity of unique UUIDs is correct
		assertTrue(DUPLICATE_UUID_MSG, TestThread.hashSet.size() == (DEFAULT_LOOP_MAX * THREAD_TOTAL));
	}
}
