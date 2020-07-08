
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


Dwalk : UGen {

	*new { |stepsPerDir = 1, stepWidth = 1, stepMode = 0, start = 0, lo = -inf, hi = inf,
		startDir = 1, dirChangeMode = 0, withDirs = 0, stepsPerDirMulUGen = 1,
		stepsPerDirAddUGen = 0, dUniqueBufSize = 1048576|
		^super.new.init(stepsPerDir, stepWidth, stepMode, start, lo, hi, startDir,
			dirChangeMode, withDirs, stepsPerDirMulUGen, stepsPerDirAddUGen, dUniqueBufSize)
	}

	init { |stepsPerDir, stepWidth, stepMode, start, lo, hi, startDir, dirChangeMode,
			withDirs, stepsPerDirMulUGen, stepsPerDirAddUGen, dUniqueBufSize|

		var dir, dirFunc, steps1, steps2, dir1, dir2, lastDir, lastIndex,
			writeDir, writeIndex, dirChange, initZero;

		dirFunc = { Dseq((startDir.sign > 0).if { [1, -1] }{ [-1, 1] }, inf) };

		withDirs = [0, false].includes(withDirs).not;

		withDirs.if {
			steps1 = Dunique(stepsPerDir, dUniqueBufSize);
			steps2 = steps1;
		}{
			steps1 = stepsPerDir;
		};

		(stepMode == 0).if {
			dir1 = Dstutter(steps1 * stepsPerDirMulUGen + stepsPerDirAddUGen, dirFunc.() * stepWidth);
		}{
			dir1 = Dstutter(steps1 * stepsPerDirMulUGen + stepsPerDirAddUGen, dirFunc.()) * stepWidth;
		};
		withDirs.if { dir2 = Dstutter(steps2 * stepsPerDirMulUGen + stepsPerDirAddUGen, dirFunc.()) };

		lastDir = LocalBuf(1).set(0);
		lastIndex = LocalBuf(1).set(start);

		(dirChangeMode == 0).if {

			dir = Dstutter(2, dir1);
			initZero = Dstutter(2, Dseq([0, Dseq([1], inf)]));

			dirChange = (dir - Dbufrd(lastDir) * initZero).sign.abs;
			dirChange <! (writeDir = Dbufwr(dir, lastDir));

			writeIndex = Dbufwr(
				(Dbufrd(lastIndex) + (initZero * (1 - dirChange) * writeDir)).clip(lo, hi),
				lastIndex
			)
		}{
			dir = Dseq([0, dir1]);
			initZero = Dseq([0, Dseq([1], inf)]);

			writeDir = Dbufwr(dir * initZero, lastDir);

			writeIndex = Dbufwr(
				(Dbufrd(lastIndex) + writeDir).clip(lo, hi),
				lastIndex
			)
		};
		^withDirs.if { [writeIndex, dir2] }{ writeIndex }
	}
}
