
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

HS : HelpSynth {}

HelpSynth {
	classvar <>all;
	var 	<>ugenFuncs, <>helpSynthDefs, <>playingHelpSynths,
		<>size, <>bus, <>server, <>helpSynthString, <>controlNames,
		<>trigSynths, <>trigSynthDef, <>trigString, <>initstring, <>inputErrString, <>postOSC,
		<>trigMsgIDCounter = -1, <>demandIndexOffset = 0, <>lastDemandIndexOffset = 0, <>inputCheck,
		<>isListening = false, <>isPlaying = false, <>isRunning = false, <>isPHelpSynthPlaying = false,
		<>hasJustStartedWithPause = false,

		<>granularity = 200,  	// time grains per second, quantization for bookkeeping (demand message comprehension)
							// not used for scheduling events
		<>initSeconds,     	// time grains reset at PHS player start

		<>helpSynthAhead = 0.0001,
		<>cleanupDelay = 0.01,
		<>sequentialSchedShift = 0.0001, // scheduling events at same logical time
		<>demandLatency,  	// = help synth latency
		<>respondLatency,	// time given to the response to be safely received by the client on time (at total latency)
		<>totalLatency, 	// demandLatency + respondLatency =
						// total (client side) logical difference between demand event and receive event

		<>mainPHSplayer, <>otherPHSusePlayers,
		<>demandStreams_indexOffsets, <>receiveStreams_indexOffsets,

		// OSC bookkeeping when PHSparPlayer or PHSusePlayer involved:

		<>trigMsgIDs_times, 	// IdentityDictionary: trigMsgID -> time (in grains)
		<>times_demandNums, 	// IdentityDictionary: time (in grains) -> demand num / array of (hs indexed) demand nums
		<>times_vals,			// IdentityDictionary: time (in grains) -> synth value / synth value array

						// Lists with one item for each demand index, they may grow with PHSusePlayers involved
		<>times_durs,		// List of IdentityDictionaries: time (in grains) -> duration (in beats)
		<>timeQueues; 	// List of PriorityQueues
	*basicNew { |server, ugenFunc, demandLatency = 0.15, respondLatency = 0.15, postOSC = false, granularity = 200, inputCheck = true|
		^super.new.ugenFuncs_(ugenFunc).server_(server).demandLatency_(demandLatency)
			.respondLatency_(respondLatency).postOSC_(postOSC).granularity_(granularity).inputCheck_(inputCheck);
	 }

	*new { |server, ugenFunc, demandLatency = 0.15, respondLatency = 0.15, postOSC = false, granularity = 200, inputCheck = true|
		^this.basicNew(server, ugenFunc, demandLatency, respondLatency, postOSC, granularity, inputCheck).commonInit.addInit;
	 }

	*add { |helpSynth|
		var old;
		old = all.findMatch(helpSynth);
		old.notNil.if { all.remove(old) };
		all.add(helpSynth);
	}

	*remove { |helpSynth| all.remove(helpSynth) }

	*clear { all = Set[] }

	add { this.class.add(this)  }

	remove { all.remove(this) }

	inputError {|str|
		Error("\n Wrong HS input - " ++ str ++ "\n").throw
	}

	commonInputCheck {
		(server.isMemberOf(Server) and: { server.serverRunning }).not.if {
			this.inputError("server must be a running Server")
		};
		(demandLatency.isKindOf(Number) and: { demandLatency > 0 } ).not.if {
			this.inputError("demandLatency must be a Number > 0")
		};
		(respondLatency.isKindOf(Number) and: { respondLatency > 0 } ).not.if {
			this.inputError("respondLatency must be a Number > 0")
		};
		(postOSC.isNil or: { postOSC.isKindOf(Boolean) } ).not.if {
			this.inputError("postOSC must be a Boolean")
		};
		(granularity.isKindOf(Integer) and: { granularity > 0 } ).not.if {
			this.inputError("granularity must be an Integer > 0")
		};
	}

	addInputCheck {
		ugenFuncs.isMemberOf(Function).not.if {
			this.inputError("ugenFunc must be a (ugenGraph)Function")
		};
	}

	commonInit {
		inputCheck.if { this.commonInputCheck };
		totalLatency = demandLatency + respondLatency;
		trigMsgIDs_times = IdentityDictionary.new;

		times_vals = IdentityDictionary.new;
		times_demandNums = IdentityDictionary.new;
		demandStreams_indexOffsets = IdentityDictionary.new;
		receiveStreams_indexOffsets = IdentityDictionary.new;
		times_durs = List.new;
		timeQueues = List.new;

		this.class.all.isNil.if { this.class.all = Set[] };
		this.add;

		otherPHSusePlayers = List.new;
		initstring = Main.elapsedTime.asString;
		helpSynthString = "hs" ++ initstring;
		trigString = "trig" ++ initstring;
 		trigSynthDef = this.makeTrigSynthDef;
 		trigSynths = List.new;
 		this.resetInitSeconds;
 	}

	addInit {
		size = 1;
		inputCheck.if { this.addInputCheck };
		helpSynthDefs = [(ugenFuncs.asSynthDef.name = helpSynthString).perform(Main.versionAtMost(3,3).if { \memStore }{ \add })];
		helpSynthDefs.miSC_areControlRateSynthDefs.not.if { Error("help synths should be control rate \n").throw };
		controlNames = ugenFuncs.def.argNames;
		playingHelpSynths = Array.newClear(1);
	}

	resetInitSeconds { initSeconds = Main.elapsedTime; }

	allocBus {
		// PHSplayer controls allocation and deallocation
		bus.isNil.if {
			bus = Bus.control(server, size);
		}{
			"Bus already allocated".warn;
		}
	}

	makeTrigSynthDef {
		^SynthDef(trigString ++ "control", { |t_tr = 0, trigMsgID, inBus|
				SendTrig.kr(t_tr, trigMsgID, In.kr(inBus,1))
		  	}, [\tr]).send(server);
	}

	listen { |num = 1, latency|
		// installs additional num trigger synths for sending osc messages
		// every demand index gets its own trigger synth
		var ensureListenFactor = 0.8;
		this.isListening.not.if { isListening = true; };
		num.do({ |i|
			var trigSynth = Synth.basicNew(trigString ++ \control.asString, server);
			server.sendBundle(latency ?? demandLatency * ensureListenFactor,
				trigSynth.newMsg(args: [\inBus, bus] )) ;
			trigSynths.add(trigSynth);

			times_durs.add(IdentityDictionary.new);
			timeQueues.add(PriorityQueue.new);
			// CmdPeriod.remove(trigSynth);
		});
	}

	// makeResponder { |trigSynthID, trigMsgID|
	// 	^OSCpathResponder(server.addr, ['/tr', trigSynthID, trigMsgID], { |time, responder, message|
	// 		var timeGrains, demandIndices, val = message[3];
	//
	// 		timeGrains = trigMsgIDs_times.removeAt(trigMsgID);
	// 		postOSC.if {
	// 			"time in grains: ".post; timeGrains.post;
	// 			"   val: ".post; val.postln;
	// 		};
	// 		times_vals.put(timeGrains, val);
	// 		responder.remove;
	// 	});
	// }

	makeResponder { |trigSynthID, trigMsgID|
		^OSCFunc(
			{ |message|
				var timeGrains, demandIndices, val = message[3];

				timeGrains = trigMsgIDs_times.removeAt(trigMsgID);
				postOSC.if {
		  			"time in grains: ".post; timeGrains.post;
		  			"   val: ".post; val.postln;
		  		};
	 			times_vals.put(timeGrains, val);
 			},
			'/tr',
			server.addr,
			argTemplate: [trigSynthID, trigMsgID]
		).oneShot;
	}

	updateDemandIndexOffsets { |pHelpSynthUse|
		lastDemandIndexOffset = demandIndexOffset;
		demandIndexOffset = demandIndexOffset + pHelpSynthUse.relDemandIndexUserNums.size;
	}

	play { |args, latency|
		var synth = Synth.basicNew(helpSynthString, server);
		playingHelpSynths.put(0, synth);
 		server.sendBundle(latency ?? demandLatency, synth.newMsg(server,
 			[\i_out, bus.index, \out, bus] ++ (args ?? []) ));
		// CmdPeriod.remove(synth);
 		isPlaying = true;
 		isRunning = true;
 	}

	schedRun { |flag, latency|
//		this.isPlaying.not.if { Error("HS not being played \n").throw };
//	 	server.sendBundle(latency ?? demandLatency, playingHelpSynths.at(0).runMsg(flag));
// 		isRunning = flag;

 		this.isPlaying.if {
	 		server.sendBundle(latency ?? demandLatency, playingHelpSynths.at(0).runMsg(flag));
 			isRunning = flag;
 		};

 	}

	sleep { |latency|
		this.isListening.if {
			trigSynths.do({|x| server.sendBundle(latency ?? demandLatency, x.freeMsg); });
			trigSynths = List.new;
 			isListening = false;
 		}
 	}

	stop { |latency|
		this.isPlaying.if {
			playingHelpSynths.do({ |x| x.notNil.if { server.sendBundle(latency ?? demandLatency, x.freeMsg) } }); 		   	isPlaying = false;
 		   	isRunning = false;
 		};
 		SystemClock.sched((latency ?? demandLatency) + cleanupDelay, { this.clearBookkeeping; });
 	}

 	clearBookkeeping {
 		trigMsgIDs_times.clear;
 		times_vals.clear;
 		times_durs.clear;
 		timeQueues.clear;
 		times_demandNums.clear;
		demandStreams_indexOffsets.clear;
		receiveStreams_indexOffsets.clear;

 		otherPHSusePlayers.clear;
 		mainPHSplayer = nil;
 		playingHelpSynths.clear;
		demandIndexOffset = 0;
		lastDemandIndexOffset = 0;
		trigMsgIDCounter = -1;
 	}

	free { |latency|
		var late = latency ?? demandLatency;
		this.isPHelpSynthPlaying.if {
			mainPHSplayer.free; // calls free again
		}{
			this.stop(late).sleep(late).busFree(late);
		}
	}

	busFree { |latency|
		SystemClock.sched((latency ?? demandLatency) + cleanupDelay, { bus.notNil.if { bus.free; bus = nil; } })
	}

	makeAdaptedQuants { |clock, quant, adaptQuant = true, quantBufferTime = 0.2, startQuantDelay = 0|
		// quantBufferTime, startQuantDelay in seconds
		// startQuantDelay used for resume in case of help synth switch
		var startQuant, demandQuant, receiveQuant, qu, ph, off,
			beatsToNextTimeOnGrid, periodShiftFactor, secureNextTime,
			nextTimeOnGridCheckSum, nextTimeOnGridIsPossible;

		(quant.isKindOf(Quant) || quant.isNil).not.if {
			Error("quant must be instance of Quant or nil").throw;
		};

		qu = quant.isNil.if { 0 } { quant.quant ?? 0 };
		ph = quant.isNil.if { 0 } { quant.phase ?? 0 };
		off = quant.isNil.if { 0 } { quant.timingOffset ?? 0 };

		adaptQuant.if {
			[qu,ph,off].do({|x|
				((x.isNumber) &&  (x >= 0)).not.if { Error("quant, phase and offset must be numbers >= 0").throw; }
			});

			beatsToNextTimeOnGrid = (clock.nextTimeOnGrid(qu,ph) - clock.beats);
			// nextTimeToGrid must allow total latency to take place before
			nextTimeOnGridCheckSum = beatsToNextTimeOnGrid - ((totalLatency + quantBufferTime + helpSynthAhead) * clock.tempo);
			nextTimeOnGridIsPossible = (nextTimeOnGridCheckSum > 0);

			// secureNextTime in beats !
			(qu == 0).if {
				// qu = 0 and ph = 0: "as soon as possible"
				// ph > 0 is overruled, if it doesn't allow required latency
				secureNextTime = max( (totalLatency + quantBufferTime + helpSynthAhead) * clock.tempo, ph);
			}{
				nextTimeOnGridIsPossible.if {
					secureNextTime = helpSynthAhead * clock.tempo + beatsToNextTimeOnGrid;
				}{
					// step to the nearest period with enough headroom
					periodShiftFactor = (nextTimeOnGridCheckSum.neg / qu).ceil;
					secureNextTime = periodShiftFactor * qu + beatsToNextTimeOnGrid + (helpSynthAhead * clock.tempo);
				}
			}
		};
		startQuant = Quant(qu, clock.miSC_getPhaseFromTimeToNextBeat(qu, secureNextTime - ((helpSynthAhead + totalLatency - startQuantDelay) * clock.tempo)), off);
		demandQuant = Quant(qu, clock.miSC_getPhaseFromTimeToNextBeat(qu, secureNextTime - (totalLatency * clock.tempo)), off);
		receiveQuant = Quant(qu, clock.miSC_getPhaseFromTimeToNextBeat(qu, secureNextTime), off);
		^[startQuant, demandQuant, receiveQuant];
	}

	currentAdaptedQuant { |clock, quant, adaptQuant = true, quantBufferTime = 0.2|
		^this.makeAdaptedQuants(clock, quant, adaptQuant, quantBufferTime).at(2);
	}

	nextTrigMsgID { ^trigMsgIDCounter = trigMsgIDCounter + 1 }
	isPHSplaying { ^this.isPHelpSynthPlaying }
	isPHSplaying_ {|x| this.isPHelpSynthPlaying_(x) }
}
		
