
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


// Instances of this class are constructed implicitely and used for updating the PLbindef

PLbindefEnvironment : Environment {

	var <name, <plbindef;

	*new { |n = 8, proto, parent, know = true, name, plbindef|
		^super.new(n).proto_(proto).parent_(parent).know_(know)
			.miSC_setName(name).miSC_setPLbindef(plbindef)
	}

	// normal setter
	superPut { |key, obj| this.superPerform(\put, key, obj) }

	// setter for pairs
	value { |...args| args.pairsDo { |key, obj| this.put(key, obj) } }

	// updates source with setter
	put { |key, obj|
		{ Pbindef(name, key, obj) }.();
		this.superPut(key, obj)
	}

	miSC_setName { |v| name = v }
	miSC_envirClear { this.superPerform(\clear) }
	miSC_setPLbindef { |p| plbindef = p }

	play { |clock, protoEvent, quant, doReset = false|
		plbindef.play(clock, protoEvent, quant, doReset)
	}
	isPlaying { plbindef.isPlaying }
	reset { plbindef.reset }
	stop { plbindef.stop }
	clear { plbindef.clear }
	remove { plbindef.remove }
}


PLbindef : Pbindef {

	var <sourceEnvir, <refEnvir;

	*new { |...args|
		var sourceEnvir, key = args[0], refEnvir, pdef, newPdef;

		pdef = Pdef.all.at(key);
		^pdef.notNil.if {
			pdef.isKindOf(PLbindef).not.if {
				SimpleInitError("Attempt to overwrite Pdef of other type, change key or remove other").throw
			}{
				// use setter of PLbindefEnvironment
				pdef.sourceEnvir.value(*args[1..]);
				pdef
			}
		}{
			// actual constructor case

			// determine the environment where to reference
			// the sourceEnvir with global Pbindef name
			(args.last.isKindOf(Environment) and: { args.size.even }).if {
				refEnvir = args.last;
				args = args.drop(-1)
			}{
				refEnvir = currentEnvironment
			};
			newPdef = super.new(*args);
			newPdef.miSC_setRefEnvir(refEnvir).miSC_updateSourceEnvir;
			newPdef
		}
	}

	miSC_updateSourceEnvir {
		sourceEnvir = sourceEnvir ?? {
			PLbindefEnvironment(
				name: key.asSymbol,
				plbindef: this
			)
		};
		this.pattern.pairs.pairsDo { |x, y| sourceEnvir.superPut(x, y.source) };
		refEnvir.put(key, sourceEnvir);
	}

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
		^nil
	}

	miSC_setRefEnvir { |v| refEnvir = v }

	// play, isPlaying, reset, stop, remove are inherited from Pbindef
}






