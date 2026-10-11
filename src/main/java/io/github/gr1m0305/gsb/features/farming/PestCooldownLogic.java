package io.github.gr1m0305.gsb.features.farming;

/**
 * Decides when the pest cooldown has run out, kept free of Minecraft classes.
 *
 * <p>The tab list is the only source for the cooldown and it does not have to be refreshed every second. So
 * for the last few seconds the countdown is continued here, and the tab list showing READY is the fallback.
 */
public final class PestCooldownLogic {
	/** With this many seconds or fewer on the tab list, the rest is counted down here. */
	static final int LOCAL_COUNTDOWN_SECONDS = 10;
	/** A cooldown longer than this cannot be the rest of the one that just ran out. */
	static final int NEW_COOLDOWN_SECONDS = 15;

	private static final long NOT_SET = Long.MIN_VALUE;

	private boolean armed;
	private boolean ranOutLocally;
	private int shownSeconds = -1;
	private long readyAt = NOT_SET;

	/** Forgets everything, for when the cooldown cannot be seen anymore. */
	public void reset() {
		armed = false;
		ranOutLocally = false;
		shownSeconds = -1;
		readyAt = NOT_SET;
	}

	/**
	 * The tab list was read.
	 *
	 * @return whether the cooldown has just run out
	 */
	public boolean tabListRead(PestCooldownParser.Cooldown cooldown, long now) {
		switch (cooldown.state()) {
			case COUNTING -> {
				// Only a changed number says something about when the second started.
				if (cooldown.seconds() != shownSeconds) {
					shownSeconds = cooldown.seconds();
					readyAt = now + cooldown.seconds() * 1000L;
				}

				if (cooldown.seconds() > NEW_COOLDOWN_SECONDS) {
					ranOutLocally = false;
				}

				// The tab list can still show the last second after the countdown here is done.
				if (!ranOutLocally) {
					armed = true;
				}

				return false;
			}
			case READY -> {
				boolean ranOut = armed;
				reset();
				return ranOut;
			}
			default -> {
				shownSeconds = -1;
				readyAt = NOT_SET;
				return false;
			}
		}
	}

	/**
	 * Called every tick.
	 *
	 * @return whether the cooldown has just run out
	 */
	public boolean tick(long now) {
		if (!armed || readyAt == NOT_SET || shownSeconds > LOCAL_COUNTDOWN_SECONDS || now < readyAt) {
			return false;
		}

		armed = false;
		ranOutLocally = true;
		readyAt = NOT_SET;
		return true;
	}
}
