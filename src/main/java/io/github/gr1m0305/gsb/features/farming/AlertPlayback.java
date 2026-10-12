package io.github.gr1m0305.gsb.features.farming;

/** The time an alert lasts: its text is shown the whole time and a ding plays every few ticks. */
public final class AlertPlayback {
	static final int TICKS_PER_SECOND = 20;
	static final int DING_INTERVAL_TICKS = 4;

	private int durationTicks;
	private int ticksLeft;

	/**
	 * @param seconds how long the alert lasts
	 */
	public void start(float seconds) {
		durationTicks = Math.max(1, Math.round(seconds * TICKS_PER_SECOND));
		ticksLeft = durationTicks;
	}

	public boolean isActive() {
		return ticksLeft > 0;
	}

	/**
	 * Advances the alert by one tick.
	 *
	 * @return whether a ding should be played in this tick
	 */
	public boolean tick() {
		if (ticksLeft <= 0) {
			return false;
		}

		boolean ding = (durationTicks - ticksLeft) % DING_INTERVAL_TICKS == 0;
		ticksLeft--;
		return ding;
	}
}
