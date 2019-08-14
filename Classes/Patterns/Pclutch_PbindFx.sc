
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2018-08-14, version 0.22
	Copyright (C) 2009-2019 Daniel Mayer
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


Pclutch_PbindFx : FilterPattern {
	var <>connected, <>start;
	*new { arg pattern, connected = true, start;
		^super.new(pattern).connected_(connected).start_(start)
	}
	storeArgs { ^[ pattern, connected, start ] }
	embedInStream { arg inval;
		var clutchStream = connected.asStream;
		var stream = pattern.asStream;
		var startStream = start.asStream;
		var outval, clutch, hasStarted = false;
		while {
			clutch = clutchStream.next(inval);
			clutch.notNil
		} {
			if(clutch === true or: { clutch == 1 }) {
				hasStarted = true;
				outval = stream.next(inval);
				if(outval.isNil) { ^inval };
				inval = outval.yield;
			} {
				hasStarted.not.if { outval = startStream.next(inval) };
				inval = outval.copy.yield;
			};
		}
	}
}

