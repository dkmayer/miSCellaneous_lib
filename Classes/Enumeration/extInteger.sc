
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


+Integer {
	enum { |pool, function = true, evalAtZero = false, type = 0, order = true, maxNum = inf|
		// type 0: one array for all levels
		// type 1: array of pools (size must equal receiver)

		var allCols, currentCol, currentIndex = 0, indexCol,
			endOfEnum = false, currentPool, check, item, count = 0;

		order.not.if {
			pool = (type == 0).if { pool.scramble }{ pool.collect(_.scramble) };
		};
		indexCol = -1!this;
		currentCol = 0!this;
		while { endOfEnum.not }{
			indexCol[currentIndex] = indexCol[currentIndex] + 1;
			currentPool = (type == 0).if { pool }{ pool[currentIndex] };
			(indexCol[currentIndex] >= (currentPool.size)).if {
				indexCol[currentIndex] = -1;
				currentIndex = currentIndex - 1;
			}{
				item = currentPool.at(indexCol[currentIndex]);

				((currentIndex == 0) && evalAtZero.not).if {
					true
				}{
					function.(item, currentIndex, currentCol, indexCol)
				}.if {
					currentCol[currentIndex] = item;
					(currentIndex == (this - 1)).if {
						allCols = allCols.add(currentCol.deepCopy);
						count = count + 1;
						(count == maxNum).if { endOfEnum = true };
					}{
						currentIndex = currentIndex + 1
					}
				}
			};
			(currentIndex == -1).if { endOfEnum = true };
		};
		^allCols;
	}
}
