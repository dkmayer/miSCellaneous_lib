
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


GFIS : UGen {

	*new { |outRate, func, init, n = 1, nOut, leakDC = true, leakCoef = 0.995|
		var sigs, outSig;
		nOut = nOut ?? { n };
		sigs = 0!n;
		n.do { |i|
			sigs[i] = (i == 0).if {
				func.(init, 0)
			}{
				func.(sigs[i-1], i)
			}
		};
		outSig = nOut.isKindOf(SequenceableCollection).if {
			nOut.collect { |i| Select.perform(outRate, i-1, sigs) }
		}{
			Select.perform(outRate, nOut-1, sigs)
		};
		^leakDC.if { LeakDC.perform(outRate, outSig, leakCoef) }{ outSig }
	}

	*ar { |func, init, n = 1, nOut, leakDC = true, leakCoef = 0.995|
		^this.new(\ar, func, init, n, nOut, leakDC, leakCoef)
	}

	*kr { |func, init, n = 1, nOut, leakDC = true, leakCoef = 0.995|
		^this.new(\kr, func, init, n, nOut, leakDC, leakCoef)
	}
}

