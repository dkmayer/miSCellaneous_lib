
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

+VarGui	{
	
	checkSingleSynthInput { |synth|
		case 	
			{ [Symbol, String].any(synth.isKindOf(_)) }
				{ 
					SynthDescLib.global[synth.asSymbol].isNil.if {
						SimpleInitError("Synth given as " ++ synth.class.asString ++ " " ++ synth.asString ++ 
							" not contained " ++ 
							"in global SynthDescLib (forgot SynthDef.add, former .memStore ?)")
							.class_(VarGui).throw;
					}
				}	
			{ [Synth, Integer].any(synth.isKindOf(_)).not }
				{ SimpleInitError("Single synth input item must be synthdef (given as Symbol or String) or " ++ 
					"Synth (directly or as nodeID integer)").class_(VarGui).throw };
		^this
	}
			
	// not a complete input check, but avoid some nearby discrepances
		
	checkArgs {|varCtr, synthCtr, stream, synth, varEnvirGroups, envir, 
			synthCtrGroups, assumePlaying, assumeRunning, assumeEnded|
		var j = 0, c, vars, synthCtrs, envirSet, synthStateVars, synthStateStrings, synthInputDup = false, result;
		
		case
			{ synth.isNil }
				{ 
					(this.synthCtr.size != 0).if {   
						SimpleInitError("synthCtr input but no synth input. " ++
							"Synth must be Symbol, String (indicating a Synthdef known by SynthDesLib), " ++ 
								"Synth or Integer (nodeID) or collection thereof.").class_(VarGui).throw;
					}{
						synths = [];
					};
				}
			{ synth.isKindOf(SequenceableCollection) }
				{ 
					synths = synth;
				}
			{ [Synth, Integer, Symbol, String].any(synth.isKindOf(_)) }
				{ 
					synths = [synth];
				}
			{ true }{ SimpleInitError("Synth must be Symbol, String (indicating a Synthdef known by SynthDesLib), " ++ 
				"Synth or Integer (nodeID) or collection thereof.").class_(VarGui).throw; };
						
		synths.do {|x| this.checkSingleSynthInput(x) };
		
		
		// prepare use of metadata for synthCtr if necessary
		
		(synthCtr.isNil && synth.notNil).if { synthCtr = (nil ! max(1, synth.size)) };

		(synthCtr.isKindOf(SequenceableCollection) and: { synthCtr.size > 0 } and:
			{ (synthCtr.first.isSequenceableCollection) || (synthCtr.first.isNil) }).if {
			synthCtr.do { |specPairs, i|
				var sym;
				specPairs.isNil.if {
					synthCtr[i] = case
						{ [Symbol, String].any(synths[i].isKindOf(_)) }
							{ synths[i].asSymbol.sVarGuiSpecs.flatten; }
						{ synths[i].isKindOf(Synth) }
							{ 
								sym = synths[i].defName.asSymbol;
								SynthDescLib.global[sym].notNil.if {
									sym.sVarGuiSpecs.flatten;
								}{
									[]
								};
							};
					specPairs = synthCtr[i];
				}
			}
		};
			
		
		// this.synthCtr stores flattened (ungrouped) ctrs
			 
		this.synthCtr = case
			{ synthCtr.isNil }{ [] }
			{ synthCtr.first.isKindOf(SequenceableCollection) }{ 
				// check if ctrs given as keys to be looked up as global ControlSpec
				c = synthCtr.flatten;
				c.do { |item, i| i.odd.if { c[i] = c[i].miSC_specAsArray } };
			} 
			{ true }{ synthCtr };


		synthCtrs = this.synthCtr.select {|x,i| i.even };  


		synthStateVars = [assumePlaying, assumeRunning, assumeEnded];
		synthStateStrings = ["assumePlaying", "assumeRunning", "assumeEnded"];
		
		synthStateStrings.do {|x,i|
			case
				{ synthStateVars[i].isKindOf(SequenceableCollection) }
					{ 
						(synthStateVars[i].every {|x| [Boolean, Nil].any(x.isKindOf(_)) } && 
							(synthStateVars[i].size == synths.size)).not.if {   
								SimpleInitError(x ++ " input as collection must equal number of synths, " ++
									"items must be Boolean or Nil").class_(VarGui).throw;
						}{
							this.perform((x ++ "_").asSymbol, synthStateVars[i]);
						}
					}
				{ [Boolean, Nil].any(synthStateVars[i].isKindOf(_)) }
					{ 
						this.perform((x ++ "_").asSymbol, synthStateVars[i].dup(synths.size));
					}
				{ true }{ SimpleInitError(x ++ " must be Boolean, Nil or collection thereof, " ++
							"in this case size must corespond with synthCtr").class_(VarGui).throw; };
		};
		
		this.assumeEnded.do {|x,i|
			((x == true) && ((this.assumePlaying[i] == true) || (this.assumeRunning[i] == true))).if {
				SimpleInitError("Contradictory assumptions about synth state, there must not be a player " ++ "
					with assumeEnded set true " ++
					"together with assumePlaying or " ++ "assumeRunning set true").class_(VarGui).throw;
			};
			((this.assumePlaying[i] == false) && (this.assumeRunning[i] == true)).if {
				SimpleInitError("Contradictory assumptions about synth state, there must not be a " ++ "
					player with assumePlaying set false " ++
					"and assumeRunning set true").class_(VarGui).throw;
			};		
		};
				
		synthStateStrings.do {|x,i|
			this.perform(x.asSymbol).do {|y,j|
				(([Symbol, String].any(synths[j].isKindOf(_))) && (y.notNil)).if {
					SimpleInitError("There must not be any assume flag set to a Boolean for a " ++ "
						Symbol / String as synth input, " ++
						"assumptions are only allowed for Synths and nodeIDs.").class_(VarGui).throw;
				}
			}
		}; 
			
		synthIDs = synths.collect {|x| [Synth, Integer].any(x.isKindOf(_)).if { x.asNodeID } };
		
		stream = stream.miSC_defNameAsArray;
		streamEnvirs = List.new;
		streamEnvirIndices = Array.newClear(stream.size);
		streams = Array.newClear(stream.size);

		stream.do {|x, i|
			var e, k;
			case
				{ x.isKindOf(Function) }{ e = Environment.new; streams[i] = e.use { Task(x) } }
				{ x.isKindOf(Pattern) }{ e = Environment.new; streams[i] = e.use { x.asEventStreamPlayer } }
				{ x.isKindOf(Symbol) || x.isKindOf(String) }
					{ e = Environment.new; streams[i] = e.use { x.asSymbol.pfuncPbinds.at(0).asEventStreamPlayer } }
				{ true }{
					x.isKindOf(PauseStream).not.if {
						SimpleInitError("Stream must be Task, Task function, Pattern, " ++
							"EventStreamPlayer, Symbol, String or collection of such").class_(VarGui).throw;
					}{
						e = x.originalStream.miSC_getEnvironment;
						k = streamEnvirs.detectIndex(_ === e);
						streams[i] = x;
					}
				};
			k.notNil.if {
				streamEnvirIndices[i] = k;
			}{
				streamEnvirs = streamEnvirs.add(e);
				streamEnvirIndices[i] = j;
				j = j + 1;
			};
		};
		
		envir.isNil.if {
			envirs = (stream.size == 0).if { [currentEnvironment] }{ streamEnvirs };
		}{
			(stream.size != 0).if { SimpleInitError("Passing an envir arg only " ++ 
					"allowed without stream players").class_(VarGui).throw };
			envirs = case
				{ envir.isKindOf(Environment) }{ [envir] }
				{ envir.isSequenceableCollection }{ envir }
				{ true }{ SimpleInitError("Envir arg  must be Environment or SequenceableCollection")
					.class_(VarGui).throw; };
			
			envirSet = IdentitySet.new;
			envirs.do { |envir| envirSet = envirSet.add(envir) };
			
			((envirs.any(_.isKindOf(Environment).not)) || (envirSet.size != envirs.size)).if {
				SimpleInitError("Envir arg must be Environment or SequenceableCollection of " ++ 
					"non-identical Environments").class_(VarGui).throw;
			};
		
		};


		// prepare use of metadata for varCtr if necessary
		// this.varCtr stores flattened (ungrouped) ctrs

		(varCtr.isNil && stream.notNil).if { varCtr = (nil ! max(1, stream.size)) };
		
		(varCtr.isKindOf(SequenceableCollection) and: { varCtr.size > 0 } and:
			{ (varCtr.first.isSequenceableCollection) || (varCtr.first.isNil) }).if {
				
			// case grouped varCtr input, some items may be nil, will look for metadata then	
			varEnvirGroups.notNil.if {
				SimpleInitError("varEnvirGroups must not be given if varCtr is already grouped, " ++ 
					"means a collection of specPair collections").class_(VarGui).throw;
			};
			hasConnectedVarEnvirGroups = true;
			envir.isNil.if {
				(stream.size != 0).if {
					((varCtr.size) > (streamEnvirs.size)).if {
						SimpleInitError("Size of varCtr exceeds number of implicitely given stream envirs")
							.class_(VarGui).throw;
					}
				}{
					((varCtr.size) > 1).if {
						SimpleInitError("Size of varCtr > 1 but no implicitely or explicitely given envirs")
							.class_(VarGui).throw;
					}
				}
			}{	// can only happen if no streams 
				((varCtr.size) > (envirs.size)).if {
					SimpleInitError("Size of varCtr exceeds number of explicitely given envirs")
						.class_(VarGui).throw;
				};
			};
			varCtr.do { |specPairs, i|
				var envirKeys, c; 
				
				specPairs.isNil.if {
					varCtr[i] = ([Symbol, String].any(stream[i].isKindOf(_))).if {
						stream[i].asSymbol.pVarGuiSpecs.flatten;
					}{
						[]
					};
					specPairs = varCtr[i];
				};

				this.varCtr = varCtr.flatten;	

				specPairs.do { |item, j| 
					j.even.if { 
						varEnvirIndices = varEnvirIndices.add(i);
						envirKeys = envirKeys.add(item); 
					} 
				};
				(envirKeys.miSC_asSet.size != envirKeys.size).if {
					SimpleInitError("Bad definition: specPairs defined for one envir contain " ++ 
						"var name more than once").class_(VarGui).throw;
				} 
			};
			vars = this.varCtr.select {|x,i| i.even }; 
			
		}{
			
			this.varCtr = varCtr.isNil.if { [] }{ varCtr };
			vars = this.varCtr.select {|x,i| i.even }; 
			
			varEnvirGroups.isNil.if {
				(vars.size != vars.miSC_asSet.size).if {
					SimpleInitError("varCtr contains multiple keys but varEnvirGroups not defined")
						.class_(VarGui).throw; 
				};
				hasConnectedVarEnvirGroups = true;
				varEnvirIndices = vars.size.collect { 0 };
			}{
				varEnvirGroups.miSC_isGrouping(vars.size).not.if {
					SimpleInitError("varEnvirGroups must be a valid grouping of number of vars. " ++ 
						"E.g. [[3,0], [1,2,4]] is a valid grouping of 5").class_(VarGui).throw; 
				};
				hasConnectedVarEnvirGroups = varEnvirGroups.miSC_isConnected;
				(varEnvirGroups.size > envirs.size).if {
					SimpleInitError("Size of varEnvirGroups must less or equal " ++ envirs.size.asString ++ 
						", the number of " ++ (envir.notNil.if { "ex" }{ "im" }) ++  "plicitely given envirs")
						.class_(VarGui).throw; 
				};
				varEnvirIndices = Array.newClear(vars.size);
				varEnvirGroups.do { |x,i|
					var y = vars[x];
					(y.size != y.miSC_asSet.size).if { 
						SimpleInitError("Bad definition: envirGroup " ++ x.asString ++ 
							" contains var name more than once").class_(VarGui).throw;
					};
					x.do(varEnvirIndices[_] = i);
				};
			};
		};				
		// check if ctrs given as keys to be looked up as global ControlSpec
		this.varCtr.do { |item, i| i.odd.if { this.varCtr[i] = this.varCtr[i].miSC_specAsArray } };

		synthCtr.first.isSequenceableCollection.if {
			synthCtrGroups.notNil.if {
				SimpleInitError("synthCtrGroups must not be given if synthCtr is already grouped, " ++ 
					"means a collection of specPair collections").class_(VarGui).throw;
			};
			hasConnectedSynthCtrGroups = true;
				
			(synthCtr.size > synths.size).if {
				SimpleInitError("Size of synthCtr exceeds number of given synths").class_(VarGui).throw;
			};
			synthCtr.do { |specPairs, i|
				var synthKeys; 
				specPairs.do { |item, j| 
					j.even.if { 
						synthCtrSynthIndices = synthCtrSynthIndices.add(i);
						synthKeys = synthKeys.add(item); 
					} 
				};
				(synthKeys.miSC_asSet.size != synthKeys.size).if {
					SimpleInitError("Bad definition: specPairs defined for one synth " ++ 
						"contain ctr name more than once").class_(VarGui).throw;
				} 
			};
			
		}{
			synthCtrGroups.isNil.if {
				(synthCtrs.size != synthCtrs.miSC_asSet.size).if {
					SimpleInitError("synthCtr contains multiple keys but synthCtrGroups not defined")
						.class_(VarGui).throw; 
				};
				hasConnectedSynthCtrGroups = true;
				synthCtrSynthIndices = synthCtrs.size.collect { 0 };
			}{
				synthCtrGroups.miSC_isGrouping(synthCtrs.size).not.if {
					SimpleInitError("synthCtrGroups must be a valid grouping of number of synthCtrs. " ++ 
						"E.g. [[3,0], [1,2,4]] is a valid grouping of 5").class_(VarGui).throw; 
				};
				hasConnectedSynthCtrGroups = synthCtrGroups.miSC_isConnected;
				(synthCtrGroups.size > synths.size).if {
					SimpleInitError("Size of synthCtrGroups must be less or equal " ++ synths.size.asString ++ 
						", the number of given synths").class_(VarGui).throw; 
				};
				synthCtrSynthIndices = Array.newClear(synthCtrs.size);
				synthCtrGroups.do { |x,i|
					var y = synthCtrs[x];
					(y.size != y.miSC_asSet.size).if { 
						SimpleInitError("Bad definition: synthCtrGroup " ++ x.asString ++ 
							" contains synth controlname more than once").class_(VarGui).throw;
					};
					x.do(synthCtrSynthIndices[_] = i);
				};
			};
		};
		
		// check if ctrs given as keys to be looked up as global ControlSpec
		this.synthCtr.do { |item, i| i.odd.if { this.synthCtr[i] = this.synthCtr[i].miSC_specAsArray } };
				
		hasConnectedSynthCtrGroups.if {
			firstSynthCtrIndices = synths.collect { |synth, i| synthCtrSynthIndices.tryPerform(\indexOf,i) };
		};
	
		(envir.notNil && (varEnvirIndices.miSC_asSet.size < envirs.size)).if { 
			SimpleInitError("Bad definition: unused explicitely given envirs").class_(VarGui).throw;
		};
	
		varColorNum = varEnvirIndices.miSC_asSet.size;
		synthColorNum = synthCtrSynthIndices.miSC_asSet.size;
			
		^this;	
	}
}

