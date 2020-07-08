
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


	miSC_isSymmetricRange { |i, j|
		var count = 0, limit = div(j - i, 2);

		while { count <= limit }{
			(this[i + count] != this[j - count]).if { ^false };
			count = count + 1
		};
		^true
	}

	miSC_isQuasiSymmetricRange { |i, j|
		^this.miSC_isSymmetricRange(i + 1, j)
	}

	// expects j >= i
	miSC_symmetryType { |i, j|
		^((j == i) or: {
			(0..j - i - 1).every { |k| this[i + k + 1] ==  this[i] }
		}).if {
			3
		}{
			this.miSC_isSymmetricRange(i, j).if {
				2
			}{
				this.miSC_isQuasiSymmetricRange(i, j).if { 1 }{ 0 }
			}
		}
	}

	miSC_smallestPeriodLength {
		var length = 1, i, z = this.size;

		while { length + 1 <= z }{
			i = 0;
			while { i + 1 <= z }{
				(this[i % length] != this[i]).if { i = z };
				i = i + 1;
				(i == z).if { ^length }
			};
			(length + 1 == z).if {
				^length + 1
			}{
				length = length + 1

			}
		};
		^1
	}

	miSC_checkSymmetricPeriods {
		var length = this.miSC_smallestPeriodLength, offset = 0, z = this.size,
			type = 0, goOn = true, periods, symmetries, completions, halfPeriodStart,
			symTypeSymbols = IdentityDictionary[
				0 -> 'asym',
				1 -> 'quasisym',
				2 -> 'sym',
				3 -> 'identic'
			];

		while { goOn }{
			type = this.miSC_symmetryType(offset, offset + length - 1);
			(type != 0).if {
				// if type is quasi-symmetric and length odd there might be a symmetric period
				// starting from the middle - check if it exists and if it's in the range,
				// if so prefer symmetric period representation
				(length.odd and: { type == 1 }).if {
					halfPeriodStart = offset + div(length, 2) + 1;
					((halfPeriodStart + length <= z) and: {
						this[halfPeriodStart] == this[halfPeriodStart - 1]
					}).if { type = 2; offset = halfPeriodStart };
				};
				goOn = false
			}{
				offset = offset + 1
			};
			(offset + length > z).if { goOn = false };
		};

		(type == 0).if {
			offset = 0;
			periods = this.clump(length);
			symmetries = 0 ! periods.size;
			completions = true ! periods.size;

			// check symmetry of incomplete period, it can be of any type
			(periods.last.size != periods.first.size).if {
				completions[periods.size - 1] = false;
				symmetries[periods.size - 1] =
					periods.last.miSC_symmetryType(0, periods.last.size - 1);
			}

		}{
			periods = ((offset > 0).if { [this[0..offset-1].asArray] }) ++ (this.drop(offset).clump(length));
			completions = periods.collect { |p| p.size == length };
			symmetries = type ! periods.size;

			// check symmetries of incomplete periods, they can be of any type
			completions.first.not.if {
				symmetries[0] = periods[0].miSC_symmetryType(0, periods[0].size - 1)
			};

			completions.last.not.if {
				symmetries[periods.size - 1] =
					periods.last.miSC_symmetryType(0, periods.last.size - 1)
			};
		};
		^[periods, symmetries.collect { |i| symTypeSymbols[i] }, completions]
	}

	miSC_checkCharacteristicPeriod {
		var periods, symmetries, completions, index, offset;
		#periods, symmetries, completions = this.miSC_checkSymmetricPeriods;
		index = completions.indexOfEqual(true);
		offset = (index > 0).if { periods.first.size }{ 0 };
		^[
			periods[index],
			offset,
			periods[index].size.odd.if { \odd }{ \even },
			symmetries[index]
		]
	}
}


+Sieve {
	checkSymmetricPeriods { ^this.weakCopy.toIntervals.list.miSC_checkSymmetricPeriods }
	checkCharacteristicPeriod { ^this.weakCopy.toIntervals.list.miSC_checkCharacteristicPeriod }
	plotCharacteristicPeriod {
		^this.weakCopy.toIntervals.list.miSC_checkCharacteristicPeriod.at(0)
			.toSieve(\intervals, \intervals).plot
	}
}
