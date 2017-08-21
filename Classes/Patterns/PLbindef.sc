/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2017-08-21, version 0.17
	Copyright (C) 2009-2017 Daniel Mayer
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


// These envir classes are used by PLbindef and PLbindefPar,
// instances are constructed implicitely and used for updating the proxies

PLbindefEnvironment : Environment {

	var <name;

	*new { |n = 8, proto, parent, know = true, name|
		^super.new(n).proto_(proto).parent_(parent).know_(know).miSC_setName(name)
	}

	// updates source with setter
	put { |key, obj|
		Pbindef(name, key, obj);
		this.superPerform(\put, key, obj)
	}

	// "normal" setter needed too
	superPut { |key, obj|
		this.superPerform(\put, key, obj)
	}

	// setter for pairs
	value { |...args|
		args.pairsDo { |key, obj| this.put(key, obj) }
	}

	miSC_setName { |v| name = v }

}


PLbindefParEnvironment : PLbindefEnvironment {

	var <num;

	*new { |n = 8, proto, parent, know = true, name, num|
		^super.new(n).proto_(proto).parent_(parent).know_(know).miSC_setName(name).miSC_setNum(num).plbindefParInit
	}

	plbindefParInit {
		num.do { |i|
			var envir = PLbindefEnvironment(name: (name ++ i).asSymbol);
			this.superPut(i, envir)
		};
	}

	put { |key, obj|
		num.do { |i| this[i].put(key, obj.miSC_pbindefAt(i)) };
		this.superPut(key, obj)
	}

	value { |...args|
		var n = args[0], cond;
		cond = args.size.odd and: {
			(n.isKindOf(Integer) or: { n.isKindOf(SequenceableCollection) })
		};
		cond.if {
			args.drop(1).pairsDo { |key, obj|
				n.asArray.do { |i| this[i].put(key, obj.miSC_pbindefAt(i)) };
				this.superPut(key, obj);
			}
		}{
			args.pairsDo { |key, obj| this.put(key, obj) }
		}
	}
	miSC_setNum { |v| num = v }

}



PLbindef : Pbindef {

	var <sourceEnvir, <refEnvir;

	*new { |...args|
		var sourceEnvir, key = args[0], refEnvir;

		// determine the environment where to reference the sourceEnvir with global Pbindef name
		(args.last.isKindOf(Environment) and: { args.size.even }).if {
			refEnvir = args.last;
			args = args.drop(-1)
		}{
			refEnvir = currentEnvironment
		};
		^super.new(*args).miSC_setRefEnvir(refEnvir).updateSourceEnvir;
	}

	updateSourceEnvir {
		sourceEnvir = sourceEnvir ?? { PLbindefEnvironment(name: key.asSymbol) };
		this.pattern.pairs.pairsDo { |x, y| sourceEnvir.superPut(x, y.source) };
		refEnvir.put(key, sourceEnvir);
	}

	// cleanup
	// reference in refEnvir will be deleted, this ensures cleanup with *removeAll
	clear {
		this.stop;
		refEnvir.notNil.if { refEnvir.put(key, nil) };
		this.source = nil;
		^nil
	}

	miSC_setRefEnvir { |v| refEnvir = v }

}



PLbindefPar : Pdef {

	var <sourceEnvir, <refEnvir, <size;

	*new { |...args|
		var srcEnvir, key = args[0], indices = args[1], pairs, refEnvir, plbindefs;

		indices.isKindOf(Integer).if { indices = (0..indices-1) };

		// determine the environment where to reference the sourceEnvir with global Pbindef name
		(args.last.isKindOf(Environment) and: { args.size.odd }).if {
			refEnvir = args.last;
			args = args.drop(-1)
		}{
			refEnvir = currentEnvironment
		};

		pairs = args[2..];

        plbindefs = indices.collect { |i|
			var pairsI = [(key ++ i).asSymbol];
			pairs.pairsDo { |k, v|
				pairsI = pairsI.add(k);
				pairsI = pairsI.add(v.miSC_pbindefAt(i));
			};
			PLbindef.new(*pairsI).eventShortcuts;
        };

		^super.new(key, Ppar(plbindefs)).miSC_setRefEnvir(refEnvir).updateSourceEnvir(pairs, indices.size);
	}

	updateSourceEnvir { |pairs, sze|
		sourceEnvir = sourceEnvir ?? {
			// size will be fixed with first update
			size = sze;
			PLbindefParEnvironment(name: key.asSymbol, num: size)
		};
		pairs.pairsDo { |x, y| sourceEnvir.put(x, y) };
		refEnvir.put(key, sourceEnvir);
	}

	// cleanup
	// reference in refEnvir will be deleted, this ensures cleanup with *removeAll
	clear {
		this.stop;
		refEnvir.notNil.if { refEnvir.put(key, nil) };
		this.source = nil;
		^nil
	}

	// need to remove all associated PLbindefs too
	remove {
		this.class.all.removeAt(this.key);
		size.do { |i| this.class.all.removeAt((key ++ i).asSymbol) };
		this.clear;
	}

	miSC_setRefEnvir { |v| refEnvir = v }

}





