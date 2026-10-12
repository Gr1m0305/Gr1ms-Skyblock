package io.github.gr1m0305.gsb.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberFormatsTest {
	@Test
	void groupsThousands() {
		assertEquals("76,549", NumberFormats.grouped(76_549L));
		assertEquals("557,532,196", NumberFormats.grouped(557_532_196.4));
		assertEquals("0", NumberFormats.grouped(0L));
		assertEquals("-3,200", NumberFormats.grouped(-3_200L));
	}

	@Test
	void compactsLikeTheSkyHanniTrackers() {
		assertEquals("41.9M", NumberFormats.compact(41_900_000));
		assertEquals("34M", NumberFormats.compact(34_000_000));
		assertEquals("18.8M", NumberFormats.compact(18_840_000));
		assertEquals("-3.2M", NumberFormats.compact(-3_200_000));
		assertEquals("1.5k", NumberFormats.compact(1_500));
		assertEquals("950", NumberFormats.compact(950.2));
		assertEquals("0", NumberFormats.compact(0));
		assertEquals("2.1B", NumberFormats.compact(2_100_000_000d));
	}

	@Test
	void compactMovesToTheNextUnitWhenRoundingUp() {
		assertEquals("1M", NumberFormats.compact(999_960));
		assertEquals("999.9k", NumberFormats.compact(999_940));
	}

	@Test
	void formatsDegrees() {
		assertEquals("-141.15", NumberFormats.degrees(-141.1532f));
		assertEquals("3.30", NumberFormats.degrees(3.3f));
		assertEquals("90.00", NumberFormats.degrees(90f));
		assertEquals("0.00", NumberFormats.degrees(-0.001f));
		assertEquals("-0.01", NumberFormats.degrees(-0.006f));
	}

	@Test
	void formatsDurations() {
		assertEquals("1d 15h 35m 19s", NumberFormats.duration(((24 + 15) * 3600 + 35 * 60 + 19) * 1000L));
		assertEquals("0s", NumberFormats.duration(0));
		assertEquals("59s", NumberFormats.duration(59_999));
		assertEquals("1h 0m 5s", NumberFormats.duration(3_605_000));
		assertEquals("2m 0s", NumberFormats.duration(120_000));
	}
}
