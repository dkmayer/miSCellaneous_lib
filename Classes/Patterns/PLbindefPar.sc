
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


// Instances of this class are constructed implicitely and used for updating the PLbindefPar


PLbindefParEnvironment : Environment {

	var <name, <plbindefPar, <num;

	*new { |n = 8, proto, parent, know = true, name, num, plbindefPar, plbindefs|
		^super.new(n).proto_(proto).parent_(parent).know_(know)
			.miSC_setName(name)
			.miSC_setNum(num).miSC_setPLbindef(plbindefPar)
			.miSC_plbindefParInit(plbindefs);
	}

	// assign PLbindefEnvironments to indices
	miSC_plbindefParInit { |plbindefs|
		var envir;
		num.do { |i|
			envir = plbindefs.notNil.if {
				plbindefs[i].sourceEnvir
			}{
				PLbindefEnvironment(name: (name ++ i).asSymbol);
			};
			this.superPut(i, envir)
		}
	}

	// normal setter
	superPut { |key, obj| this.superPerform(\put, key, obj) }

	// updates PLbindefEnvironments
	put { |key, obj|
		num.do { |i|
			this.superPerform(\at, i).put(key, obj.miSC_maybeWrapAt(i))
		}
	}

	value { |...args|
		var n = args[0], cond;
		cond = args.size.odd and: {
			(n.isKindOf(Integer) or: { n.isKindOf(SequenceableCollection) })
		};
		cond.if {
			args.drop(1).pairsDo { |key, obj|
				n.asArray.do { |i, j| this[i].put(key, obj.miSC_maybeWrapAt(j)) };
			}
		}{
			args.pairsDo { |key, obj| this.put(key, obj) }
		}
	}

	miSC_setNum { |v| num = v }
	miSC_setName { |v| name = v }
	miSC_setPLbindef { |p| plbindefPar = p }

	// this is the tricky part
	// we want differentiated behaviour for 'at' including prototyping
	at { |item|
		^item.isKindOf(SequenceableCollection).if {
			// subarray setting and getting is handled by temporary instances of this class
			TempPLbindefParEnvironment().plbindefParEnvironment_(this).plbindefArray_(item);
		}{
			item.isKindOf(Integer).if {
				// the only normal entries of this Environment are numbers
				// pointing to PLbindefEnvironments
				this.superPerform(\at, item)
			}{
				(item.asString.last == $_).if {
					// for prototype setting we need "normal" 'at'
					// actual differentiated setting is defined in 'put'
					this.superPerform(\at, item);
				}{
					// for prototype getting we pull from PLbindefParEnvironments
					num.collect { |i| this.superPerform(\at, i).superPerform(\at, item) }
				}
			}
		}
	}

	play { |clock, protoEvent, quant, doReset = false|
		plbindefPar.play(clock, protoEvent, quant, doReset)
	}

	reset { plbindefPar.reset }
	stop { plbindefPar.stop }
	clear { plbindefPar.clear }
	remove { plbindefPar.remove }

	subEnvirs { ^num.collect { |i| this[i] } }
	subPLbindefs { ^num.collect { |i| this[i].plbindef } }

}



