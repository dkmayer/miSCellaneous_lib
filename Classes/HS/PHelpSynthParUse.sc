
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


PHSparUse : PHelpSynthParUse {}

PHelpSynthParUse : PHelpSynthUse { 
	
	*basicNew { |helpSynth, pbindArgs, hsIndices|
		^super.basicNew(helpSynth, *pbindArgs).hsIndices_(hsIndices);
	}

	*new { |helpSynth, pbindArgs, hsIndices|
		^this.basicNew(helpSynth, pbindArgs, hsIndices).commonInit;
	}
	
	inputError { |str|
		Error("\n Wrong PHSparUse input - " ++ str ++ "\n").throw 
	}
	
	pbindHelpMsg {
		^"pbindArgs must be collection of the form: [dur1, pbindData1, ... , durN, pbindDataN] \n" ++
		"dur i: duration value or pattern / stream of durations for corresponding Pbind(s) \n" ++
		"pbindData i: a collection of Pbind pairs or a collection of Pbind pair collections, \n" ++
		"defining possibly several Pbinds with same event timing \n"
	}

	getReservedKeys { ^[\demandIndex, \timeGrains, \vals, \theseIndices, \switchIndex, \theseVals, \thisVal, \dur, \val, \receiveCleanup] }

	addInputCheck { 
		var hsIndicesMsg = "hsIndices must be collection of possible hsIndex inputs, \n" ++
				"its size must equal the number of demands in pbindArgs. \n" ++
				"Each hsIndex must either be a possible integer HelpSynth index, one of the symbols \\all, \\switch, \n" ++
				"a collection of unequal HelpSynth indices or a pattern / stream, which produces such items. \n";
		helpSynth.isKindOf(HelpSynthPar).not.if {
			this.inputError("helpSynthPar must be an instance of HSpar")
		};
		(hsIndices.isNil || (hsIndices.isKindOf(SequenceableCollection) and: { 
				(hsIndices.size == (pbindArgs.size.div(2))) and: {
					hsIndices.miSC_isKindOfPossibleHSindices(pbindArgs.size.div(2), helpSynth.size)
				}
			})
		).not.if {
			this.inputError(hsIndicesMsg)
		};		
	}

	makeDemandPattern { |relDemandIndex, demandDur, demandIndexUserNum, demandHSIndices|
		^Pbind(
			\dur, demandDur,
			\demandHSIndices, demandHSIndices,
			\type, \rest,
			\dummy, Pfunc({|e| e.use({
				var timeGrains = ((thisThread.seconds  - helpSynth.initSeconds) * helpSynth.granularity).round.asInteger,
					trigSynth, trigMsgID, trigSynthID, responder, demandIndex, thisDemandIndexOffset, switchIndex, demandNum, x,
					oldHSindices, newHSindices, theseHSindices;
				
				// only one trigger at a time, but it may cover more than one demand 
				thisDemandIndexOffset = helpSynth.demandStreams_indexOffsets.at(thisThread.miSC_getParent);
				demandIndex = thisDemandIndexOffset + relDemandIndex;
			
				// make entry times_durs
				helpSynth.times_durs.at(demandIndex).put(timeGrains, ~dur );
			
				// put value in prioirity queue once for each demand index
	 			(helpSynth.timeQueues.at(demandIndex)).put(timeGrains, timeGrains);

				switchIndex = helpSynth.times_switchIndices.at(timeGrains) ?? 
					(x = helpSynth.currentSwitchIndex; helpSynth.times_switchIndices.put(timeGrains, x); x);

				oldHSindices = helpSynth.times_hsIndices.at(timeGrains);
				theseHSindices = case 
					{ ~demandHSIndices.isNil || (~demandHSIndices == \switch) } { [switchIndex] }
					{ ~demandHSIndices == \all } { (0..(helpSynth.size - 1)) }
					{ true } { ~demandHSIndices.asArray };
		
				helpSynth.times_theseHSindices.at(demandIndex).put(timeGrains, theseHSindices);
			
				newHSindices = oldHSindices.isNil.if {
					theseHSindices.asSet
				}{
				   theseHSindices.asSet - oldHSindices.asSet;
				};
			
				demandNum = helpSynth.times_demandNums.removeAt(timeGrains) ?? 0;
				// 	make entry times_hsIndices
				helpSynth.times_hsIndices.put(timeGrains, (oldHSindices ++ newHSindices).asArray.sort);

				newHSindices.do({|i|
					var trigSynthIndex = thisDemandIndexOffset * helpSynth.size + i;
				
					trigMsgID = helpSynth.nextTrigMsgID;
					trigSynthID = helpSynth.trigSynths.at(trigSynthIndex).nodeID;
					responder = helpSynth.makeResponder(trigSynthID, trigMsgID);
					responder.add;
				
					//	make entry trigMsgIDs_times
					helpSynth.trigMsgIDs_times.put(trigMsgID, timeGrains);
	
					//	make entry trigMsgIDs_hsIndices
					helpSynth.trigMsgIDs_hsIndices.put(trigMsgID, i);

					helpSynth.playingHelpSynths.at(i).isNil.if {
						Error("trying to poll a value from a help synth not playing, as " ++ 
							"it hasn't been switched before (maybe set hsStartIndices = \all)").throw;
					}{
	 					helpSynth.server.sendBundle(helpSynth.demandLatency, ["/n_set", trigSynthID, \t_tr, 1], 
							["/n_set", trigSynthID, \trigMsgID, trigMsgID]);
					}
				});
				demandNum = demandNum + 1;
			
				// 	make entry times_demandNums
				helpSynth.times_demandNums.put(timeGrains, demandNum);				})
	 		})
		)
	}

	makeReceivePatternProtoArgs { |relDemandIndex, repeats|
		^[\demandIndex, 
			Pstutter(repeats, Pfunc({ |e| e.use { 
				helpSynth.receiveStreams_indexOffsets.at(thisThread.miSC_getParent.miSC_getParent) + 					relDemandIndex;
			}})).asStream,
		\timeGrains, 
			Pstutter(repeats, Pfunc({ |e| e.use {
				this.helpSynth.timeQueues.at(~demandIndex).pop.asInteger; 
			}})).asStream,
		\vals, 
			Pstutter(repeats, Pfunc({|e| e.use {
				helpSynth.times_vals.at(~timeGrains); 
			}})).asStream,
		\theseIndices, 
			Pstutter(repeats, Pfunc({|e| e.use { 
				helpSynth.times_theseHSindices.at(~demandIndex).at(~timeGrains); 
			}})).asStream,
		\switchIndex, 
			Pstutter(repeats, Pfunc({|e| e.use { 
				helpSynth.times_switchIndices.at(~timeGrains); 
			}})).asStream,
		\theseVals, 
			Pstutter(repeats, Pfunc({|e| e.use { 
				~vals.at(~theseIndices); 
			}})).asStream,
		\thisVal, 
			Pstutter(repeats, Pfunc({|e| e.use { 
				~theseVals.at(0) 
			}})).asStream,
		\dur, 
			Pstutter(repeats, Pfunc({ |e| e.use { 
				var maybeDur;
				maybeDur = (helpSynth.times_durs.at(~demandIndex)).at(~timeGrains);
				maybeDur ?? {  Error(" no registered duration at time in ms: " ++ 					~timeGrains.asString).throw; }
			}})).asStream,
		\val, 
			Pstutter(repeats, Pfunc({ |e| e.use { 
				var maybeVal, theseDemandIndices;
			
				maybeVal = ~vals.at(~switchIndex);
				maybeVal ?? ~thisVal ?? { Error(" no registered duration at time in ms: " ++ ~timeGrains.asString).throw; }
			}})).asStream,
		\receiveCleanup, 
			Pstutter(repeats, Pfunc({ |e| e.use {
				var demandNum;
			
				(helpSynth.times_durs.at(~demandIndex)).removeAt(~timeGrains);
				(helpSynth.times_theseHSindices.at(~demandIndex)).removeAt(~timeGrains); 
				demandNum = helpSynth.times_demandNums.removeAt(~timeGrains);
				(demandNum == 1).if {
					helpSynth.times_vals.removeAt(~timeGrains);
					helpSynth.times_switchIndices.removeAt(~timeGrains);
				}{
					helpSynth.times_demandNums.put(~timeGrains, demandNum - 1)
				}
			}})).asStream
		]
	}
	
	play { |clock, quant|
		var warnMsg = "No PHSparPlayer - " ++
			"Playing a PHSparUse needs a PHSpar being played\n";

		helpSynth.isPHSplaying.not.if { 
			warnMsg.warn; 
		}{
			^PHelpSynthUsePlayer(this).play(clock, quant.asQuant);
		}
	}
}

