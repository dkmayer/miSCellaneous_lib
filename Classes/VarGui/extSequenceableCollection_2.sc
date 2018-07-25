
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

+SequenceableCollection {

	miSC_pfuncPbindsFromTuples { | pBefore, pReplace, pAfter, instrument, 
		excludeDur = false, excludeLegato = false, post = false, trace = false |
		
		// method for a SequenceableCollection of tuples [symbol, n], 
		// where n > 1 indicates an arrayed arg

		// miSC_pfuncPbindsFromTuples and miSC_sVarGuiSpecsFromTuples exist
		// to avoid multiple lookups in SynthDef metadata by pVarGui


		var pat, keyPfuncPairs = List.new, replaceIndex;
		this.do { |keyNumPair| 
			keyPfuncPairs.add(keyNumPair[0]).add(Pfunc { 
				var key, num, val;
				#key, num = keyNumPair;
				val = currentEnvironment[key];
				(num > 1).if { [val] }{ val }
			}) 
		};
		keyPfuncPairs = pBefore.asArray ++ 
			(instrument !? [\instrument, instrument]).asArray ++ 
			excludeDur.if { [] }{ [\dur, Pfunc { ~dur }] } ++
			excludeLegato.if { [] }{ [\legato, Pfunc { ~legato }] } ++
			keyPfuncPairs ++ pAfter.asArray;
			
		pReplace.pairsDo { |key, pat|
			replaceIndex = keyPfuncPairs.indexOf(key);
			replaceIndex.notNil.if { keyPfuncPairs[replaceIndex + 1] = pat }
		};
		post.if {
			["", "Pbind pairs:", ""].do(_.postln);
			keyPfuncPairs.pairsDo { |key, pat|
				Char.tab.post;
				key.post;
				Char.tab.post;
				pat.postln;
			};
			"".postln;
		};
		pat = Pbind(*keyPfuncPairs);
		^trace.if { pat.trace }{ pat }
	}


	miSC_specAsArray { |synthName, metaKey = \specs, useGlobalSpecs = true|
		this.every { |x| x.isKindOf(Symbol) || x.isKindOf(String) }.if {
			^this.collect(_.miSC_specAsArray(synthName, metaKey, useGlobalSpecs))
		}{
			^this.copy
		}
	}
	
	miSC_oddSpecAsArray { |synthName, metaKey = \specs, useGlobalSpecs = true|
		var c = Array.newClear(this.size);
		^c.do { |item, i| 
			i.odd.if { 
				c[i] = this[i].miSC_specAsArray.copy 
			}{
				c[i] = this[i]
			}
		}
	}

	miSC_sVarGuiData { | ...args |
		var sVarGuiArgs = [\ctrBefore, \ctrReplace, \ctrAfter, \exclude, \metaKey, \useGlobalSpecs, \num],
			synthCtr = [], synth = [], server, input;
			
		this.every { |x| (x.isKindOf(Symbol) || x.isKindOf(String)) }.not.if {
			SimpleInitError("Items of SequenceableCollection as receiver of method " ++
				"sVarGui must be Symbol or String").class_(VarGui).throw
		};
		input = this.collect(_.asSymbol);
		
		// last arg may be Server

		args.last.isKindOf(Server).if {
			server = args.last;
			args = args.drop(-1);
		};
		(args == []).if { args = ()!(this.size) };
		
		// this doesn't check args itself but only its arrangement as collection of dictionaries

		(((args.size == 0) || (args.size == this.size)) && args.every { |x| 
			x.isKindOf(Dictionary) and: { x.keys.isSubsetOf(sVarGuiArgs) } }).not.if {
				SimpleInitError("Args of method aSequenceableCollection.sVarGui must be Dictionaries." ++
				" Their keys must be valid arg names of aSymbol.sVarGui: ctrBefore, ctrReplace, " ++
				"ctrAfter, exclude, metaKey, useGlobalSpecs, num.").class_(VarGui).throw
		};
		
		args.do { |dict, i|
			dict = dict.copy;
			dict.miSC_maybePutPairs(\metaKey, \specs, \useGlobalSpecs, true, \num, 1);

			synthCtr = synthCtr ++ (input[i].miSC_performWithEnvir(\sVarGuiSpecs, dict));
			synth = synth ++ (input[i] ! (dict[\num] ?? { 1 }));
			
		};
		^[synthCtr, synth, server];
	}

	sVarGui { | ...args |
		var data = this.miSC_sVarGuiData(*args);
		^VarGui(synthCtr: data[0], synth: data[1], server: data[2]);
	}

	miSC_pVarGuiData { | ...args |
		var pVarGuiArgs = [\ctrBefore, \ctrReplace, \ctrAfter, \durCtr, \legatoCtr, 
			\pBefore, \pReplace, \pAfter, \exclude, \excludeGate, \clock, \quant, 
			\metaKey, \useGlobalSpecs, \post, \trace, \num], data, 
			varCtr = [], stream = [], clock = [], quant = [], input;

		// this doesn't check args itself but only its arrangement as collection of dictionaries
		
		this.every { |x| (x.isKindOf(Symbol) || x.isKindOf(String)) }.not.if {
			SimpleInitError("Items of SequenceableCollection as receiver of method " ++
				"pVarGui must be Symbol or String").class_(VarGui).throw
		};
		input = this.collect(_.asSymbol);

		(args == []).if { args = ()!(this.size) };

		(((args.size == 0) || (args.size == this.size)) && args.every { |x| 
			x.isKindOf(Dictionary) and: { x.keys.isSubsetOf(pVarGuiArgs) } }).not.if {
				SimpleInitError("Args of method aSequenceableCollection.pVarGui must be Dictionaries." ++
				" Their keys must be valid arg names of aSymbol.sVarGui: ctrBefore, ctrReplace, ctrAfter, " ++
				"durCtr, legatoCtr, pBefore, pReplace, pAfter, exclude, excludeGate, " ++
				"clock, quant, metaKey, useGlobalSpecs, post, trace, num.").class_(VarGui).throw
		};
		
		args.do { |dict, i|
			dict = dict.copy;
			dict.miSC_maybePutPairs(\durCtr, #[0.05, 3, \exp, 0, 0.2], \legatoCtr, #[0.1, 5, \exp, 0, 0.8],
				\excludeGate, true, \metaKey, \specs, \useGlobalSpecs, true, \post, false, \trace, false, \num, 1);

			data = input[i].miSC_performWithEnvir(\miSC_pVarGuiData, dict);
			varCtr = varCtr ++ data[0];
			stream = stream ++ data[1];
			clock = clock ++ data[2];
			quant = quant ++ data[3];
		};
		^[varCtr, stream, clock, quant];
	}
	
	pVarGui { | ...args |
		var data = this.miSC_pVarGuiData(*args);
		^VarGui(data[0], stream: data[1], clock: data[2], quant: data[3])
	}
		
	psVarGui { | pVarGuiArgDictionaries, sVarGuiArgDictionaries, server |
		var streamVarGuiData, synthVarGuiData, src, pDicts, sDicts, wrapper;
		src = this.collect(_.miSC_defNameAsArray);
		wrapper = { |x| x.isKindOf(Dictionary).if { [x] }{ x.asArray } };
		pDicts = wrapper.(pVarGuiArgDictionaries);
		sDicts = wrapper.(sVarGuiArgDictionaries);
		
		((src.size == 2) && src.every { |x| x.every { |y| y.isKindOf(Symbol) || y.isKindOf(String) } }).not.if {
				SimpleInitError("Receiver of method psVarGui / spVarGui must be SequenceableCollection of " ++
					"two items that may be Symbols / Strings or SequenceableCollections thereof").class_(VarGui).throw
		};

		streamVarGuiData = src[0].miSC_pVarGuiData(*pDicts);
		synthVarGuiData = src[1].miSC_sVarGuiData(*sDicts);

		^VarGui(streamVarGuiData[0], synthVarGuiData[0], streamVarGuiData[1], synthVarGuiData[1], 
			clock: streamVarGuiData[2], quant: streamVarGuiData[3], server: server)
	}

	spVarGui { | sVarGuiArgDictionaries, pVarGuiArgDictionaries, server |
		^this.reverse.psVarGui(pVarGuiArgDictionaries, sVarGuiArgDictionaries, server);
	}
		
}

