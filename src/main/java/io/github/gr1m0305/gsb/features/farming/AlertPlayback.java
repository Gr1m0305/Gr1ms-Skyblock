package io.github.gr1m0305.gsb.features.farming;

/** The one second an alert lasts: its text is shown the whole time and a ding plays every few ticks. */
public final class AlertPlayback {
	static final int DURATION_TICKS = 20;
	static final int DING_INTERVAL_TICKS = 4;

	private int ticksLeft;

	public void start() {
		ticksLeft = DURATION_TICKS;
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

		boolean ding = (DURATION_TICKS - ticksLeft) % DING_INTERVAL_TICKS == 0;
		ticksLeft--;
		return ding;
	}
}
