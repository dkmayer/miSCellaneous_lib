
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2018-07-25, version 0.21
	Copyright (C) 2009-2018 Daniel Mayer
	Email: 	daniel-mayer@email.de
	URL:	http://daniel-mayer.at

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

MemoRoutine : Stream {

	var <>routine, <>lastValues, <>count, <>copyItems, <>copySets;
	
	*new { |func, stackSize = (512), bufSize = 1, copyItems = 0, copySets = 1|
		^super.new.routine_(Routine(func, stackSize)).lastValues_({}!bufSize).count_(0)
			.copyItems_(copyItems.miSC_getCopyCode).copySets_(copySets.miSC_getCopyCode)
	}

	next { |inval|
		var newLastValues, val = routine.next(inval);
		newLastValues = this.lastValues.shift(1, val.miSC_copy(copyItems, copySets));
		this.lastValues = newLastValues;
		count = count + 1;
		^val
	}
	
	value { |inval|
		var newLastValues, val = routine.next(inval);
		newLastValues = this.lastValues.shift(1, val.miSC_copy(copyItems, copySets));
		this.lastValues = newLastValues;
		count = count + 1;
		^val
	}
	
	resume { |inval|
		var newLastValues, val = routine.next(inval);
		newLastValues = this.lastValues.shift(1, val.miSC_copy(copyItems, copySets));
		this.lastValues = newLastValues;
		count = count + 1;
		^val
	}
	
	run { |inval|
		var newLastValues, val = routine.next(inval);
		newLastValues = this.lastValues.shift(1, val.miSC_copy(copyItems, copySets));
		this.lastValues = newLastValues;
		count = count + 1;
		^val
	}
	
	bufSize { ^lastValues.size }
	
	at { |i| ^lastValues[i] }
	
	lastValue { ^lastValues[0] }
	
	reset { routine.reset }
	
	stop { routine.stop }
}



