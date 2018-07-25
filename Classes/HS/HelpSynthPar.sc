
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


HSpar : HelpSynthPar {}

HelpSynthPar : HelpSynth {
	var	<>currentSwitchIndex, <>currentHSindices, <>lastIndex, <>lastPause, <>switchCounter = -1,

		// OSC bookkeeping when PHSparPlayer or PHSusePlayer involved:

		// size fixed at init time:

		<>hsRunnings, 		// array of booleans: HelpSynth running status

		// variable size:

		<>times_switchIndices, 		// IdentityDictionary: time (in grains) -> switch index
		<>trigMsgIDs_hsIndices,		// IdentityDictionary: trigMsgID -> HelpSynth index
		<>times_hsIndices,  		// IdentityDictionary: time (in grains) -> HelpSynth index array
								// indicates all HelpSynth indices triggered at a time

								// Lists with one item for each demand index, they may grow with PHSusePlayers involved
		<>times_theseHSindices;		// List of IdentityDictionaries: time (in grains) -> HelpSynth index array
								// for each demand index and time it indicates all triggered HelpSynth indices


	*basicNew { |server, ugenFuncs, demandLatency = 0.15, respondLatency = 0.15, postOSC = false, granularity = 200, inputCheck = true|
		^super.basicNew(server, ugenFuncs, demandLatency, respondLatency, postOSC, granularity).inputCheck_(inputCheck);
	}

	*new { |server, ugenFuncs, demandLatency = 0.15, respondLatency = 0.15, postOSC = false, granularity = 200, inputCheck = true|
		^this.basicNew(server, ugenFuncs, demandLatency, respondLatency, postOSC, granularity, inputCheck).commonInit.addInit;
	}

	inputError {|str|
		Error("\n Wrong HSpar input - " ++ str ++ "\n").throw
	}

	addInputCheck {
		(ugenFuncs.isKindOf(SequenceableCollection) and:
			{ ugenFuncs.every(_.isMemberOf(Function)) } ).not.if {
				this.inputError("ugenFuncs must be a SequenceableCollection of (ugenGraph)Functions")
		};
	}

	addInit {
		inputCheck.if { this.addInputCheck };
		size = ugenFuncs.size;
		times_switchIndices = IdentityDictionary.new;
		trigMsgIDs_hsIndices = IdentityDictionary.new;
		times_hsIndices = IdentityDictionary.new;
		playingHelpSynths = Array.newClear(size);
		times_theseHSindices = List.new;
		hsRunnings = Array.fill(size, false);

		helpSynthDefs = ugenFuncs.collect {|item,i| (item.asSynthDef.name = helpSynthString ++
			"_" ++ i.asString).perform(Main.versionAtMost(3,3).if { \memStore }{ \add })
		};
		helpSynthDefs.miSC_areControlRateSynthDefs.not.if { Error("help synths should be control rate \n").throw };
		controlNames = ugenFuncs.collect({|x| x.def.argNames});
	}

	sendNewMsg { |synth, args, bus|
		server.sendBundle(demandLatency, synth.newMsg(server, [\i_out, bus, \out, bus] ++ args));
	}

	listen { |num = 1, latency|
		// num: number of demand indices
		// installs additional trigger synths for sending osc messages
		// every demand index gets trigger synths for each help synth
		var ensureListenFactor = 0.8, trigSynth, counter = 0;
		this.isListening.not.if { isListening = true; };
		num.do({|i|
			helpSynthDefs.do({|x,j|
				trigSynth = Synth.basicNew(trigString ++ \control.asString, server);
				server.sendBundle(demandLatency,
					trigSynth.newMsg(args: [\inBus, bus.index + counter] )) ;
				counter = counter + 1;
				trigSynths.add(trigSynth);
				// CmdPeriod.remove(trigSynth);
 			});
 			times_durs.add(IdentityDictionary.new);
			timeQueues.add(PriorityQueue.new);
 			times_theseHSindices.add(IdentityDictionary.new);
		});
	}

/*	makeResponder { |trigSynthID, trigMsgID|
		^OSCpathResponder(server.addr, ['/tr', trigSynthID, trigMsgID], { |time, responder, message| 			var timeGrains, hsIndex, hsIndices, val = message[3], vals;

			timeGrains = trigMsgIDs_times.removeAt(trigMsgID);
			hsIndex = trigMsgIDs_hsIndices.removeAt(trigMsgID);
		  	postOSC.if {
		  		"time in grains: ".post; timeGrains.post;
		  		"   hsIndex: ".post; hsIndex.post;
		  		"   val: ".post; val.postln;
		  	};
			hsIndices = times_hsIndices.removeAt(timeGrains);
			vals = times_vals.removeAt(timeGrains);
			vals = (vals ?? Array.newClear(size));
			vals[hsIndex] = val;
	 		times_vals.put(timeGrains, vals);
 			responder.remove;
		  	}
		)
	}*/

	makeResponder { |trigSynthID, trigMsgID|
		^OSCFunc(
			{ |message|
				var timeGrains, hsIndex, hsIndices, val = message[3], vals;

				timeGrains = trigMsgIDs_times.removeAt(trigMsgID);
				hsIndex = trigMsgIDs_hsIndices.removeAt(trigMsgID);
		  		postOSC.if {
		  			"time in grains: ".post; timeGrains.post;
		  			"   hsIndex: ".post; hsIndex.post;
		  			"   val: ".post; val.postln;
		  		};
				hsIndices = times_hsIndices.removeAt(timeGrains);
				vals = times_vals.removeAt(timeGrains);
				vals = (vals ?? Array.newClear(size));
				vals[hsIndex] = val;
	 			times_vals.put(timeGrains, vals);
		  	},
			'/tr',
			server.addr,
			argTemplate: [trigSynthID, trigMsgID]
		).oneShot
	}

	play { |index, args, switchOn, switchOff, set, hsStartIndices, latency|
		var synth, late = latency ?? demandLatency;

		this.isListening.not.if { Error("HelpSynthPar not listening \n").throw };
		switchCounter = switchCounter + 1;
		(switchCounter == 0).if {
			// .start could have been called before
			this.isPlaying.not.if {
				size.do({|i|
					synth = Synth.newPaused(helpSynthString ++ "_" ++ i.asString,
						[\i_out, bus.index + i, \out, bus.index + i] ++ ((index == i).if { args } { [] }),
						server);
					playingHelpSynths.put(i,synth);
					// CmdPeriod.remove(synth);
				})
			};
			(switchOn.if { hsStartIndices ++ [index] } { hsStartIndices }).asSet.do({|i|
				server.sendBundle(late, playingHelpSynths[i].runMsg(true));
				hsRunnings[i] = true;
				});
 		}{
			// pause last synth only if index changes
			(lastPause && (lastIndex != index)).if {
				server.sendBundle(late, playingHelpSynths[lastIndex].runMsg(false));
				hsRunnings[index] = false;
			};
			set.if {
				switchOn.if {
					server.sendBundle(late, (playingHelpSynths[index]).setMsg(*args), playingHelpSynths[index].runMsg(true));
					hsRunnings[index] = true;
				}{
					server.sendBundle(late, (playingHelpSynths[index]).setMsg(*args));
				}
			};
		};
 		lastIndex = index;
		lastPause = switchOff;
		isPlaying = true;
	}

	schedRun { |flag, latency, hsIndices|
		// this.isPlaying.not.if { Error("PHSpar not being played \n").throw };

		this.isPlaying.if {
	 		server.sendBundle(latency ?? demandLatency,
	 			*hsIndices.collect({|x| hsRunnings[x] = flag; playingHelpSynths[x].runMsg(flag)})
	 		);
		};
 	}

 	clearBookkeeping {
 		this.superPerform(\clearBookkeeping);
		times_switchIndices.clear;
		trigMsgIDs_hsIndices.clear;
		times_theseHSindices.clear;
		times_hsIndices.clear;
		hsRunnings.fill(false);

		switchCounter = -1;
		lastIndex = nil;
		lastPause = nil;
		currentSwitchIndex = nil;
 	}
}
