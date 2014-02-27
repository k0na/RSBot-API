package org.powerbot.script.lang;

import org.powerbot.script.rs3.tools.MethodContext;
import org.powerbot.script.rs3.tools.Area;
import org.powerbot.script.rs3.tools.Locatable;
import org.powerbot.script.rs3.tools.Nameable;

public abstract class PlayerQuery<K extends Locatable & Nameable> extends AbstractQuery<PlayerQuery<K>, K>
		implements Locatable.Query<PlayerQuery<K>>, Nameable.Query<PlayerQuery<K>> {
	protected PlayerQuery(final MethodContext factory) {
		super(factory);
	}

	@Override
	protected PlayerQuery<K> getThis() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> at(final Locatable l) {
		return select(new Locatable.Matcher(l));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> within(final double distance) {
		return within(ctx.players.local(), distance);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> within(final Locatable target, final double distance) {
		return select(new Locatable.WithinRange(target, distance));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> within(final Area area) {
		return select(new Locatable.WithinArea(area));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> nearest() {
		return nearest(ctx.players.local());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> nearest(final Locatable target) {
		return sort(new Locatable.NearestTo(target));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> name(final String... names) {
		return select(new Nameable.Matcher(names));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PlayerQuery<K> name(final Nameable... names) {
		return select(new Nameable.Matcher(names));
	}
}