PLbindefPar : Pdef {

	var <sourceEnvir, <refEnvir, <num;

	*new { |...args|
		var srcEnvir, key = args[0], indices = args[1], pairs,
			refEnvir, plbindefs, wrappedPLbindefs, pdef, newPdef;

		indices.isKindOf(Integer).if { indices = (0..indices-1) };

		pdef = Pdef.all.at(key);
		^pdef.notNil.if {
			pdef.isKindOf(PLbindefPar).not.if {
				SimpleInitError("Attempt to overwrite Pdef of other type, change key or remove other").throw
			}{
				// use setter of PLbindefParEnvironment
				pdef.sourceEnvir.value(indices, *args[2..]);
				pdef
			}
		}{
			// actual constructor case

			// determine the environment where to reference
			// the sourceEnvir with global Pbindef name
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
					pairsI = pairsI.add(v.miSC_maybeWrapAt(i));
				};
				PLbindef.new(*pairsI);
       		};
			// event shortcut hook must be applied before Ppar, thus case distinction
			wrappedPLbindefs = plbindefs.collect(_.eventShortcuts);
			newPdef = super.new(key, Ppar(wrappedPLbindefs));
			newPdef.miSC_setRefEnvir(refEnvir).miSC_updateSourceEnvir(pairs, indices.size, plbindefs);
			newPdef
		}
	}

	miSC_updateSourceEnvir { |pairs, sze, plbindefs|
		sourceEnvir = sourceEnvir ?? {
			// size will be fixed with first update
			num = sze;
			PLbindefParEnvironment(
				name: key.asSymbol,
				num: num,
				plbindefPar: this,
				plbindefs: plbindefs
			)
		};
		refEnvir.put(key, sourceEnvir);
	}

	play { |clock, protoEvent, quant, doReset = false|
		num.do { |i|
			sourceEnvir[i].play(
				clock.miSC_maybeWrapAt(i),
				protoEvent.miSC_maybeWrapAt(i),
				quant.miSC_maybeWrapAt(i),
				doReset.miSC_maybeWrapAt(i)
			);
		}
	}

	reset { num.do { |i| sourceEnvir[i].plbindef.reset } }
	stop { num.do { |i| sourceEnvir[i].plbindef.stop } }

	// cleanup
	clear {
		this.stop;
		refEnvir.notNil.if {
			refEnvir.put(key, nil);
			refEnvir = nil;
		};
		sourceEnvir.notNil.if {
			sourceEnvir.miSC_envirClear;
			sourceEnvir = nil
		};
		this.source = nil;
		num.do { |i| this.class.all.at((key ++ i).asSymbol).clear };
		^nil
	}

	// need to remove all associated PLbindefs too
	remove {
		this.class.all.removeAt(this.key);
		num.do { |i| this.class.all.removeAt((key ++ i).asSymbol) };
		this.clear;
	}

	miSC_setRefEnvir { |v| refEnvir = v }

	subEnvirs { ^sourceEnvir.subEnvirs }
	subPLbindefs { ^sourceEnvir.subPLbindefs }

	eventShortcuts {
		^num.do { |i|
			sourceEnvir[i].miSC_setPLbindef(sourceEnvir[i].plbindef.eventShortcuts)
		}
	}

}


TempPLbindefParEnvironment : Environment {

	var <>plbindefParEnvironment, <>plbindefArray;

	*new { |n = 8, proto, parent, know = true|
		^super.new(n).proto_(proto).parent_(parent).know_(know)
	}

	put { |key, obj|
		plbindefArray.do { |i, j|
			plbindefParEnvironment[i].put(key, obj.miSC_maybeWrapAt(j))
		}
	}

	at { |key|
		^(key.asString.last == $_).if {
			// for subarray prototype setting we need normal 'at'
			this.superPerform(\at, key)
		}{
			// for subarray prototype getting we pull from PLbindefParEnvironments
			plbindefArray.collect { |i| plbindefParEnvironment[i][key] }
		}
	}

	play { |clock, protoEvent, quant, doReset = false|
		plbindefArray.do { |i, j|
			plbindefParEnvironment[i].play(
				clock.miSC_maybeWrapAt(j),
				protoEvent.miSC_maybeWrapAt(j),
				quant.miSC_maybeWrapAt(j),
				doReset.miSC_maybeWrapAt(j)
			);
		}
	}

	reset { plbindefArray.do { |i| plbindefParEnvironment[i].reset } }
	stop { plbindefArray.do { |i| plbindefParEnvironment[i].stop } }
	clear { plbindefArray.do { |i| plbindefParEnvironment[i].clear } }
}

