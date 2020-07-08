
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

+ Symbol {

	miSC_specAsArray { | synthName, metaKey = \specs, useGlobalSpecs = true |
		var desc, spec;

		desc = SynthDescLib.global[synthName.asSymbol];
		spec = desc.tryPerform(\metadata).tryPerform(\at, metaKey) ?? 
			{ useGlobalSpecs.if { Spec.specs[this] } };
		spec.isNil.if { SimpleInputError("no spec found for key " ++ this.asString)
			.method_(thisMethod).throw; };
		^spec.miSC_specAsArray
	}


	miSC_expandCtrs { | item, i, metaKey = \specs, useGlobalSpecs = true|
		^item.(i).miSC_specAsArray(this, metaKey, useGlobalSpecs)
	}

	miSC_expandCtrPairs { | item, i, metaKey = \specs, useGlobalSpecs = true|
		^item.(i).collect { |x,j| 
			j.odd.if { this.miSC_expandCtrs(x, metaKey, useGlobalSpecs) }{ x } 
		}
	}
		
	miSC_findGlobalControlSpec { 
		var item = Spec.specs[this];
		^item.isKindOf(ControlSpec).if { item }{ nil };	}
	
	miSC_getControlTuples { | exclude, metaKey = \specs, useGlobalSpecs = true |
		// returns an array of tuples [symbol, n], where n > 1 indicates an arrayed arg
		var symbol, desc, controls, maybeValidArrayedArg;
		desc = SynthDescLib.global[this];
		desc.isNil.if {
			^SimpleInputError("SynthDef unknown to SynthDescLib.global (forgot to add ?)")
				.method_(thisMethod).throw;
		}{
			exclude = exclude.asArray;
			exclude = exclude ++ this.miSC_controlsToExclude(metaKey, useGlobalSpecs); 
			
			controls = List[];
			maybeValidArrayedArg = false;
			desc.controls.do { |control, i|
				symbol = control.name.asSymbol;
				(symbol == '?').if {
					maybeValidArrayedArg.if {
						controls.last[1] = controls.last[1] + 1;					}
				}{
					exclude.includes(symbol).if {
						maybeValidArrayedArg = false;
					}{
						controls.add([symbol, 1]);
						maybeValidArrayedArg = true;
					}
				}
			};
			^controls
		}
	}
	

	miSC_controlsToExclude { | metaKey = \specs, useGlobalSpecs = true | 
		// for associated SynthDef: gives List of control name keys with 
		// (case useGlobalSpecs = true) no metadata and no global ControlSpecs defined 
		// (case useGlobalSpecs = false) no metadata defined 
		
		var desc, controls, controlsToExclude = List.new, key, specData;
		desc = SynthDescLib.global[this];
		controls = desc.controls;
		controls.do { |controlName|
			key = controlName.name.asSymbol;
			(key != '?').if {
				specData = desc.metadata.tryPerform(\at, metaKey).tryPerform(\at, key);
				specData.isNil.if { 
					(useGlobalSpecs.not or: { key.miSC_findGlobalControlSpec.isNil }).if {
						controlsToExclude.add(key)					}
				}
			}
		};
		^controlsToExclude
	}
	
	miSC_sVarGuiSpecsFromTuples { | tuples, exclude, metaKey = \specs |
		
		// expects SequenceableCollection of tuples [symbol, n], 
		// where n > 1 indicates an arrayed arg
		 
		// miSC_pfuncPbindsFromTuples and miSC_sVarGuiSpecsFromTuples exist
		// to avoid multiple lookups in SynthDef metadata by pVarGui

		var specData, varGuiSpecs = List.new, key, num;

		tuples.do { |keyNumPair|
			#key, num = keyNumPair;
			varGuiSpecs.add(key);
			specData = SynthDescLib.global[this].metadata.tryPerform(\at, metaKey).tryPerform(\at, key);
			specData.isNil.if { 
				(specData = key.miSC_findGlobalControlSpec).isNil.if {
					SimpleInputError("no metaData for key " ++ key.asString).method_(thisMethod).throw;
				} 
			};
			specData = specData.miSC_specAsArray;
			(num > 1).if { specData = specData ! num };
			varGuiSpecs.add(specData);
		};	
		^varGuiSpecs	
	}


	pfuncPbinds { | pBefore, pReplace, pAfter, exclude, excludeGate = true, 
		excludeDur = false, excludeLegato = false, metaKey = \specs, useGlobalSpecs = true,
		post = false, trace = false, num = 1 |
		var excludeI, controlTuples, excludeFreq, pitchKeys = #[\note, \midinote, \degree];
		pBefore = pBefore ?? [];
		pReplace = pReplace ?? [];
		pAfter = pAfter ?? [];
		exclude = exclude ?? [];
		
		^num.collect { |i|
			
			// detect reserved pbind pitch controls that determine freq, then set flag excludeFreq  
			excludeFreq = pitchKeys.any { |k| pBefore.(i).includes(k) or: { pAfter.(i).includes(k) } };
			excludeI = exclude.(i).asArray;

			// controls without defined metadata / associated global ControlSpec are excluded, 
			// they could be included as explicitely given pattern pairs and controls

			controlTuples = this.miSC_getControlTuples(
				excludeI ++ (excludeGate.(i).if { [\gate] }{ [] }) ++
				(excludeFreq.if { [\freq] }{ [] }),
				metaKey.(i), useGlobalSpecs.(i)
			);
			
 			controlTuples.miSC_pfuncPbindsFromTuples(
 				pBefore.(i), pReplace.(i), pAfter.(i), this,
 				excludeI.includes(\dur), excludeI.includes(\legato), post.(i), trace.(i)
 			);
		};		
	}


	sVarGuiSpecs { | ctrBefore, ctrReplace, ctrAfter, exclude,  
		metaKey = \specs, useGlobalSpecs = true, num = 1 |
		var excludeI, controlTuples, expand, 
		ctrs = Array.newClear(num), replaceIndex;
		
		ctrBefore = ctrBefore ?? [];
		ctrReplace = ctrReplace ?? [];
		ctrAfter = ctrAfter ?? [];
		exclude = exclude ?? [];
		
		num.do { |i|	
			excludeI = exclude.(i).asArray;
			controlTuples = this.miSC_getControlTuples(
				excludeI, metaKey.(i), useGlobalSpecs.(i)
			);
			expand = { |x| this.miSC_expandCtrPairs(x, i, metaKey.(i), useGlobalSpecs.(i)) };

			ctrs[i] = expand.(ctrBefore) ++
				this.miSC_sVarGuiSpecsFromTuples(controlTuples, excludeI, metaKey.(i)) ++
				expand.(ctrAfter);

			expand.(ctrReplace)
				.pairsDo { |key, ctr|
					replaceIndex = ctrs[i].indexOf(key);
					replaceIndex.notNil.if { ctrs[i][replaceIndex + 1] = ctr };
			};
		};		
		^ctrs;
	}

	pVarGuiSpecs { | ctrBefore, ctrReplace, ctrAfter, durCtr = #[0.05, 3, \exp, 0, 0.2], 
		legatoCtr = #[0.1, 5, \exp, 0, 0.8], exclude, excludeGate = true, 
		metaKey = \specs, useGlobalSpecs = true, num = 1|
		
		^this.sVarGuiSpecs({ |i| ctrBefore.(i) ++ [\dur, durCtr.(i), \legato, legatoCtr.(i)] }, 
			ctrReplace, ctrAfter, { |i| exclude.(i).asArray ++ (excludeGate.(i).if { [\gate] }{ [] }) },
			metaKey, useGlobalSpecs, num);
	}

	
	sVarGui { | ctrBefore, ctrReplace, ctrAfter, exclude, 
		metaKey = \specs, useGlobalSpecs = true, num = 1, server|
		^VarGui(
			synthCtr: this.sVarGuiSpecs(ctrBefore, ctrReplace, ctrAfter, exclude,  
				metaKey, useGlobalSpecs, num), 
			synth: this ! num,
			server: server
		);
	}


	miSC_pVarGuiData { | ctrBefore, ctrReplace, ctrAfter, 
		durCtr = #[0.05, 3, \exp, 0, 0.2], legatoCtr = #[0.1, 5, \exp, 0, 0.8], 
		pBefore, pReplace, pAfter, exclude, excludeGate = true, clock, quant, 
		metaKey = \specs, useGlobalSpecs = true, post = false, trace = false, num = 1|
		
		var addedPairs, excludeI, controlTuples, excludeFreq = false,
			pitchKeys = #[\note, \midinote, \degree], expand1, expand2,
			ctrs = Array.newClear(num), streams = Array.newClear(num), replaceIndex;
		
		ctrBefore = ctrBefore ?? [];
		ctrReplace = ctrReplace ?? [];
		ctrAfter = ctrAfter ?? [];
		pBefore = pBefore ?? [];
		pReplace = pReplace ?? [];
		pAfter = pAfter ?? [];
		exclude = exclude ?? [];
		
		num.do { |i|
			// detect reserved pbind pitch controls that determine freq, then set flag excludeFreq  
			excludeFreq = pitchKeys.any { |k| pBefore.(i).includes(k) or: { pAfter.(i).includes(k) } };
			excludeI = exclude.(i).asArray;

			// controls without defined metadata / associated global ControlSpec are excluded, 
			// they could be included as explicitely given pattern pairs and controls
			
			controlTuples = this.miSC_getControlTuples(
				excludeI ++ (excludeGate.(i).if { [\gate] }{ [] }) ++
				(excludeFreq.if { [\freq] }{ [] }),
				metaKey.(i), useGlobalSpecs.(i)
			);
			
			expand1 = { |x| this.miSC_expandCtrPairs(x, i, metaKey.(i), useGlobalSpecs.(i)) };
			expand2 = { |x| this.miSC_expandCtrs(x, i, metaKey.(i), useGlobalSpecs.(i)) };

			ctrs[i] = expand1.(ctrBefore) ++
				(excludeI.includes(\dur).not.if { 
					[\dur, expand2.(durCtr.(i))] }{ [] }) ++
				(excludeI.includes(\legato).not.if { 
					[\legato, expand2.(legatoCtr.(i))] }{ [] }) ++
				this.miSC_sVarGuiSpecsFromTuples(controlTuples, excludeI, metaKey.(i)) ++
				expand1.(ctrAfter);

			
			expand1.(ctrReplace)
				.pairsDo { |key, ctr|
					replaceIndex = ctrs[i].indexOf(key);
					replaceIndex.notNil.if { ctrs[i][replaceIndex + 1] = ctr };
			};

 			streams[i] = controlTuples.miSC_pfuncPbindsFromTuples(
 				pBefore.(i), pReplace.(i), pAfter.(i), this,
 				excludeI.includes(\dur), excludeI.includes(\legato), post.(i), trace.(i)
 			);
		};		
		^[ctrs, streams, num.collect(clock.(_)), num.collect(quant.(_))]
	}

	pVarGui { | ctrBefore, ctrReplace, ctrAfter, 
		durCtr = #[0.05, 3, \exp, 0, 0.2], legatoCtr = #[0.1, 5, \exp, 0, 0.8], 
		pBefore, pReplace, pAfter, exclude, excludeGate = true, clock, quant, 
		metaKey = \specs, useGlobalSpecs = true, post = false, trace = false, num = 1|

		var data = this.miSC_pVarGuiData(ctrBefore, ctrReplace, ctrAfter, durCtr, legatoCtr,
			pBefore, pReplace, pAfter, exclude, excludeGate, clock, quant, metaKey, 
			useGlobalSpecs, post, trace, num);

		^VarGui(data[0], stream: data[1], clock: data[2], quant: data[3]);
	}
	
}

	
