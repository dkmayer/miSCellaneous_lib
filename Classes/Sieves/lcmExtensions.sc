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

	lcmByGcd { |...args|
		var lcm, thr = 2 ** 31 - 1, postWarning = false, returnNil = false;
		(args.size == 0).if { ^(this == 0).if { 0 }{ this.abs } };
		(args.every { |i| i.isKindOf(Integer) }).not.if { "lcmByGcd requires Integers".error };
		args.any(_ == 0).if { ^0 };
		lcm = args.inject(this, { |x, y, i|
			// z must be float for reliable threshold check, thus '/' instead of 'div'
			var z = x / gcd(x.isFloat.if { x.round.asInteger }{ x }, y) * y;
			(z > thr).if {
				(i < (args.size-1)).if {
					("exceeding int32 bound within iteration, " ++
						"reduce number of args or try lcmByFactors").warn;
					returnNil = true;
				};
				postWarning = true;
			};
			z
		});
		returnNil.if { ^nil };
		^postWarning.if {
			"exceeding int32 bound, returning float".warn;
			lcm
		}{
			lcm.round.asInteger
		}
	}

	lcmByFactors { |...args|
		var lcm, f, factors, argFactors, argListFactors, lcmFactors,
			lcmFactorDict = IdentityDictionary.new,
			postWarning = false, thr = 2 ** 31 - 1;

		(args.size == 0).if { ^(this == 0).if { 0 }{ this.abs } };
		(args.every { |i| i.isKindOf(Integer) }).not.if { "lcmByGcd requires Integers".error };
		args.any(_ == 0).if { ^0 };

		factors = this.abs.factors;
		argListFactors = args.abs.collect(_.factors);

		f = { |dict|
			{ |x|
				var m = dict.at(x);
				dict.put(x, m.isNil.if { 1 }{ m + 1 });
			}
		};
		factors.do(f.(lcmFactorDict));

		// collect prime factors and occurences
		argListFactors.do { |argFactors|
			var argFactorDict = IdentityDictionary.new;
			argFactors.do(f.(argFactorDict));

			argFactorDict.keysValuesDo{ |k,v,i|
				var m = lcmFactorDict.at(k);
				lcmFactorDict.put(k, m.isNil.if { v }{ max(m,v) })
			};
		};

		lcmFactors = [];
		lcmFactorDict.keysValuesDo { |k,v| v.do { lcmFactors = lcmFactors.add(k) } };
		lcmFactors.sort;

		// threshold check, if result exceeds int32 bound return float
		lcmFactors.do { |x,i|
			(lcmFactors.size > 1).if {
				(i == 0).if {
					lcm = x.asFloat;
				}{
					lcm = lcm * x;
					(lcm > thr).if { postWarning = true };
				}
			}{
				^[x, lcmFactors, [factors, argListFactors]]
			}
		};
		^postWarning.if {
			"exceeding int32 bound, returning float".warn;
			[lcm, lcmFactors, [factors, argListFactors]]
		}{
			[lcm.round.asInteger, lcmFactors, [factors, argListFactors]]
		}
	}

}

+SequenceableCollection {

	lcmByGcd { ^this[0].lcmByGcd(*this[1..]) }
	lcmByFactors { ^this[0].first.lcmByFactors(*this[1..]) }
}

