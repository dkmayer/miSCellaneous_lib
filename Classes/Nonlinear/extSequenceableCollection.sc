
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2020-04-19, version 0.23
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

	miSC_adaptFb1ArgList { |in, countOffset|
		// expects argList as receiver
		var count = countOffset, adaptedFb1ArgList;
		adaptedFb1ArgList = this.collect { |argItem, i|
			argItem.isCollection.if {
				argItem.collect { |y, j|
					y.miSC_isAr.if {
						count = count + 1;
						in[count]
					}{
						y
					}
				}
			}{
				argItem.miSC_isAr.if {
					count = count + 1;
					in[count]
				}{
					argItem
				}
			}
		};
		// need count again in Fb1
		^[adaptedFb1ArgList, count]
	}


// method for applying functions on numeric approximations with Fb1_ODE

	miSC_fb1_perform { |thing, in, size, outSize|
		var valueArray, addArray, composed;
		^thing.isNil.if {
			this
		}{
			valueArray = this[0..size-1];
			addArray = this[size..outSize-1];

			composed = thing.isKindOf(SequenceableCollection).if {
				// if an array is passed to compose, then
				// a Function in it applies to the whole current y (which is an array)
				// whereas an operator applies to the respective component
				valueArray.collect { |x, i|
					var thingI = thing.miSC_maybeWrapAt(i);
					thingI.isKindOf(Function).if {
						thingI.applyTo(valueArray, in)
					}{
						thingI.applyTo(x, in)
					}
				}
			}{
				thing.applyTo(valueArray, in)
			};
			composed ++ addArray
		}
	}
}



