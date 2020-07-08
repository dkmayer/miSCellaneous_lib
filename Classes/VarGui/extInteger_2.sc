
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

+Integer {


	miSC_partMax { |part|
		^(part.size + 1).collect {|i|
			case
				{ i == 0 }{ part[i] }
				{ i == part.size }{ this - part[i - 1] }
				{ true }{ part[i] - part[i - 1] }
		}.maxItem;
	}


	miSC_eqPart { |part, k|
		// search for quite equal partitions, needed for column break
		// part expects ordered list that describes a partition of the integer by indices (ascending numbers > 0, < given integer)
		// gives a new partition with k indices
		var eqPart, subPart, eqSubPart, newEqSubPart, min, newMin, div, l, m, r, deltaLo, deltaHi, eqIndex;
		case 
			{ k == (part.size + 1) }
				{ eqPart = part }
			{ k > (part.size + 1) }
				{ Error("no possible partition").throw; }
			{ k == 2 }
				{ 
					m = part.size;
					r = part.detectIndex(_ >= (this/2));
					case
						{ r.isNil }
							{ eqPart = [part[m-1]] }
						{ r == 0 }
							{ eqPart = [part[0]] }
						{ true }{ 
							deltaLo = (this/2) - part[r - 1];
							deltaHi = part[r] - (this/2);
							eqPart = (deltaLo >= deltaHi).if { [part[r]] }{ [part[r - 1]] };
						}
				}
			{ true }
				{	
					// search around k-th part, then go on recursively

					l = 0;
					div = this/k;
					while { 
						(r.isNil) && (l < (part.size - k)) 
					}{	
						(part[l] >= div).if { r = l };
						l = l + 1;
					};
							
					((r.isNil) || (r == 0)).if {
						eqIndex = ((r.isNil).if { part.size - k }{ 0 });
						subPart = part.drop(eqIndex + 1) - part[eqIndex];
						eqSubPart = (this - part[eqIndex]).miSC_eqPart(subPart, k - 1);
					}{
						[r - 1, r].do {|i,j|
							subPart = part.drop(i+1) - part[i];
							newEqSubPart = (this - part[i]).miSC_eqPart(subPart, k - 1);
							newMin = max(part[i], (this - part[i]).miSC_partMax(newEqSubPart));
							((j == 0) or: { newMin < min }).if {
								min = newMin;
								eqIndex = i;
								eqSubPart = newEqSubPart;
							};
						};
					};
					eqPart = [part[eqIndex]] ++ (eqSubPart + part[eqIndex]);				};
		^eqPart
	}
	
	
}
