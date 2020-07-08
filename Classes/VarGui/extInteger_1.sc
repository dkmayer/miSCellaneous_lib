
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

	miSC_groupIndices {|groups| 
		var i = groups.size;
		^(this.collect({|j| 
			groups.detectIndex({ |item, k| item.includes(j) }) ?? { i = i + 1; i - 1 }; 
		}))
	}

	miSC_cubeDivision {|dim| // division of unit cube fine enough for given number
		var divCeiling = (this ** dim.reciprocal).ceil.asInteger, 
			div, prod, prod2, i = 0;
		div = divCeiling ! dim;
		prod = div.product;
		prod2 = prod * (div[0] - 1) / div[0];
		while ({ 
			(i <= (dim-1)) && (prod >= this) && (prod2 >= this) 
		},{ 
			div[i] = div[i] - 1;
			i = i + 1;
			prod = prod2;
			prod2 = prod * (div[i] - 1) / div[i];
		});
		^div;
	}

	miSC_selectCubeComponent {|index, areaUsage|
		var lo, mid, hi;
		mid = (index + 0.5) / this;
		^rrand(mid - (areaUsage / this / 2), mid + (areaUsage / this / 2));
	}

	miSC_distinctCubePoints {|dim, areaUsage = 0.9| // just for handsome integers !
		var div = this.miSC_cubeDivision(dim);
		^div.miSC_enumerateIndices.scramble.copyFromStart(this - 1)
			.collect { |x| 
				x.collect { |y,i| 
					div[i].miSC_selectCubeComponent(y, areaUsage) 
				};
			};
	}

	miSC_distinctColors {|redLo = 0.4, redHi = 0.7, greenLo = 0.4, greenHi = 0.7, 
			blueLo = 0.4, blueHi = 0.7, areaUsage = 0.9, greyMode = false| // just for handsome integers !
		var cubePoints = greyMode.if { (0..this-1).scramble / (this - 1) }{ this.miSC_distinctCubePoints(3, areaUsage) };
		
		^this.collect {|i|
			Color(greyMode.if { cubePoints[i] }{ cubePoints[i][0] }  * (redHi - redLo) + redLo,
			 	greyMode.if { cubePoints[i] }{ cubePoints[i][1] } * (greenHi - greenLo) + greenLo,
			 	greyMode.if { cubePoints[i] }{ cubePoints[i][2] } * (blueHi - blueLo) + blueLo);
			}
	}


	miSC_colorDeviationPairs {|colorGroups| 
		// expects double nested collection with all indices occuring
		// outer groups mean related colors, inner groups same color
		var pairs = Array.newClear(this), n;
		colorGroups.do {|group,i|
			n = group.size;
			group.do {|item, j|
				item.isNumber.if {
					pairs[item] = [i, n - j - 1];					}{
					item.do {|jtem| pairs[jtem] = [i, n - j - 1] };
				};
			};
		};
		^pairs;
	}

	miSC_getCtrIndex { |argName| ^nil }
	

	miSC_runMsg { arg flag = true;
		^[12, this, flag.binaryValue]; 
	}
	
	miSC_setMsg { arg ... args;
		^[15, this] ++ args.asOSCArgArray; 	 
	}
	
	miSC_setnMsg { arg ... args;
		^[16, this] ++ Node.setnMsgArgs(*args);
	}

	miSC_freeMsg { ^[11, this] }

		
}
