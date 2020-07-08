
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


+Object {
	miSC_isPointSel { ^((this == \points) or: { this == \p }) }
	miSC_isIntervalSel { ^((this == \intervals) or: { this == \i }) }
}

+Integer {
	toSieve { ^Sieve.newEmpty.miSC_setList(List[this]).miSC_setMode(\points) }
}



+SequenceableCollection {

	toSieve { |fromMode = \points, toMode = \points, addOffset = 0, withCheck = true|

		var sieve, newOffset, pointCol, intervalCol;

		addOffset.isInteger.not.if {
			SimpleInitError("Argument addOffset must be Integer").throw
		};
		this.every(_.isInteger).not.if {
			SimpleInitError(
				"SequenceableCollection must consist of Integers"
			).throw
		};

		case { fromMode.miSC_isPointSel }{
			pointCol = List.newUsing(this + addOffset);
			intervalCol = pointCol.differentiate.drop(1);
			withCheck.if {
				intervalCol.every(_>0).not.if {
					SimpleInitError(
						"With fromMode = \points SequenceableCollection " ++
						"must be strictly ascending"
					).throw
				}
			};

			case { toMode.miSC_isPointSel }{
				^Sieve.newEmpty.miSC_setList(pointCol).miSC_setMode(\points)
			}
			{ toMode.miSC_isIntervalSel }{
				newOffset = pointCol[0];
				^Sieve.newEmpty.miSC_setList(intervalCol).miSC_setOffset(newOffset)
			}

			{ true }{
				SimpleInitError(
					"Allowed Symbols for toMode: \points, \p, \intervals, \i"
				).throw
			};
		}
		{ fromMode.miSC_isIntervalSel }{

			withCheck.if {
				this.every(_>0).not.if {
					SimpleInitError(
						"With fromMode = \intervals SequenceableCollection " ++
						"must consist of positive Integers"
					).throw
				}
			};

			case { toMode.miSC_isPointSel }{
				sieve = Sieve.newEmpty.miSC_setMode(\points);
				sieve.list.add(addOffset);
				this.do { |interval| var point = interval + sieve.list.last; sieve.list.add(point) };
				^sieve
			}
			{ toMode.miSC_isIntervalSel }{
				^Sieve.newEmpty.miSC_setList(this).miSC_setOffset(addOffset)
			}

			{ true }{
				SimpleInitError(
					"Allowed Symbols for toMode: " ++
					"\points, \p, \intervals, \i"
				).throw
			};
		}

		{ true }{
			SimpleInitError(
				"allowed Symbols for fromMode: \points, \p, \intervals, \i"
			).throw
		};

	}
}

