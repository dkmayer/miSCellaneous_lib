
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



+Float {
	miSC_decimalStrings { |precision = 8|
		var precString = this.asStringPrec(precision), prePointString, postPointString,
			signString, manString, expString, posOfPoint, shift;

		#manString, expString = precString.split($e);		
		(manString[0] == $-).if {
			manString = manString.drop(1);
			signString = "-";
		};
		
		expString.isNil.if {
			// no exponent
			posOfPoint = precString.find(".");
			posOfPoint.isNil.if {
				prePointString = precString;
			}{
				prePointString = precString.copyFromStart(posOfPoint-1);
				postPointString = precString.copyToEnd(posOfPoint+1);
			}
		}{
			// with exponent
			shift = expString.drop(1).asInteger;
			(expString[0] == $+).if {
				(manString.size == 1).if {
					// no comma
					prePointString = manString ++ ($0!shift).join;
				}{
					(shift < (manString.size-2)).if {
						prePointString = manString[0].asString ++ manString.copyRange(2, shift+1);
						postPointString = manString.copyToEnd(shift+1);
					}{
						prePointString = manString[0].asString ++ manString.copyToEnd(2) ++
								($0 ! (shift - (manString.size-2))).join;					}
				}
			}{
				prePointString = "0";
				(manString.size == 1).if {
					// no comma
					postPointString = ($0!(shift-1)).join ++ manString;
				}{
					postPointString = ($0!(shift-1)).join ++
						manString[0].asString ++ manString.copyToEnd(2);
				}
			}
		};
		^[signString, prePointString, postPointString]
	}
	
}

