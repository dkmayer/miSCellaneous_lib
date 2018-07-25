
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


PV_BinRange : PV_ChainUGen {

	*new { |buffer, loBin, hiBin|
		^this.multiNew(\control, buffer, loBin, hiBin)
	}

	*new1 { |rate, buffer, loBin, hiBin|
		var bufSizes = buffer.miSC_getFFTbufSizes, wipe;
		var bufSize = bufSizes[0];
		var chain_clipped = bufSizes.collect { LocalBuf(bufSize) };

		chain_clipped = PV_Copy(buffer, chain_clipped);

		wipe = loBin * 2 / bufSize;
		chain_clipped = PV_BrickWall(chain_clipped, wipe);

		wipe = hiBin * 2 / bufSize - 1;
		chain_clipped = PV_BrickWall(chain_clipped, wipe);

		^chain_clipped[0]
	}
}

PV_BinGap : PV_ChainUGen {

	*new { |buffer, loBin, hiBin|
		^this.multiNew(\control, buffer, loBin, hiBin)
	}

	*new1 { |rate, buffer, loBin, hiBin|
		var bufSizes = buffer.miSC_getFFTbufSizes, wipe;
		var bufSize = bufSizes[0];
		var chain_gap_1 = bufSizes.collect { LocalBuf(bufSize) };
		var chain_gap_2 = bufSizes.collect { LocalBuf(bufSize) };

		chain_gap_1 = PV_Copy(buffer, chain_gap_1);
		chain_gap_2 = PV_Copy(buffer, chain_gap_2);

		wipe = loBin - 1 * 2 / bufSize - 1;
		chain_gap_1 = PV_BrickWall(chain_gap_1, wipe);

		wipe = hiBin + 1 * 2 / bufSize;
		chain_gap_2 = PV_BrickWall(chain_gap_2, wipe);

		^PV_Add(chain_gap_1, chain_gap_2)[0];
	}
}

