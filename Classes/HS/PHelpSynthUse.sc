
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


PHSuse : PHelpSynthUse {}

PHelpSynthUse {
	var <>helpSynth, <>pbindArgs, <>demandPattern, <>receivePattern, <>relDemandIndexUserNums, <>hsIndices;
	
	*basicNew { |helpSynth ... pbindArgs|
		^super.new.helpSynth_(helpSynth).pbindArgs_(pbindArgs);
	}

	*new { |helpSynth ... pbindArgs|
		^this.basicNew(helpSynth, *pbindArgs).commonInit;
	}

	inputError { |str|
		Error("\n Wrong PHSuse input - " ++ str ++ "\n").throw 
	}
	
	pbindHelpMsg {
		^"pbindArgs (not collected) must be of the form: dur1, pbindData1, ... , durN, pbindDataN \n" ++
		"dur i: duration value or pattern / stream of durations for corresponding Pbind(s) \n" ++
		"pbindData i: a collection of Pbind pairs or a collection of Pbind pair collections, \n" ++
		"defining possibly several Pbinds with same event timing \n"
	}
	
	getReservedKeys { ^[\demandIndex, \timeGrains, \dur, \val, \receiveCleanup] }
	
	commonInputCheck { 
		var reservedKeys = this.getReservedKeys;
		
		(pbindArgs.isKindOf(SequenceableCollection) and: { pbindArgs.miSC_isPossiblePbindArgs }).not.if {
			this.inputError(this.pbindHelpMsg)
		};
		pbindArgs.miSC_hasNoReservedKeysInPbindArgs(reservedKeys).not.if {
			this.inputError("Pbind keys must not contain one of the reserved keys: \n" ++
				reservedKeys.asString ++ "\n\n" ++ this.pbindHelpMsg
			)
		};
	}

	addInputCheck { 
		helpSynth.isKindOf(HelpSynthPar).if {
			this.inputError("helpSynth must be an instance of HS")
		};
	}

	commonInit { 
		helpSynth.isKindOf(HelpSynth).not.if {
			this.inputError("helpSynth must be an instance of HS or HSpar")
		};
		helpSynth.inputCheck.if { this.commonInputCheck.addInputCheck };
		relDemandIndexUserNums = List.new;
		# demandPattern, receivePattern = this.makeDemandAndReceivePatterns(pbindArgs);
	}

	makeDemandPattern { |relDemandIndex, durPat, demandIndexUserNum, hsIndices|
		^Pbind(
			\dur, durPat,
			\type, \rest,
			\dummy, Pfunc({|e| 
				var timeGrains = ((thisThread.seconds  - helpSynth.initSeconds) * helpSynth.granularity).round.asInteger,
					trigSynth, trigMsgID, trigSynthID, demandNum, responder, demandIndex;
				
				// only one trigger at a time, but it may cover more than one demand 
				demandIndex = helpSynth.demandStreams_indexOffsets.at(thisThread.miSC_getParent) + relDemandIndex;
				trigSynthID = helpSynth.trigSynths.at(demandIndex).nodeID;
	
				// make entry times_durs
				helpSynth.times_durs.at(demandIndex).put(timeGrains, e.use({~dur}) );

				// put value in prioirity queue once for each demand index
	 			(helpSynth.timeQueues.at(demandIndex)).put(timeGrains, timeGrains);
			
				// check demand indices of this time
				demandNum = helpSynth.times_demandNums.removeAt(timeGrains);

				demandNum.isNil.if {
					// no trigMsg yet scheduled for this time 
 					// make responder to expect this trigMsg from the trigger synth from now on
 					trigMsgID = helpSynth.nextTrigMsgID; 
 				
					responder = helpSynth.makeResponder(trigSynthID, trigMsgID);
					responder.add;
				
					// make entry times_demandNums
					helpSynth.times_demandNums.put(timeGrains, 1);

					// make entry trigMsgIDs_times
					helpSynth.trigMsgIDs_times.put(trigMsgID, timeGrains);
				
		 			helpSynth.server.sendBundle(helpSynth.demandLatency, ["/n_set", trigSynthID, \t_tr, 1], 
						["/n_set", trigSynthID, \trigMsgID, trigMsgID]
					);
	 			}{	// more than one demand at a time grain, but ... no further msg, update times_demandNums
					helpSynth.times_demandNums.put(timeGrains, demandNum + 1);
				};
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
				helpSynth.timeQueues.at(~demandIndex).pop.asInteger; 
			}})).asStream,
		\dur, 
			Pstutter(repeats, Pfunc({ |e| e.use { 
				var maybeDur;
				maybeDur = (helpSynth.times_durs.at(~demandIndex)).at(~timeGrains);

				maybeDur.isNil.if { 
					("WARNING: no registered duration at time in ms: " 
			    		++ ~timeGrains.asString).postln;
			    			~type = \rest; 
				};
				maybeDur 
			}})).asStream,
		\val, 
			Pstutter(repeats, Pfunc({ |e| e.use { 
				var maybeVal, theseDemandIndices;
				maybeVal = (helpSynth.times_vals).at(~timeGrains);
				maybeVal.isNil.if { 
				   ("WARNING: no registered value at time in ms: " 
			    	++ ~timeGrains.asString).postln; 
				};
				maybeVal 
			}})).asStream,
		\receiveCleanup, 
			Pstutter(repeats, Pfunc({ |e| e.use {
				var demandNum;
				(helpSynth.times_durs.at(~demandIndex)).removeAt(~timeGrains);
				demandNum = helpSynth.times_demandNums.removeAt(~timeGrains);
				(demandNum == 1).if {
					helpSynth.times_vals.removeAt(~timeGrains);
				}{
					helpSynth.times_demandNums.put(~timeGrains, demandNum - 1)
				}
			}})).asStream
		]
	}

	makeDemandAndReceivePatterns { 
		var demandPatternArgs = List.new, receivePatternArgs = List.new, 
			durPat, relDemandIndexUserNum, pairsOrArrayOf, demandCounter = 1, receiveCounter = 1, protoArgs;
			
		pbindArgs.do({|item,i|
			(i.even).if { 
				durPat = item;
				pairsOrArrayOf = pbindArgs @ (i+1);
				relDemandIndexUserNum = pairsOrArrayOf.every( {|x| x.isKindOf(Array) } ).if { 
					pairsOrArrayOf.size 
				}{ 
					pairsOrArrayOf = [pairsOrArrayOf]; 
					1; 
				};
				relDemandIndexUserNums.add(relDemandIndexUserNum);
				demandPatternArgs.add(helpSynth.sequentialSchedShift * demandCounter);
				demandPatternArgs.add(this.makeDemandPattern(i.div(2), durPat, 
					relDemandIndexUserNum, hsIndices.notNil.if { hsIndices[i.div(2)] }{ \switch }  ));
				demandCounter = demandCounter + 1;	
				// catch responses for a demand index at a time once
				protoArgs = this.makeReceivePatternProtoArgs(i.div(2), pairsOrArrayOf.size);
				
				relDemandIndexUserNum.do({ |j|
					receivePatternArgs.add(helpSynth.sequentialSchedShift * receiveCounter); 
					receivePatternArgs.add(Pbind(*(protoArgs ++ (pairsOrArrayOf @ j))));
					receiveCounter = receiveCounter + 1;
				});
			};
		});
		^[Ptpar(demandPatternArgs), Ptpar(receivePatternArgs)];
	}

	play { |clock, quant|
		var warnMsg = "No PHSplayer - " ++
			"Playing a PHSuse needs a PHS being played\n";
		helpSynth.isPHSplaying.not.if { 
			warnMsg.warn; 
		}{
			^PHelpSynthUsePlayer(this).play(clock, quant.asQuant);
		}
	}
	
	// for use with VarGui
	asTask { |clock, quant, newEnvir = true, removeCtrWithCmdPeriod = true|
		var envir, task, player = this, controller;
		envir = newEnvir.if { () }{ currentEnvironment };
		task = envir.use { Task { 1e5.wait } };
		controller =  SimpleController(task)
			.put(\userPlayed, { 
				this.helpSynth.isPHelpSynthPlaying.if {
					envir.use { player = player.play(clock: clock, quant: quant.asQuant) }  
				}{
					// PHSusePlayer must wait for PHSplayer
					{ task.stop; }.defer(0.05);
				}
			})
			.put(\userStopped, { player.stop });
		removeCtrWithCmdPeriod.if { CmdPeriod.doOnce { controller.remove } };		^task
	}
}

