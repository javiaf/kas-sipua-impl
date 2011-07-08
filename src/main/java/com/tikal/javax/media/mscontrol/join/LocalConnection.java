package com.tikal.javax.media.mscontrol.join;

import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;

public class LocalConnection {

	private Direction direction;
	private Joinable joinable;
	private LocalConnection other;

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public Joinable getJoinable() {
		return joinable;
	}

	public void setJoinable(Joinable joinable) {
		this.joinable = joinable;
	}

	public LocalConnection getOther() {
		return other;
	}

	public void setOther(LocalConnection other) {
		this.other = other;
	}

	public LocalConnection(Direction direction, Joinable joinable) {
		this.direction = direction;
		this.joinable = joinable;
	}

	public void join(LocalConnection other) {
		this.other = other;
		other.setOther(this);
	}
}
