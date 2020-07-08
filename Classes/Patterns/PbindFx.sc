
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


AbstractPbindFx : Pchain {

	classvar <>defaultSourceCleanupDelay = 0.3;
	classvar <>defaultFxCleanupDelay = 0.05;
	classvar <>defaultCleanupDt = 0.2;

	*new { |pbindData ...fxData|
		var srcPat, fxPat, pairs, fxEventsPat;

		srcPat = case { pbindData.isKindOf(SequenceableCollection) }{
			Pbind(*pbindData)
		}{ pbindData.isKindOf(Pattern) }{
			pbindData
		}{ true }{
			SimpleInitError("pbindData must be event pattern or list of patternpairs").throw
		};
		srcPat = srcPat.eventShortcuts;

		fxEventsPat = Ptuple(fxData.collect { |fxDataI, i|
			var fxPatI = case { fxDataI.isKindOf(SequenceableCollection) }{
				Pbind(*fxDataI)
			}{ fxDataI.isKindOf(Pattern) }{
				fxDataI
			}{ true }{
				SimpleInitError("fxData item must be event pattern or list of patternpairs").throw
			};

			// need to apply here as data is encapsulated in Events
			// normal replacement mechanism wouldn't work

			fxPatI = fxPatI.eventShortcuts;

			// start not before fx demanded with Pclutch_PbindFx
			// (slight change of Pclutch default behaviour)
			Pclutch_PbindFx(
				Pevent(fxPatI <> Pbind(\cleanupDelay, defaultFxCleanupDelay)),
				Pfunc { |e| e.fxOrder.miSC_getClutch(i+1) },
				0
			)
		});

		fxPat = Pbind(
			\fxDataSize, fxData.size,
			\defaultFxOrder, [0],
			[\fxOrder, \fxPredecessors, \fxSuccessors],
			Pfunc { |e|
				(e.fxOrder ?? { e.defaultFxOrder }).value.miSC_getFxOrderData(e.fxDataSize);
			},
			\type, \pbindFx,
			\fxEvents, fxEventsPat
		);

		^super.new(fxPat, srcPat);
	}

}

PbindFx : AbstractPbindFx {

	*new { |pbindData ... fxData|
		^super.new(pbindData, *fxData)
	}
}

// receiver supposed to be clutchIndex

+ Integer {
	miSC_getClutch { |index|
		^(index == this).if { true }{ false }
	}
}

// receiver supposed to be clutchIndices

+ SequenceableCollection {

	miSC_getClutch { |index|
		^this.includes(index).if { true }{ false }
	}

	miSC_atKey { |key|
		var i = this.indexOf(key);
		^i !? { this[i + 1] }
	}
}

