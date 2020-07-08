
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2020-07-08, version 0.24
	Copyright (C) 2009-2020 Daniel Mayer
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


+SequenceableCollection {
	miSC_makeLacePat {
		^Ppatlace(this.collect(_.miSC_makeLacePat), inf)
	}
	miSC_makeLaceList { |repeats = inf|
		^this.collect(_.miSC_makeLacePat)
	}

	ev {
		var ev = ();
		this.pairsDo { |k,v| ev.put(k, ev.use { v.() }) };
		^ev
	}
	on { ^this.ev.on }

	pa {
		var a;
        this.pairsDo { |k,v|
			a = a.add(k);
			a = a.add(v.isKindOf(Function).if { Pfunc { |e| e.use { v.() } } }{ v });
		};
		^a
	}

	p { ^Pbind(*this.pa)}
	pm { |sym| ^Pmono(sym, *this.pa)}
	pma { |sym| ^PmonoArtic(sym, *this.pa)}
	pbf { |sym| ^Pbindef(sym, *this.pa)}

	pp { |clock, protoEvent, quant| ^this.p.play(clock, protoEvent, quant)}
	ppm { |sym, clock, protoEvent, quant| ^this.pm(sym).play(clock, protoEvent, quant)}
	ppma { |sym, clock, protoEvent, quant| ^this.pma(sym).play(clock, protoEvent, quant)}
	ppbf { |sym, clock, protoEvent, quant| ^this.pbf(sym).play(clock, protoEvent, quant)}

	eventShortcuts { ^this.collect(_.eventShortcuts) }

	miSC_maybeWrapAt { |i| ^this.wrapAt(i) }

}

