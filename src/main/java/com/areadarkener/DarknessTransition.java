package com.areadarkener;

/** A scene fade measured with the monotonic clock, independent of frame rate. */
final class DarknessTransition
{
	private static final long DURATION_NANOS = 1_000_000_000L;
	private double start;
	private int target;
	private long startedAt;

	void setTarget(int strength, long now)
	{
		int next = Math.max(0, Math.min(100, strength));
		if (next != target)
		{
			start = valueAt(now);
			target = next;
			startedAt = now;
		}
	}

	double valueAt(long now)
	{
		double progress = Math.max(0, Math.min(1, (double) (now - startedAt) / DURATION_NANOS));
		return start + (target - start) * progress;
	}
}
